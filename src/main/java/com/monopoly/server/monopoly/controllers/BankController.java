package com.monopoly.server.monopoly.controllers;
import com.monopoly.server.monopoly.classes.dto.TransactionDto;
import com.monopoly.server.monopoly.classes.request.BankPaymentRequest;
import com.monopoly.server.monopoly.classes.request.TransferRequest;
import com.monopoly.server.monopoly.exceptions.*;
import com.monopoly.server.monopoly.exceptions.InvalidTransactionException;
import com.monopoly.server.monopoly.exceptions.PlayerNotFoundException;
import com.monopoly.server.monopoly.services.BankService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bank")
@CrossOrigin(origins = "http://localhost:4200")
public class BankController {

    @Autowired
    private BankService bankService;

    @PostMapping("/transfer")
    public ResponseEntity<TransactionDto> transferMoney(@RequestBody TransferRequest request) {
        try {
            TransactionDto transaction = bankService.transferMoney(
                    request.getFromPlayerId(),
                    request.getToPlayerId(),
                    request.getAmount(),
                    request.getDescription()
            );
            return ResponseEntity.ok(transaction);
        } catch (PlayerNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (InvalidTransactionException | InsufficientFundsException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/pay-to-bank")
    public ResponseEntity<TransactionDto> payToBank(@RequestBody BankPaymentRequest request) {
        try {
            TransactionDto transaction = bankService.payToBank(
                    request.getPlayerId(),
                    request.getAmount(),
                    request.getDescription()
            );
            return ResponseEntity.ok(transaction);
        } catch (PlayerNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (InsufficientFundsException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/pay-from-bank")
    public ResponseEntity<TransactionDto> payFromBank(@RequestBody BankPaymentRequest request) {
        try {
            TransactionDto transaction = bankService.payFromBank(
                    request.getPlayerId(),
                    request.getAmount(),
                    request.getDescription()
            );
            return ResponseEntity.ok(transaction);
        } catch (PlayerNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/transactions/{sessionCode}")
    public ResponseEntity<List<TransactionDto>> getTransactions(@PathVariable String sessionCode) {
        try {
            List<TransactionDto> transactions = bankService.getSessionTransactions(sessionCode);
            return ResponseEntity.ok(transactions);
        } catch (SessionNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}