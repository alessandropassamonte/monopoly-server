package com.monopoly.server.monopoly.entities;

import com.monopoly.server.monopoly.enums.GameStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "game_sessions")
@Data
public class GameSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String sessionCode;

    @Column(nullable = false)
    private String hostName;

    @Enumerated(EnumType.STRING)
    private GameStatus status;

    @OneToMany(mappedBy = "gameSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Player> players = new ArrayList<>();

    @OneToMany(mappedBy = "gameSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    // Constructors, getters, setters
    public GameSession() {
    }

    public GameSession(String sessionCode, String hostName) {
        this.sessionCode = sessionCode;
        this.hostName = hostName;
        this.status = GameStatus.WAITING;
    }

}
