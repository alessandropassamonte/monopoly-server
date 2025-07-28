package com.monopoly.server.monopoly.entities;

import com.monopoly.server.monopoly.enums.PlayerColor;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "players")
@Data
public class Player {
    @Version
    private int version;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    private PlayerColor color;

    @Column(nullable = false)
    private boolean isHost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private GameSession gameSession;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
    private Set<PropertyOwnership> properties = new HashSet<>();

    @CreationTimestamp
    private LocalDateTime joinedAt;

    // Constructors
    public Player() {}

    public Player(String name, PlayerColor color, GameSession gameSession, boolean isHost) {
        this.name = name;
        this.color = color;
        this.gameSession = gameSession;
        this.isHost = isHost;
        this.balance = new BigDecimal("1500"); // Starting money in Monopoly
    }

}