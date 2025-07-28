package com.monopoly.server.monopoly.classes.dto;

import com.monopoly.server.monopoly.enums.PropertyColor;
import com.monopoly.server.monopoly.enums.PropertyType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class PropertyDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private BigDecimal rent;
    private PropertyColor colorGroup;
    private PropertyType type;


}
