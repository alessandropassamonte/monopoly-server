package com.monopoly.server.monopoly.services;

import com.monopoly.server.monopoly.classes.dto.PlayerDto;
import com.monopoly.server.monopoly.classes.dto.WebSocketMessage;
import com.monopoly.server.monopoly.entities.Player;
import com.monopoly.server.monopoly.entities.PropertyOwnership;
import com.monopoly.server.monopoly.enums.PropertyColor;
import com.monopoly.server.monopoly.exceptions.InsufficientFundsException;
import com.monopoly.server.monopoly.exceptions.PlayerNotFoundException;
import com.monopoly.server.monopoly.repositories.PlayerRepository;
import com.monopoly.server.monopoly.repositories.PropertyOwnershipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class BankruptcyService {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PropertyOwnershipRepository ownershipRepository;

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private BankService bankService;

    @Autowired
    private WebSocketService webSocketService;

    /**
     * Verifica se un giocatore può pagare un debito vendendo proprietà
     */
    public BigDecimal calculateLiquidationValue(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Giocatore non trovato"));

        BigDecimal totalValue = player.getBalance();
        List<PropertyOwnership> properties = ownershipRepository.findByPlayer(player);

        for (PropertyOwnership ownership : properties) {
            // Valore degli edifici (venduti al 50%)
            if (ownership.isHasHotel()) {
                BigDecimal hotelCost = getHouseCost(ownership.getProperty().getColorGroup());
                totalValue = totalValue.add(hotelCost.divide(BigDecimal.valueOf(2)));
            }
            if (ownership.getHouses() > 0) {
                BigDecimal houseCost = getHouseCost(ownership.getProperty().getColorGroup());
                BigDecimal houseValue = houseCost.multiply(BigDecimal.valueOf(ownership.getHouses()))
                        .divide(BigDecimal.valueOf(2));
                totalValue = totalValue.add(houseValue);
            }

            // Valore ipoteca (50% del prezzo proprietà)
            if (!ownership.isMortgaged()) {
                BigDecimal mortgageValue = ownership.getProperty().getPrice().divide(BigDecimal.valueOf(2));
                totalValue = totalValue.add(mortgageValue);
            }
        }

        return totalValue;
    }

    /**
     * Liquidazione forzata di tutte le proprietà di un giocatore
     */
    @Transactional
    public BigDecimal liquidatePlayerAssets(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Giocatore non trovato"));

        BigDecimal totalLiquidated = BigDecimal.ZERO;
        List<PropertyOwnership> properties = ownershipRepository.findByPlayer(player);

        // 1. Vendi tutti gli edifici
        for (PropertyOwnership ownership : properties) {
            if (ownership.isHasHotel() || ownership.getHouses() > 0) {
                BigDecimal buildingValue = liquidateBuildings(ownership);
                totalLiquidated = totalLiquidated.add(buildingValue);
            }
        }

        // 2. Ipoteca tutte le proprietà non ipotecate
        for (PropertyOwnership ownership : properties) {
            if (!ownership.isMortgaged()) {
                BigDecimal mortgageValue = ownership.getProperty().getPrice().divide(BigDecimal.valueOf(2));
                ownership.setMortgaged(true);
                ownershipRepository.save(ownership);
                totalLiquidated = totalLiquidated.add(mortgageValue);
            }
        }

        // 3. Aggiorna il bilancio del giocatore
        player.setBalance(player.getBalance().add(totalLiquidated));
        playerRepository.save(player);

        return totalLiquidated;
    }

    /**
     * Bancarotta: trasferisce tutte le proprietà al creditore
     */
    @Transactional
    public void declareBankruptcy(Long bankruptPlayerId, Long creditorPlayerId) {
        Player bankruptPlayer = playerRepository.findById(bankruptPlayerId)
                .orElseThrow(() -> new PlayerNotFoundException("Giocatore in bancarotta non trovato"));

        Player creditor = null;
        if (creditorPlayerId != null) {
            creditor = playerRepository.findById(creditorPlayerId)
                    .orElseThrow(() -> new PlayerNotFoundException("Creditore non trovato"));
        }

        List<PropertyOwnership> properties = ownershipRepository.findByPlayer(bankruptPlayer);

        if (creditor != null) {
            // Bancarotta verso un giocatore: trasferisci tutto al creditore
            transferAllPropertiesToPlayer(properties, creditor);

            // Trasferisci anche il denaro rimasto
            if (bankruptPlayer.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                creditor.setBalance(creditor.getBalance().add(bankruptPlayer.getBalance()));
                playerRepository.save(creditor);
            }
        } else {
            // Bancarotta verso la banca: metti tutto all'asta (per ora elimina)
            liquidateToBank(properties);
        }

        // Rimuovi il giocatore dalla partita
        bankruptPlayer.setBalance(BigDecimal.ZERO);
        playerRepository.save(bankruptPlayer);

        // Notifica WebSocket
        webSocketService.broadcastToSession(
                bankruptPlayer.getGameSession().getSessionCode(),
                new WebSocketMessage("PLAYER_BANKRUPT",
                        bankruptPlayer.getGameSession().getSessionCode(),
                        Map.of(
                                "bankruptPlayer", bankruptPlayer.getName(),
                                "creditor", creditor != null ? creditor.getName() : "Banca",
                                "propertiesTransferred", properties.size()
                        ))
        );
    }

    /**
     * Trasferisce tutte le proprietà a un giocatore (regole bancarotta)
     */
    private void transferAllPropertiesToPlayer(List<PropertyOwnership> properties, Player newOwner) {
        for (PropertyOwnership ownership : properties) {
            // Prima liquida tutti gli edifici
            liquidateBuildings(ownership);

            // Trasferisci la proprietà
            ownership.setPlayer(newOwner);

            // Se era ipotecata, il nuovo proprietario può scegliere:
            // - Pagare il 10% per mantenerla ipotecata
            // - Estinguere l'ipoteca (55% del valore)
            // Per semplicità, manteniamo l'ipoteca e il nuovo proprietario può decidere dopo
            if (ownership.isMortgaged()) {
                BigDecimal mortgageTax = ownership.getProperty().getPrice()
                        .multiply(BigDecimal.valueOf(0.1));

                // Se il nuovo proprietario ha fondi, paga la tassa
                if (newOwner.getBalance().compareTo(mortgageTax) >= 0) {
                    newOwner.setBalance(newOwner.getBalance().subtract(mortgageTax));
                    // Altrimenti la proprietà rimane ipotecata senza tassa
                }
            }

            ownershipRepository.save(ownership);
        }
        playerRepository.save(newOwner);
    }

    /**
     * Liquidazione alla banca (per bancarotta verso la banca)
     */
    private void liquidateToBank(List<PropertyOwnership> properties) {
        // Per ora semplicemente elimina le proprietà
        // In futuro si potrebbe implementare un sistema d'asta
        for (PropertyOwnership ownership : properties) {
            liquidateBuildings(ownership);
            ownershipRepository.delete(ownership);
        }
    }

    /**
     * Liquida tutti gli edifici di una proprietà (vendita forzata al 50%)
     */
    private BigDecimal liquidateBuildings(PropertyOwnership ownership) {
        BigDecimal liquidatedValue = BigDecimal.ZERO;
        BigDecimal houseCost = getHouseCost(ownership.getProperty().getColorGroup());
        BigDecimal sellPrice = houseCost.divide(BigDecimal.valueOf(2));

        if (ownership.isHasHotel()) {
            liquidatedValue = liquidatedValue.add(sellPrice);
            ownership.setHasHotel(false);
        }

        if (ownership.getHouses() > 0) {
            liquidatedValue = liquidatedValue.add(
                    sellPrice.multiply(BigDecimal.valueOf(ownership.getHouses()))
            );
            ownership.setHouses(0);
        }

        if (liquidatedValue.compareTo(BigDecimal.ZERO) > 0) {
            // Paga il proprietario
            Player owner = ownership.getPlayer();
            owner.setBalance(owner.getBalance().add(liquidatedValue));
            playerRepository.save(owner);
        }

        ownershipRepository.save(ownership);
        return liquidatedValue;
    }

    /**
     * Verifica se un giocatore è in bancarotta
     */
    public boolean isPlayerBankrupt(Long playerId, BigDecimal debtAmount) {
        BigDecimal liquidationValue = calculateLiquidationValue(playerId);
        return liquidationValue.compareTo(debtAmount) < 0;
    }

    /**
     * Costi delle case per gruppo colore
     */
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
        return costs.getOrDefault(colorGroup, BigDecimal.valueOf(100));
    }

    /**
     * Calcola il patrimonio totale di un giocatore
     */
    public BigDecimal calculatePlayerNetWorth(Long playerId) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Giocatore non trovato"));

        BigDecimal netWorth = player.getBalance();
        List<PropertyOwnership> properties = ownershipRepository.findByPlayer(player);

        for (PropertyOwnership ownership : properties) {
            // Valore della proprietà
            if (ownership.isMortgaged()) {
                // Se ipotecata, vale solo il valore di riscatto rimanente
                BigDecimal remainingValue = ownership.getProperty().getPrice()
                        .multiply(BigDecimal.valueOf(0.05)); // 55% - 50% = 5%
                netWorth = netWorth.add(remainingValue);
            } else {
                netWorth = netWorth.add(ownership.getProperty().getPrice());
            }

            // Valore degli edifici
            if (ownership.isHasHotel()) {
                netWorth = netWorth.add(getHouseCost(ownership.getProperty().getColorGroup()));
            }
            if (ownership.getHouses() > 0) {
                BigDecimal houseValue = getHouseCost(ownership.getProperty().getColorGroup())
                        .multiply(BigDecimal.valueOf(ownership.getHouses()));
                netWorth = netWorth.add(houseValue);
            }
        }

        return netWorth;
    }
}