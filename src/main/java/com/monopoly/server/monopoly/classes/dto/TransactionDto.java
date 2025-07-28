package com.monopoly.server.monopoly.classes.dto;

import com.monopoly.server.monopoly.enums.TransactionType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Data
public class TransactionDto {
    private Long id;
    private TransactionType type;
    private BigDecimal amount;
    private String fromPlayerName;
    private String toPlayerName;
    private String description;
    private LocalDateTime timestamp;


}
