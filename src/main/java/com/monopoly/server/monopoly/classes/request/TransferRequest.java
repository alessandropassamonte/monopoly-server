package com.monopoly.server.monopoly.classes.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {
    private Long fromPlayerId;
    private Long toPlayerId;
    private BigDecimal amount;
    private String description;


}
