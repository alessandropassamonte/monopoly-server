package com.monopoly.server.monopoly.services;

import com.monopoly.server.monopoly.classes.dto.PropertyDto;
import com.monopoly.server.monopoly.classes.dto.PropertyOwnershipDto;
import com.monopoly.server.monopoly.classes.dto.WebSocketMessage;
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

        // Calcola costo riscatto (55% del prezzo originale)
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
        return costs.get(colorGroup);
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
            return baseRent.multiply(BigDecimal.valueOf(5));
        }

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

        int multiplier = switch ((int) railroadCount) {
            case 1 -> 25;
            case 2 -> 50;
            case 3 -> 100;
            case 4 -> 200;
            default -> 0;
        };

        return BigDecimal.valueOf(multiplier);
    }

    private BigDecimal calculateUtilityRent(PropertyOwnership ownership, int diceRoll) {
        // Conta quante società possiede
        long utilityCount = ownershipRepository.findByPlayer(ownership.getPlayer())
                .stream()
                .filter(o -> o.getProperty().getType() == PropertyType.UTILITY)
                .count();

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
        return propertyRepository.findByType(PropertyType.STREET)
                .stream()
                .map(this::mapToPropertyDto)
                .collect(Collectors.toList());
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
