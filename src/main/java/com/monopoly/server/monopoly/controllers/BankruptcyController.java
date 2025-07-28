package com.monopoly.server.monopoly.controllers;

import com.monopoly.server.monopoly.classes.request.BankruptcyRequest;
import com.monopoly.server.monopoly.exceptions.PlayerNotFoundException;
import com.monopoly.server.monopoly.services.BankruptcyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/bankruptcy")
@CrossOrigin(origins = "http://localhost:4200")
public class BankruptcyController {

    @Autowired
    private BankruptcyService bankruptcyService;

    /**
     * Calcola il valore di liquidazione di un giocatore
     */
    @GetMapping("/liquidation-value/{playerId}")
    public ResponseEntity<BigDecimal> calculateLiquidationValue(@PathVariable Long playerId) {
        try {
            System.out.println("=== CALCULATE LIQUIDATION VALUE ===");
            System.out.println("Player ID: " + playerId);

            BigDecimal value = bankruptcyService.calculateLiquidationValue(playerId);
            System.out.println("Liquidation value: " + value);
            return ResponseEntity.ok(value);
        } catch (PlayerNotFoundException e) {
            System.err.println("Player not found: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Error calculating liquidation value: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Calcola il patrimonio netto di un giocatore
     */
    @GetMapping("/net-worth/{playerId}")
    public ResponseEntity<BigDecimal> calculateNetWorth(@PathVariable Long playerId) {
        try {
            System.out.println("=== CALCULATE NET WORTH ===");
            System.out.println("Player ID: " + playerId);

            BigDecimal netWorth = bankruptcyService.calculatePlayerNetWorth(playerId);
            System.out.println("Net worth: " + netWorth);
            return ResponseEntity.ok(netWorth);
        } catch (PlayerNotFoundException e) {
            System.err.println("Player not found: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Error calculating net worth: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Liquidazione forzata degli asset di un giocatore
     */
    @PostMapping("/liquidate/{playerId}")
    public ResponseEntity<Map<String, Object>> liquidateAssets(@PathVariable Long playerId) {
        try {
            System.out.println("=== LIQUIDATE ASSETS ===");
            System.out.println("Player ID: " + playerId);

            BigDecimal liquidatedAmount = bankruptcyService.liquidatePlayerAssets(playerId);
            System.out.println("Liquidated amount: " + liquidatedAmount);

            return ResponseEntity.ok(Map.of(
                    "liquidatedAmount", liquidatedAmount,
                    "message", "Asset liquidati con successo"
            ));
        } catch (PlayerNotFoundException e) {
            System.err.println("Player not found: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Error liquidating assets: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Dichiarazione di bancarotta
     */
    @PostMapping("/declare")
    public ResponseEntity<Map<String, String>> declareBankruptcy(@RequestBody BankruptcyRequest request) {
        try {
            System.out.println("=== DECLARE BANKRUPTCY ===");
            System.out.println("Bankrupt Player ID: " + request.getBankruptPlayerId());
            System.out.println("Creditor Player ID: " + request.getCreditorPlayerId());

            bankruptcyService.declareBankruptcy(
                    request.getBankruptPlayerId(),
                    request.getCreditorPlayerId()
            );

            String message = request.getCreditorPlayerId() != null ?
                    "Proprietà trasferite al creditore" :
                    "Proprietà liquidate alla Banca";

            return ResponseEntity.ok(Map.of(
                    "status", "SUCCESS",
                    "message", message
            ));
        } catch (PlayerNotFoundException e) {
            System.err.println("Player not found: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Error declaring bankruptcy: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Verifica se un giocatore è in bancarotta per un debito specifico
     */
    @GetMapping("/check/{playerId}/{debtAmount}")
    public ResponseEntity<Map<String, Object>> checkBankruptcy(
            @PathVariable Long playerId,
            @PathVariable BigDecimal debtAmount) {
        try {
            System.out.println("=== CHECK BANKRUPTCY ===");
            System.out.println("Player ID: " + playerId + ", Debt: " + debtAmount);

            boolean isBankrupt = bankruptcyService.isPlayerBankrupt(playerId, debtAmount);
            BigDecimal liquidationValue = bankruptcyService.calculateLiquidationValue(playerId);

            return ResponseEntity.ok(Map.of(
                    "isBankrupt", isBankrupt,
                    "liquidationValue", liquidationValue,
                    "debtAmount", debtAmount,
                    "shortfall", isBankrupt ? debtAmount.subtract(liquidationValue) : BigDecimal.ZERO
            ));
        } catch (PlayerNotFoundException e) {
            System.err.println("Player not found: " + e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            System.err.println("Error checking bankruptcy: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}

