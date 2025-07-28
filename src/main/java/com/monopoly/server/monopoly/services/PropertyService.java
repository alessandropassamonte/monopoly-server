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
import java.util.List;
import java.util.Map;
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
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException("Proprietà non trovata"));

        PropertyOwnership ownership = ownershipRepository.findByProperty(property)
                .orElseThrow(() -> new PropertyNotFoundException("Proprietà non posseduta da nessuno"));

        Player tenant = playerRepository.findById(tenantPlayerId)
                .orElseThrow(() -> new PlayerNotFoundException("Giocatore inquilino non trovato"));

        Player owner = ownership.getPlayer();

        // Non può pagare affitto a se stesso
        if (tenant.getId().equals(owner.getId())) {
            throw new InvalidTransactionException("Non puoi pagare affitto a te stesso");
        }

        // Proprietà ipotecata non genera affitto
        if (ownership.isMortgaged()) {
            throw new InvalidTransactionException("Non si paga affitto su proprietà ipotecate");
        }

        // Calcola affitto
        BigDecimal rentAmount = calculatePropertyRent(ownership, diceRoll);

        if (rentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Affitto non dovuto");
        }

        // Verifica fondi sufficienti
        if (tenant.getBalance().compareTo(rentAmount) < 0) {
            throw new InsufficientFundsException("Fondi insufficienti per pagare l'affitto");
        }

        // Effettua il trasferimento
        return bankService.transferMoney(
                tenant.getId(),
                owner.getId(),
                rentAmount,
                "Affitto " + property.getName()
        );
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

        // Notifica WebSocket
        webSocketService.broadcastToSession(
                currentOwner.getGameSession().getSessionCode(),
                new WebSocketMessage("PROPERTY_TRANSFERRED",
                        currentOwner.getGameSession().getSessionCode(),
                        Map.of(
                                "property", ownership.getProperty().getName(),
                                "fromPlayer", currentOwner.getName(),
                                "toPlayer", newOwner.getName(),
                                "price", price != null ? price : BigDecimal.ZERO
                        ))
        );

        return mapToOwnershipDto(ownership);
    }

    /**
     * Vende tutti gli edifici da una proprietà (regola obbligatoria prima del trasferimento)
     */
    private void sellAllBuildings(PropertyOwnership ownership) {
        BigDecimal totalRefund = BigDecimal.ZERO;
        BigDecimal houseCost = getHouseCost(ownership.getProperty().getColorGroup());
        BigDecimal sellPrice = houseCost.divide(BigDecimal.valueOf(2));

        if (ownership.isHasHotel()) {
            totalRefund = totalRefund.add(sellPrice);
            ownership.setHasHotel(false);
        }

        if (ownership.getHouses() > 0) {
            totalRefund = totalRefund.add(sellPrice.multiply(BigDecimal.valueOf(ownership.getHouses())));
            ownership.setHouses(0);
        }

        if (totalRefund.compareTo(BigDecimal.ZERO) > 0) {
            bankService.payFromBank(ownership.getPlayer().getId(), totalRefund,
                    "Vendita forzata edifici da " + ownership.getProperty().getName());
        }

        ownershipRepository.save(ownership);
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

        // Calcola valore ipoteca (50% del prezzo)
        BigDecimal mortgageValue = ownership.getProperty().getPrice().divide(BigDecimal.valueOf(2));

        // Paga il giocatore
        bankService.payFromBank(ownership.getPlayer().getId(), mortgageValue,
                "Ipoteca " + ownership.getProperty().getName());

        ownership.setMortgaged(true);
        ownership = ownershipRepository.save(ownership);

        return mapToOwnershipDto(ownership);
    }

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

        ownership.setMortgaged(false);
        ownership = ownershipRepository.save(ownership);

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
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException("Proprietà non trovata"));

        PropertyOwnership ownership = ownershipRepository.findByProperty(property)
                .orElse(null);

        if (ownership == null || ownership.isMortgaged()) {
            return BigDecimal.ZERO;
        }

        return calculatePropertyRent(ownership, diceRoll);
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
        BigDecimal baseRent = ownership.getProperty().getRent();

        if (ownership.isHasHotel()) {
            // Hotel = affitto base * 5 (regola ufficiale)
            return baseRent.multiply(BigDecimal.valueOf(5));
        }

        if (ownership.getHouses() > 0) {
            // Moltiplicatori ufficiali per case
            int multiplier = switch (ownership.getHouses()) {
                case 1 -> 5;
                case 2 -> 15;
                case 3 -> 45;
                case 4 -> 80;
                default -> 1;
            };
            return baseRent.multiply(BigDecimal.valueOf(multiplier));
        }

        // Controlla se ha monopolio (doppio affitto)
        if (hasColorGroupMonopoly(ownership.getPlayer(), ownership.getProperty().getColorGroup())) {
            return baseRent.multiply(BigDecimal.valueOf(2));
        }

        return baseRent;
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