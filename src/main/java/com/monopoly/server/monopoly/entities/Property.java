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

    @Column(name = "rent_house_1")
    private BigDecimal rentWith1House;
    @Column(name = "rent_house_2")
    private BigDecimal rentWith2Houses;
    @Column(name = "rent_house_3")
    private BigDecimal rentWith3Houses;
    @Column(name = "rent_house_4")
    private BigDecimal rentWith4Houses;
    @Column(name = "rent_hotel")
    private BigDecimal rentHotel;

    @Enumerated(EnumType.STRING)
    private PropertyColor colorGroup;

    @Enumerated(EnumType.STRING)
    private PropertyType type; // STREET, RAILROAD, UTILITY, SPECIAL





    // getters and setters...
}
