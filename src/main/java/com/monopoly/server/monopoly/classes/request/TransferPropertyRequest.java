package com.monopoly.server.monopoly.classes.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferPropertyRequest {
    private Long newOwnerId;
    private BigDecimal price; // Può essere null per trasferimenti gratuiti
}
