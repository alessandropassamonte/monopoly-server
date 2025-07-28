package com.monopoly.server.monopoly.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TransactionType {
    PLAYER_TO_PLAYER, PLAYER_TO_BANK, BANK_TO_PLAYER,
    PROPERTY_PURCHASE, RENT_PAYMENT, TAX_PAYMENT, SALARY
}
