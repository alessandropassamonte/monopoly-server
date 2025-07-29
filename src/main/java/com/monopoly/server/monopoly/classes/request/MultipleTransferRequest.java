package com.monopoly.server.monopoly.classes.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class MultipleTransferRequest {
    private List<Long> ownershipIds;
    private Long newOwnerId;
    private BigDecimal compensationAmount; // Positivo = nuovo proprietario paga, Negativo = nuovo proprietario riceve
    private String description;
}
