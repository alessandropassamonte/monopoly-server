package com.monopoly.server.monopoly.classes.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

/**
 * Request per l'acquisto di una proprietà a prezzo personalizzato
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomPurchaseRequest {
    private Long playerId;
    private BigDecimal customPrice;
}