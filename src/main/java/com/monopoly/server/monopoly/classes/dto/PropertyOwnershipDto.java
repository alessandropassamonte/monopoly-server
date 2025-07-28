package com.monopoly.server.monopoly.classes.dto;

import com.monopoly.server.monopoly.entities.Player;
import com.monopoly.server.monopoly.entities.Property;
import com.monopoly.server.monopoly.enums.PropertyColor;
import com.monopoly.server.monopoly.enums.PropertyType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
@Data
public class PropertyOwnershipDto {
    private Long id;
    private Long propertyId;
    private String propertyName;
    private BigDecimal propertyPrice;
    private PropertyType propertyType;
    private PropertyColor colorGroup;
    private int houses;
    private boolean hasHotel;
    private boolean isMortgaged;
    private BigDecimal currentRent;
    private LocalDateTime purchasedAt;
}
