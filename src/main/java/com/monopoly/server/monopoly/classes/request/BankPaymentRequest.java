package com.monopoly.server.monopoly.classes.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BankPaymentRequest {
    private Long playerId;
    private BigDecimal amount;
    private String description;


}
