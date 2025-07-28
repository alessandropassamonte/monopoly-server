package com.monopoly.server.monopoly.classes.dto;

import com.monopoly.server.monopoly.enums.PlayerColor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class PlayerDto {
    private Long id;
    private String name;
    private BigDecimal balance;
    private PlayerColor color;
    private boolean isHost;
    private int propertiesCount;

}
