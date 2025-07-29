package com.monopoly.server.monopoly.services;

import com.monopoly.server.monopoly.classes.dto.PropertyDto;
import com.monopoly.server.monopoly.classes.dto.PropertyOwnershipDto;
import com.monopoly.server.monopoly.classes.dto.WebSocketMessage;
import com.monopoly.server.monopoly.classes.dto.TransactionDto;
import com.monopoly.server.monopoly.entities.Player;
import com.monopoly.server.monopoly.entities.Property;
import com.monopoly.server.monopoly.entities.PropertyOwnership;
import com.monopoly.server.monopoly.enums.PropertyColor;
import com.monopoly.server.monopoly.enums.PropertyType;
import com.monopoly.server.monopoly.exceptions.*;
import com.monopoly.server.monopoly.repositories.PlayerRepository;
import com.monopoly.server.monopoly.repositories.PropertyOwnershipRepository;
import com.monopoly.server.monopoly.repositories.PropertyRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PropertyService {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyOwnershipRepository ownershipRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private BankService bankService;

    @Autowired
    private WebSocketService webSocketService;

    public PropertyOwnershipDto purchaseProperty(Long playerId, Long propertyId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Giocatore non trovato"));

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException("Proprietà non trovata"));

        // Verifica se la proprietà è già posseduta
        if (ownershipRepository.existsByProperty(property)) {
            throw new PropertyAlreadyOwnedException("Proprietà già posseduta");
        }

        // Verifica fondi sufficienti
        if (player.getBalance().compareTo(property.getPrice()) < 0) {
            throw new InsufficientFundsException("Fondi insufficienti per l'acquisto");
        }

        // Effettua il pagamento
        bankService.payToBank(playerId, property.getPrice(),
                "Acquisto " + property.getName());

        // Crea ownership
        PropertyOwnership ownership = new PropertyOwnership();
        ownership.setPlayer(player);
        ownership.setProperty(property);
        ownership.setHouses(0);
        ownership.setHasHotel(false);
        ownership.setMortgaged(false);

        ownership = ownershipRepository.save(ownership);

        // Notifica WebSocket
        webSocketService.broadcastToSession(
                player.getGameSession().getSessionCode(),
                new WebSocketMessage("PROPERTY_PURCHASED",
                        player.getGameSession().getSessionCode(),
                        Map.of(
                                "player", player.getName(),
                                "property", property.getName(),
                                "price", property.getPrice()
                        ))
        );

        return mapToOwnershipDto(ownership);
    }

    /**
     * NUOVO: Pagamento affitto - implementa le regole ufficiali
     */
    @Transactional
    public TransactionDto payRent(Long propertyId, Long tenantPlayerId, int diceRoll) {
        System.out.println("=== PAY RENT REQUEST ===");
        System.out.println("Property ID: " + propertyId + ", Tenant ID: " + tenantPlayerId + ", Dice: " + diceRoll);

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException("Proprietà non trovata"));

        PropertyOwnership ownership = ownershipRepository.findByProperty(property)
                .orElseThrow(() -> new PropertyNotFoundException("Proprietà non posseduta da nessuno"));

        Player tenant = playerRepository.findById(tenantPlayerId)
                .orElseThrow(() -> new PlayerNotFoundException("Giocatore inquilino non trovato"));

        Player owner = ownership.getPlayer();

        System.out.println("Property: " + property.getName() + ", Owner: " + owner.getName() + ", Tenant: " + tenant.getName());

        // Non può pagare affitto a se stesso
        if (tenant.getId().equals(owner.getId())) {
            System.out.println("ERROR: Cannot pay rent to self");
            throw new InvalidTransactionException("Non puoi pagare affitto a te stesso");
        }

        // Proprietà ipotecata non genera affitto
        if (ownership.isMortgaged()) {
            System.out.println("ERROR: Property is mortgaged");
            throw new InvalidTransactionException("Non si paga affitto su proprietà ipotecate");
        }

        // Calcola affitto
        BigDecimal rentAmount = calculatePropertyRent(ownership, diceRoll);
        System.out.println("Calculated rent amount: " + rentAmount);

        if (rentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("ERROR: No rent due");
            throw new InvalidTransactionException("Affitto non dovuto");
        }

        // Verifica fondi sufficienti
        if (tenant.getBalance().compareTo(rentAmount) < 0) {
            System.out.println("ERROR: Insufficient funds. Tenant balance: " + tenant.getBalance() + ", Rent: " + rentAmount);
            throw new InsufficientFundsException("Fondi insufficienti per pagare l'affitto");
        }

        // Effettua il trasferimento
        TransactionDto transaction = bankService.transferMoney(
                tenant.getId(),
                owner.getId(),
                rentAmount,
                "Affitto " + property.getName()
        );

        System.out.println("✅ Rent payment completed successfully");
        return transaction;
    }

    // NUOVO: Metodo helper per vendita edifici nel trasferimento
    private void sellAllBuildings(PropertyOwnership ownership) {
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal houseCost = getHouseCost(ownership.getProperty().getColorGroup());
        BigDecimal sellPrice = houseCost.divide(BigDecimal.valueOf(2));

        System.out.println("=== SELLING ALL BUILDINGS ===");
        System.out.println("Property: " + ownership.getProperty().getName());
        System.out.println("Has hotel: " + ownership.isHasHotel() + ", Houses: " + ownership.getHouses());

        if (ownership.isHasHotel()) {
            totalRefund = totalRefund.add(sellPrice);
            ownership.setHasHotel(false);
            System.out.println("Sold hotel for: " + sellPrice);
        }

        if (ownership.getHouses() > 0) {
            BigDecimal housesRefund = sellPrice.multiply(BigDecimal.valueOf(ownership.getHouses()));
            totalRefund = totalRefund.add(housesRefund);
            System.out.println("Sold " + ownership.getHouses() + " houses for: " + housesRefund);
            ownership.setHouses(0);
        }

        if (totalRefund.compareTo(BigDecimal.ZERO) > 0) {
            bankService.payFromBank(ownership.getPlayer().getId(), totalRefund,
                    "Vendita forzata edifici da " + ownership.getProperty().getName());
            System.out.println("Total refund: " + totalRefund);
        }

        ownershipRepository.save(ownership);
        System.out.println("✅ All buildings sold successfully");
    }

    /**
     * NUOVO: Vendita casa - implementa regole ufficiali (50% del costo)
     */
    @Transactional
    public PropertyOwnershipDto sellHouse(Long ownershipId) {
        PropertyOwnership ownership = ownershipRepository.findById(ownershipId)
                .orElseThrow(() -> new PropertyNotFoundException("Proprietà non trovata"));

        if (ownership.getHouses() <= 0) {
            throw new InvalidPropertyActionException("Nessuna casa da vendere");
        }

        if (ownership.isHasHotel()) {
            throw new InvalidPropertyActionException("Vendi prima l'hotel");
        }

        // Verifica costruzione equilibrata nel gruppo
        if (!canSellHouseFromGroup(ownership)) {
            throw new InvalidPropertyActionException(
                    "Non puoi vendere: mantieni la costruzione equilibrata nel gruppo colore");
        }

        // Calcola prezzo vendita (50% del costo)
        BigDecimal houseCost = getHouseCost(ownership.getProperty().getColorGroup());
        BigDecimal sellPrice = houseCost.divide(BigDecimal.valueOf(2));

        // Vendi casa
        ownership.setHouses(ownership.getHouses() - 1);
        ownership = ownershipRepository.save(ownership);

        // Paga il giocatore
        bankService.payFromBank(ownership.getPlayer().getId(), sellPrice,
                "Vendita casa da " + ownership.getProperty().getName());

        return mapToOwnershipDto(ownership);
    }

    /**
     * NUOVO: Vendita hotel - implementa regole ufficiali
     */
    @Transactional
    public PropertyOwnershipDto sellHotel(Long ownershipId) {
        PropertyOwnership ownership = ownershipRepository.findById(ownershipId)
                .orElseThrow(() -> new PropertyNotFoundException("Proprietà non trovata"));

        if (!ownership.isHasHotel()) {
            throw new InvalidPropertyActionException("Nessun hotel da vendere");
        }

        // Calcola prezzo vendita hotel (50% del costo)
        BigDecimal hotelCost = getHouseCost(ownership.getProperty().getColorGroup());
        BigDecimal sellPrice = hotelCost.divide(BigDecimal.valueOf(2));

        // Vendi hotel e ripristina 4 case
        ownership.setHasHotel(false);
        ownership.setHouses(4);
        ownership = ownershipRepository.save(ownership);

        // Paga il giocatore
        bankService.payFromBank(ownership.getPlayer().getId(), sellPrice,
                "Vendita hotel da " + ownership.getProperty().getName());

        return mapToOwnershipDto(ownership);
    }

    /**
     * NUOVO: Trasferimento proprietà tra giocatori
     */
    @Transactional
    public PropertyOwnershipDto transferProperty(Long ownershipId, Long newOwnerId, BigDecimal price) {
        System.out.println("=== TRANSFERRING PROPERTY ===");
        System.out.println("Ownership ID: " + ownershipId + ", New Owner ID: " + newOwnerId + ", Price: " + price);

        PropertyOwnership ownership = ownershipRepository.findById(ownershipId)
                .orElseThrow(() -> new PropertyNotFoundException("Proprietà non trovata"));

        Player newOwner = playerRepository.findById(newOwnerId)
                .orElseThrow(() -> new PlayerNotFoundException("Nuovo proprietario non trovato"));

        Player currentOwner = ownership.getPlayer();

        // Verifica che siano nella stessa sessione
        if (!currentOwner.getGameSession().equals(newOwner.getGameSession())) {
            throw new InvalidTransactionException("I giocatori devono essere nella stessa sessione");
        }

        // Non può trasferire a se stesso
        if (currentOwner.getId().equals(newOwner.getId())) {
            throw new InvalidTransactionException("Non puoi trasferire una proprietà a te stesso");
        }

        // CORREZIONE: Verifica se il proprietario corrente aveva il gruppo completo PRIMA del trasferimento
        boolean currentOwnerHadCompleteGroup = hasColorGroupMonopoly(currentOwner, ownership.getProperty().getColorGroup());

        // Prima di trasferire, vendi tutti gli edifici se presenti
        if (ownership.getHouses() > 0 || ownership.isHasHotel()) {
            sellAllBuildings(ownership);
        }

        // Se c'è un prezzo, effettua il pagamento
        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
            if (newOwner.getBalance().compareTo(price) < 0) {
                throw new InsufficientFundsException("Fondi insufficienti per l'acquisto");
            }

            bankService.transferMoney(
                    newOwner.getId(),
                    currentOwner.getId(),
                    price,
                    "Acquisto " + ownership.getProperty().getName() + " da " + currentOwner.getName()
            );
        }

        // Gestisci ipoteca: nuovo proprietario può estinguere o pagare 10%
        if (ownership.isMortgaged()) {
            BigDecimal mortgageTax = ownership.getProperty().getPrice()
                    .multiply(BigDecimal.valueOf(0.1));

            if (newOwner.getBalance().compareTo(mortgageTax) >= 0) {
                bankService.payToBank(newOwner.getId(), mortgageTax,
                        "Tassa trasferimento ipoteca " + ownership.getProperty().getName());
            }
            // Se non può pagare la tassa, la proprietà rimane ipotecata
        }

        // Trasferisci proprietà
        ownership.setPlayer(newOwner);
        ownership = ownershipRepository.save(ownership);

        // NUOVO: Ricalcola affitti per entrambi i giocatori se necessario
        PropertyColor colorGroup = ownership.getProperty().getColorGroup();

        // Ricalcola per il vecchio proprietario (se aveva il gruppo completo)
        if (currentOwnerHadCompleteGroup) {
            recalculateGroupRents(currentOwner, colorGroup);
        }

        // Ricalcola per il nuovo proprietario (se ora ha il gruppo completo)
        boolean newOwnerHasCompleteGroup = hasColorGroupMonopoly(newOwner, colorGroup);
        if (newOwnerHasCompleteGroup) {
            recalculateGroupRents(newOwner, colorGroup);
        }

        // Notifica WebSocket
        webSocketService.broadcastPropertyUpdate(
                currentOwner.getGameSession().getSessionCode(),
                Map.of(
                        "action", "PROPERTY_TRANSFERRED",
                        "property", ownership.getProperty().getName(),
                        "fromPlayer", currentOwner.getName(),
                        "toPlayer", newOwner.getName(),
                        "price", price != null ? price : BigDecimal.ZERO,
                        "currentOwnerHadCompleteGroup", currentOwnerHadCompleteGroup,
                        "newOwnerHasCompleteGroup", newOwnerHasCompleteGroup
                )
        );

        System.out.println("✅ Property transfer completed successfully");
        return mapToOwnershipDto(ownership);
    }

    /**
     * NUOVO: Trasferimento multiplo di proprietà (per scambio negoziato)
     */
    @Transactional
    public List<PropertyOwnershipDto> transferMultipleProperties(
            List<Long> ownershipIds,
            Long newOwnerId,
            BigDecimal compensationAmount) {

        System.out.println("=== TRANSFERRING MULTIPLE PROPERTIES ===");
        System.out.println("Ownership IDs: " + ownershipIds + ", New Owner: " + newOwnerId + ", Compensation: " + compensationAmount);

        if (ownershipIds == null || ownershipIds.isEmpty()) {
            throw new InvalidTransactionException("Nessuna proprietà da trasferire");
        }

        Player newOwner = playerRepository.findById(newOwnerId)
                .orElseThrow(() -> new PlayerNotFoundException("Nuovo proprietario non trovato"));

        List<PropertyOwnershipDto> transferredProperties = new ArrayList<>();
        Player currentOwner = null;
        Set<PropertyColor> affectedColorGroups = new HashSet<>();

        // Trasferisci tutte le proprietà
        for (Long ownershipId : ownershipIds) {
            PropertyOwnership ownership = ownershipRepository.findById(ownershipId)
                    .orElseThrow(() -> new PropertyNotFoundException("Proprietà non trovata: " + ownershipId));

            // Verifica che tutte le proprietà appartengano allo stesso giocatore
            if (currentOwner == null) {
                currentOwner = ownership.getPlayer();
            } else if (!currentOwner.getId().equals(ownership.getPlayer().getId())) {
                throw new InvalidTransactionException("Tutte le proprietà devono appartenere allo stesso giocatore");
            }

            // Verifica sessione
            if (!currentOwner.getGameSession().equals(newOwner.getGameSession())) {
                throw new InvalidTransactionException("I giocatori devono essere nella stessa sessione");
            }

            // Vendi edifici se presenti
            if (ownership.getHouses() > 0 || ownership.isHasHotel()) {
                sellAllBuildings(ownership);
            }

            // Gestisci ipoteca
            if (ownership.isMortgaged()) {
                BigDecimal mortgageTax = ownership.getProperty().getPrice()
                        .multiply(BigDecimal.valueOf(0.1));
                if (newOwner.getBalance().compareTo(mortgageTax) >= 0) {
                    bankService.payToBank(newOwner.getId(), mortgageTax,
                            "Tassa trasferimento ipoteca " + ownership.getProperty().getName());
                }
            }

            // Trasferisci
            affectedColorGroups.add(ownership.getProperty().getColorGroup());
            ownership.setPlayer(newOwner);
            ownership = ownershipRepository.save(ownership);

            transferredProperties.add(mapToOwnershipDto(ownership));
        }

        // Gestisci compenso monetario
        if (compensationAmount != null && compensationAmount.compareTo(BigDecimal.ZERO) != 0) {
            if (compensationAmount.compareTo(BigDecimal.ZERO) > 0) {
                // Il current owner riceve denaro
                if (newOwner.getBalance().compareTo(compensationAmount) < 0) {
                    throw new InsufficientFundsException("Fondi insufficienti per il compenso");
                }
                bankService.transferMoney(newOwnerId, currentOwner.getId(), compensationAmount,
                        "Compenso scambio proprietà multiple");
            } else {
                // Il current owner paga denaro
                BigDecimal paymentAmount = compensationAmount.abs();
                if (currentOwner.getBalance().compareTo(paymentAmount) < 0) {
                    throw new InsufficientFundsException("Fondi insufficienti per il pagamento");
                }
                bankService.transferMoney(currentOwner.getId(), newOwnerId, paymentAmount,
                        "Pagamento scambio proprietà multiple");
            }
        }

        // Ricalcola affitti per tutti i gruppi di colore coinvolti
        for (PropertyColor colorGroup : affectedColorGroups) {
            recalculateGroupRents(currentOwner, colorGroup);
            recalculateGroupRents(newOwner, colorGroup);
        }

        // Notifica WebSocket
        webSocketService.broadcastPropertyUpdate(
                currentOwner.getGameSession().getSessionCode(),
                Map.of(
                        "action", "MULTIPLE_PROPERTIES_TRANSFERRED",
                        "propertiesCount", transferredProperties.size(),
                        "fromPlayer", currentOwner.getName(),
                        "toPlayer", newOwner.getName(),
                        "compensation", compensationAmount != null ? compensationAmount : BigDecimal.ZERO,
                        "affectedColorGroups", affectedColorGroups.size()
                )
        );

        System.out.println("✅ Multiple properties transfer completed successfully");
        return transferredProperties;
    }


    public List<PropertyOwnershipDto> getPlayerProperties(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Giocatore non trovato"));

        return ownershipRepository.findByPlayer(player)
                .stream()
                .map(this::mapToOwnershipDto)
                .collect(Collectors.toList());
    }

    public PropertyOwnershipDto mortgageProperty(Long ownershipId) {
        PropertyOwnership ownership = ownershipRepository.findById(ownershipId)
                .orElseThrow(() -> new PropertyNotFoundException("Proprietà non trovata"));

        if (ownership.isMortgaged()) {
            throw new InvalidPropertyActionException("Proprietà già ipotecata");
        }

        if (ownership.getHouses() > 0 || ownership.isHasHotel()) {
            throw new InvalidPropertyActionException("Rimuovi case/hotel prima di ipotecare");
        }

        // Verifica se questa proprietà fa parte di un gruppo completo PRIMA dell'ipoteca
        boolean hadCompleteGroup = hasColorGroupMonopoly(ownership.getPlayer(), ownership.getProperty().getColorGroup());

        // Calcola valore ipoteca (50% del prezzo)
        BigDecimal mortgageValue = ownership.getProperty().getPrice().divide(BigDecimal.valueOf(2));

        // Paga il giocatore
        bankService.payFromBank(ownership.getPlayer().getId(), mortgageValue,
                "Ipoteca " + ownership.getProperty().getName());

        // Ipoteca la proprietà
        ownership.setMortgaged(true);
        ownership = ownershipRepository.save(ownership);

        // NUOVO: Se aveva il gruppo completo, ricalcola gli affitti delle altre proprietà
        if (hadCompleteGroup) {
            recalculateGroupRents(ownership.getPlayer(), ownership.getProperty().getColorGroup());
        }

        // Notifica WebSocket
        webSocketService.broadcastPropertyUpdate(
                ownership.getPlayer().getGameSession().getSessionCode(),
                Map.of(
                        "action", "PROPERTY_MORTGAGED",
                        "propertyName", ownership.getProperty().getName(),
                        "playerName", ownership.getPlayer().getName(),
                        "hadCompleteGroup", hadCompleteGroup
                )
        );

        return mapToOwnershipDto(ownership);
    }

    private void recalculateGroupRents(Player player, PropertyColor colorGroup) {
        System.out.println("=== RECALCULATING GROUP RENTS ===");
        System.out.println("Player: " + player.getName() + ", Color Group: " + colorGroup);

        List<PropertyOwnership> groupProperties = ownershipRepository
                .findByPlayerAndProperty_ColorGroup(player, colorGroup);

        boolean hasCompleteGroup = hasColorGroupMonopoly(player, colorGroup);
        System.out.println("Has complete group: " + hasCompleteGroup);

        // Aggiorna ogni proprietà del gruppo
        for (PropertyOwnership ownership : groupProperties) {
            // Ricalcola solo se non è ipotecata
            if (!ownership.isMortgaged()) {
                System.out.println("Recalculating rent for: " + ownership.getProperty().getName());
                // Il calcolo dell'affitto è già gestito nel metodo calculatePropertyRent
                // Non serve salvare nulla, il calcolo è dinamico
            }
        }

        System.out.println("Group rent recalculation completed");
    }

    /**
     * CORRETTO: Riscatta proprietà con ricalcolo affitti gruppo
     */
    public PropertyOwnershipDto redeemProperty(Long ownershipId) {
        PropertyOwnership ownership = ownershipRepository.findById(ownershipId)
                .orElseThrow(() -> new PropertyNotFoundException("Proprietà non trovata"));

        if (!ownership.isMortgaged()) {
            throw new InvalidPropertyActionException("Proprietà non ipotecata");
        }

        // Calcola costo riscatto (55% del prezzo originale = valore ipoteca + 10%)
        BigDecimal redeemCost = ownership.getProperty().getPrice()
                .multiply(BigDecimal.valueOf(0.55));

        if (ownership.getPlayer().getBalance().compareTo(redeemCost) < 0) {
            throw new InsufficientFundsException("Fondi insufficienti per il riscatto");
        }

        // Paga alla banca
        bankService.payToBank(ownership.getPlayer().getId(), redeemCost,
                "Riscatto " + ownership.getProperty().getName());

        // Riscatta la proprietà
        ownership.setMortgaged(false);
        ownership = ownershipRepository.save(ownership);

        // NUOVO: Verifica se ora ha il gruppo completo e ricalcola affitti
        boolean hasCompleteGroupNow = hasColorGroupMonopoly(ownership.getPlayer(), ownership.getProperty().getColorGroup());
        if (hasCompleteGroupNow) {
            recalculateGroupRents(ownership.getPlayer(), ownership.getProperty().getColorGroup());
        }

        // Notifica WebSocket
        webSocketService.broadcastPropertyUpdate(
                ownership.getPlayer().getGameSession().getSessionCode(),
                Map.of(
                        "action", "PROPERTY_REDEEMED",
                        "propertyName", ownership.getProperty().getName(),
                        "playerName", ownership.getPlayer().getName(),
                        "hasCompleteGroupNow", hasCompleteGroupNow
                )
        );

        return mapToOwnershipDto(ownership);
    }


    public PropertyOwnershipDto buildHouse(Long ownershipId) {
        PropertyOwnership ownership = ownershipRepository.findById(ownershipId)
                .orElseThrow(() -> new PropertyNotFoundException("Proprietà non trovata"));

        if (ownership.getProperty().getType() != PropertyType.STREET) {
            throw new InvalidPropertyActionException("Solo le strade possono avere case");
        }

        if (ownership.isMortgaged()) {
            throw new InvalidPropertyActionException("Non puoi costruire su proprietà ipotecate");
        }

        if (ownership.isHasHotel()) {
            throw new InvalidPropertyActionException("La proprietà ha già un hotel");
        }

        if (ownership.getHouses() >= 4) {
            throw new InvalidPropertyActionException("Massimo 4 case per proprietà");
        }

        // Verifica monopolio del gruppo colore
        if (!hasColorGroupMonopoly(ownership.getPlayer(), ownership.getProperty().getColorGroup())) {
            throw new InvalidPropertyActionException("Devi possedere tutto il gruppo colore");
        }

        // NUOVO: Verifica costruzione equilibrata
        if (!canBuildHouseInGroup(ownership)) {
            throw new InvalidPropertyActionException(
                    "Costruzione non equilibrata: tutte le proprietà del gruppo devono avere lo stesso numero di case");
        }

        // Costo casa (varia per gruppo)
        BigDecimal houseCost = getHouseCost(ownership.getProperty().getColorGroup());

        if (ownership.getPlayer().getBalance().compareTo(houseCost) < 0) {
            throw new InsufficientFundsException("Fondi insufficienti per costruire");
        }

        // Paga e costruisci
        bankService.payToBank(ownership.getPlayer().getId(), houseCost,
                "Casa su " + ownership.getProperty().getName());

        ownership.setHouses(ownership.getHouses() + 1);
        ownership = ownershipRepository.save(ownership);

        return mapToOwnershipDto(ownership);
    }

    public PropertyOwnershipDto buildHotel(Long ownershipId) {
        PropertyOwnership ownership = ownershipRepository.findById(ownershipId)
                .orElseThrow(() -> new PropertyNotFoundException("Proprietà non trovata"));

        if (ownership.getHouses() != 4) {
            throw new InvalidPropertyActionException("Servono 4 case per costruire un hotel");
        }

        BigDecimal hotelCost = getHouseCost(ownership.getProperty().getColorGroup());

        if (ownership.getPlayer().getBalance().compareTo(hotelCost) < 0) {
            throw new InsufficientFundsException("Fondi insufficienti per l'hotel");
        }

        bankService.payToBank(ownership.getPlayer().getId(), hotelCost,
                "Hotel su " + ownership.getProperty().getName());

        ownership.setHouses(0);
        ownership.setHasHotel(true);
        ownership = ownershipRepository.save(ownership);

        return mapToOwnershipDto(ownership);
    }

    /**
     * NUOVO: Verifica costruzione equilibrata - regola ufficiale Monopoly
     */
    private boolean canBuildHouseInGroup(PropertyOwnership ownership) {
        List<PropertyOwnership> groupProperties = ownershipRepository
                .findByPlayerAndProperty_ColorGroup(ownership.getPlayer(), ownership.getProperty().getColorGroup());

        // Trova il numero minimo di case nel gruppo
        int minHouses = groupProperties.stream()
                .mapToInt(PropertyOwnership::getHouses)
                .min()
                .orElse(0);

        // Può costruire solo se questa proprietà ha il numero minimo di case
        return ownership.getHouses() == minHouses;
    }

    /**
     * NUOVO: Verifica se può vendere casa mantenendo equilibrio
     */
    private boolean canSellHouseFromGroup(PropertyOwnership ownership) {
        List<PropertyOwnership> groupProperties = ownershipRepository
                .findByPlayerAndProperty_ColorGroup(ownership.getPlayer(), ownership.getProperty().getColorGroup());

        // Trova il numero massimo di case nel gruppo
        int maxHouses = groupProperties.stream()
                .mapToInt(PropertyOwnership::getHouses)
                .max()
                .orElse(0);

        // Può vendere solo se questa proprietà ha il numero massimo di case
        return ownership.getHouses() == maxHouses;
    }

    public BigDecimal calculateRent(Long propertyId, int diceRoll) {
        System.out.println("=== CALCULATING RENT ===");
        System.out.println("Property ID: " + propertyId + ", Dice Roll: " + diceRoll);

        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException("Proprietà non trovata"));

        PropertyOwnership ownership = ownershipRepository.findByProperty(property)
                .orElse(null);

        if (ownership == null) {
            System.out.println("Property not owned, rent = 0");
            return BigDecimal.ZERO;
        }

        if (ownership.isMortgaged()) {
            System.out.println("Property is mortgaged, rent = 0");
            return BigDecimal.ZERO;
        }

        BigDecimal calculatedRent = calculatePropertyRent(ownership, diceRoll);
        System.out.println("Calculated rent: " + calculatedRent);
        return calculatedRent;
    }
    private boolean hasColorGroupMonopoly(Player player, PropertyColor colorGroup) {
        List<Property> groupProperties = propertyRepository.findByColorGroup(colorGroup);
        List<PropertyOwnership> playerGroupOwnership =
                ownershipRepository.findByPlayerAndProperty_ColorGroup(player, colorGroup);

        return groupProperties.size() == playerGroupOwnership.size();
    }

    private BigDecimal getHouseCost(PropertyColor colorGroup) {
        // Costi delle case secondo le regole ufficiali
        Map<PropertyColor, BigDecimal> costs = Map.of(
                PropertyColor.BROWN, BigDecimal.valueOf(50),
                PropertyColor.LIGHT_BLUE, BigDecimal.valueOf(50),
                PropertyColor.PINK, BigDecimal.valueOf(100),
                PropertyColor.ORANGE, BigDecimal.valueOf(100),
                PropertyColor.RED, BigDecimal.valueOf(150),
                PropertyColor.YELLOW, BigDecimal.valueOf(150),
                PropertyColor.GREEN, BigDecimal.valueOf(200),
                PropertyColor.DARK_BLUE, BigDecimal.valueOf(200)
        );
        return costs.getOrDefault(colorGroup, BigDecimal.valueOf(100));
    }

    private BigDecimal calculatePropertyRent(PropertyOwnership ownership, int diceRoll) {
        // IMPORTANTE: Se la proprietà è ipotecata, affitto = 0
        if (ownership.isMortgaged()) {
            System.out.println("Property is mortgaged, rent = 0");
            return BigDecimal.ZERO;
        }

        Property property = ownership.getProperty();

        switch (property.getType()) {
            case STREET:
                return calculateStreetRent(ownership);
            case RAILROAD:
                return calculateRailroadRent(ownership);
            case UTILITY:
                return calculateUtilityRent(ownership, diceRoll);
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculateStreetRent(PropertyOwnership ownership) {
        // IMPORTANTE: Se la proprietà è ipotecata, affitto = 0
        if (ownership.isMortgaged()) {
            return BigDecimal.ZERO;
        }

        BigDecimal baseRent = ownership.getProperty().getRent();

        // Se ha hotel
        if (ownership.isHasHotel()) {
            return baseRent.multiply(BigDecimal.valueOf(5));
        }

        // Se ha case
        if (ownership.getHouses() > 0) {
            int multiplier = switch (ownership.getHouses()) {
                case 1 -> 5;
                case 2 -> 15;
                case 3 -> 45;
                case 4 -> 80;
                default -> 1;
            };
            return baseRent.multiply(BigDecimal.valueOf(multiplier));
        }

        // CORREZIONE: Verifica monopolio considerando solo proprietà NON ipotecate
        if (hasCompleteColorGroupNonMortgaged(ownership.getPlayer(), ownership.getProperty().getColorGroup())) {
            return baseRent.multiply(BigDecimal.valueOf(2));
        }

        return baseRent;
    }

    private boolean hasCompleteColorGroupNonMortgaged(Player player, PropertyColor colorGroup) {
        List<Property> allGroupProperties = propertyRepository.findByColorGroup(colorGroup);
        List<PropertyOwnership> playerGroupOwnership = ownershipRepository
                .findByPlayerAndProperty_ColorGroup(player, colorGroup);

        // Deve possedere tutte le proprietà del gruppo
        if (allGroupProperties.size() != playerGroupOwnership.size()) {
            return false;
        }

        // IMPORTANTE: Tutte le proprietà del gruppo devono essere NON ipotecate
        boolean allNonMortgaged = playerGroupOwnership.stream()
                .allMatch(ownership -> !ownership.isMortgaged());

        System.out.println("Complete group check for " + colorGroup + ": " +
                "owns all=" + (allGroupProperties.size() == playerGroupOwnership.size()) +
                ", all non-mortgaged=" + allNonMortgaged);

        return allNonMortgaged;
    }



    private BigDecimal calculateRailroadRent(PropertyOwnership ownership) {
        // Conta quante stazioni possiede il giocatore
        long railroadCount = ownershipRepository.findByPlayer(ownership.getPlayer())
                .stream()
                .filter(o -> o.getProperty().getType() == PropertyType.RAILROAD)
                .count();

        // Affitti ufficiali per stazioni: 25, 50, 100, 200
        int rentAmount = switch ((int) railroadCount) {
            case 1 -> 25;
            case 2 -> 50;
            case 3 -> 100;
            case 4 -> 200;
            default -> 0;
        };

        return BigDecimal.valueOf(rentAmount);
    }

    private BigDecimal calculateUtilityRent(PropertyOwnership ownership, int diceRoll) {
        // Conta quante società possiede
        long utilityCount = ownershipRepository.findByPlayer(ownership.getPlayer())
                .stream()
                .filter(o -> o.getProperty().getType() == PropertyType.UTILITY)
                .count();

        // Moltiplicatore ufficiale: x4 (1 società) o x10 (2 società)
        int multiplier = utilityCount == 2 ? 10 : 4;
        return BigDecimal.valueOf(diceRoll * multiplier);
    }

    private PropertyOwnershipDto mapToOwnershipDto(PropertyOwnership ownership) {
        return PropertyOwnershipDto.builder()
                .id(ownership.getId())
                .propertyId(ownership.getProperty().getId())
                .propertyName(ownership.getProperty().getName())
                .propertyPrice(ownership.getProperty().getPrice())
                .propertyType(ownership.getProperty().getType())
                .colorGroup(ownership.getProperty().getColorGroup())
                .houses(ownership.getHouses())
                .hasHotel(ownership.isHasHotel())
                .isMortgaged(ownership.isMortgaged())
                .currentRent(calculatePropertyRent(ownership, 7)) // Default dice roll
                .purchasedAt(ownership.getPurchasedAt())
                .build();
    }

    public List<PropertyDto> getAllProperties() {
        return propertyRepository.findAll()
                .stream()
                .map(this::mapToPropertyDto)
                .collect(Collectors.toList());
    }

    /**
     * NUOVO: Ottieni tutte le proprietà possedute in una sessione
     */
    public List<PropertyOwnershipDto> getSessionProperties(String sessionCode) {
        try {
            // Trova la sessione tramite un repository (dobbiamo aggiungerlo)
            // Per ora usiamo una query più diretta
            List<PropertyOwnership> allOwnerships = ownershipRepository.findAll();

            return allOwnerships.stream()
                    .filter(ownership -> ownership.getPlayer().getGameSession().getSessionCode().equals(sessionCode))
                    .map(this::mapToOwnershipDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error getting session properties: " + e.getMessage());
            return List.of();
        }
    }

    private PropertyDto mapToPropertyDto(Property property) {
        return PropertyDto.builder()
                .id(property.getId())
                .name(property.getName())
                .price(property.getPrice())
                .rent(property.getRent())
                .colorGroup(property.getColorGroup())
                .type(property.getType())
                .build();
    }
}