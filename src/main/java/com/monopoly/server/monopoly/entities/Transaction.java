package com.monopoly.server.monopoly.entities;

import com.monopoly.server.monopoly.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_player_id")
    private Player fromPlayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_player_id")
    private Player toPlayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private GameSession gameSession;

    private String description;

    @CreationTimestamp
    private LocalDateTime timestamp;

    // Constructors, getters, setters
    public Transaction() {}

    public Transaction(TransactionType type, BigDecimal amount, Player fromPlayer,
                       Player toPlayer, GameSession gameSession, String description) {
        this.type = type;
        this.amount = amount;
        this.fromPlayer = fromPlayer;
        this.toPlayer = toPlayer;
        this.gameSession = gameSession;
        this.description = description;
    }

    // getters and setters...
}
