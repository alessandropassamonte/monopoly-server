package com.monopoly.server.monopoly.entities;

import com.monopoly.server.monopoly.enums.PropertyColor;
import com.monopoly.server.monopoly.enums.PropertyType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "properties")
@Data
public class Property {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private BigDecimal rent;

    @Enumerated(EnumType.STRING)
    private PropertyColor colorGroup;

    @Enumerated(EnumType.STRING)
    private PropertyType type; // STREET, RAILROAD, UTILITY, SPECIAL

    // getters and setters...
}
