package com.monopoly.server.monopoly.classes.request;

import lombok.Data;

@Data
public class PayRentRequest {
    private Long tenantPlayerId;
    private int diceRoll = 7;
}