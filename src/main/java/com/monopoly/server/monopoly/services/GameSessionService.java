package com.monopoly.server.monopoly.services;

import com.monopoly.server.monopoly.classes.dto.GameSessionDto;
import com.monopoly.server.monopoly.classes.dto.PlayerDto;
import com.monopoly.server.monopoly.classes.dto.WebSocketMessage;
import com.monopoly.server.monopoly.entities.GameSession;
import com.monopoly.server.monopoly.entities.Player;
import com.monopoly.server.monopoly.enums.GameStatus;
import com.monopoly.server.monopoly.enums.PlayerColor;
import com.monopoly.server.monopoly.exceptions.*;
import com.monopoly.server.monopoly.repositories.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GameSessionService {

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PropertyOwnershipRepository propertyOwnershipRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private WebSocketService webSocketService;

    @PersistenceContext
    private EntityManager entityManager;

    public GameSessionDto createSession(String hostName) {
        System.out.println("=== CREATING SESSION FOR HOST: " + hostName + " ===");

        String sessionCode = generateSessionCode();
        System.out.println("Generated session code: " + sessionCode);

        // 1. Crea e salva la sessione
        GameSession session = new GameSession(sessionCode, hostName);
        session = gameSessionRepository.save(session);
        System.out.println("Session saved with ID: " + session.getId());

        // 2. Forza il flush per assicurarsi che sia persistita
        entityManager.flush();

        // 3. Crea il player host
        Player hostPlayer = new Player(hostName, PlayerColor.RED, session, true);
        hostPlayer = playerRepository.save(hostPlayer);
        System.out.println("Host player created with ID: " + hostPlayer.getId() + ", isHost: " + hostPlayer.isHost());

        // 4. Forza di nuovo il flush
        entityManager.flush();

        // 5. Ricarica la sessione dal database con i players
        entityManager.clear(); // Pulisce il cache di primo livello
        session = gameSessionRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new RuntimeException("Sessione appena creata non trovata"));

        System.out.println("Session reloaded, players count: " + session.getPlayers().size());
        session.getPlayers().forEach(p ->
                System.out.println("Player: " + p.getName() + ", isHost: " + p.isHost() + ", ID: " + p.getId())
        );

        GameSessionDto dto = mapToDto(session);
        System.out.println("DTO created with players count: " + dto.getPlayers().size());
        dto.getPlayers().forEach(p ->
                System.out.println("DTO Player: " + p.getName() + ", isHost: " + p.isHost() + ", ID: " + p.getId())
        );

        return dto;
    }

    public GameSessionDto joinSession(String sessionCode, String playerName, PlayerColor color) {
        System.out.println("=== JOINING SESSION: " + sessionCode + " AS: " + playerName + " ===");

        GameSession session = gameSessionRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new SessionNotFoundException("Sessione non trovata"));

        System.out.println("Session found, current players: " + session.getPlayers().size());

        if (session.getStatus() != GameStatus.WAITING) {
            throw new SessionNotJoinableException("La sessione non è più aperta");
        }

        if (session.getPlayers().size() >= 8) {
            throw new SessionFullException("Sessione piena");
        }

        // Controlla se il colore è già preso
        boolean colorTaken = session.getPlayers().stream()
                .anyMatch(p -> p.getColor() == color);
        if (colorTaken) {
            throw new ColorTakenException("Colore già scelto da un altro giocatore");
        }

        // Crea il nuovo player
        Player newPlayer = new Player(playerName, color, session, false);
        newPlayer = playerRepository.save(newPlayer);
        System.out.println("New player created: " + newPlayer.getName() + ", ID: " + newPlayer.getId());

        // Flush e ricarica
        entityManager.flush();
        entityManager.clear();

        session = gameSessionRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new SessionNotFoundException("Sessione non trovata"));

        System.out.println("Session reloaded after join, players count: " + session.getPlayers().size());

        // Notifica agli altri giocatori
        webSocketService.broadcastToSession(sessionCode,
                new WebSocketMessage("PLAYER_JOINED", sessionCode, mapToPlayerDto(newPlayer)));

        return mapToDto(session);
    }

    public void startGame(String sessionCode, Long hostPlayerId) {
        System.out.println("=== STARTING GAME: " + sessionCode + " BY HOST ID: " + hostPlayerId + " ===");

        GameSession session = gameSessionRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new SessionNotFoundException("Sessione non trovata"));

        Player host = playerRepository.findById(hostPlayerId)
                .orElseThrow(() -> new PlayerNotFoundException("Host non trovato"));

        System.out.println("Host player found: " + host.getName() + ", isHost: " + host.isHost());
        System.out.println("Host session ID: " + host.getGameSession().getId() + ", current session ID: " + session.getId());

        if (!host.isHost() || !host.getGameSession().getId().equals(session.getId())) {
            throw new UnauthorizedException("Solo l'host può iniziare la partita");
        }

        if (session.getPlayers().size() < 2) {
            throw new NotEnoughPlayersException("Servono almeno 2 giocatori");
        }

        session.setStatus(GameStatus.IN_PROGRESS);
        gameSessionRepository.save(session);

        webSocketService.broadcastToSession(sessionCode,
                new WebSocketMessage("GAME_STARTED", sessionCode, null));

        System.out.println("Game started successfully");
    }

    private String generateSessionCode() {
        String code;
        do {
            code = StringUtils.leftPad(String.valueOf((int)(Math.random() * 10000)), 4, "0");
        } while (gameSessionRepository.findBySessionCode(code).isPresent());
        return code;
    }

    private GameSessionDto mapToDto(GameSession session) {
        return GameSessionDto.builder()
                .id(session.getId())
                .sessionCode(session.getSessionCode())
                .hostName(session.getHostName())
                .status(session.getStatus())
                .players(session.getPlayers().stream()
                        .map(this::mapToPlayerDto)
                        .collect(Collectors.toList()))
                .createdAt(session.getCreatedAt())
                .build();
    }

    private PlayerDto mapToPlayerDto(Player player) {
        int propertiesCount = 0;
        try {
            // Usa il repository corretto per contare le proprietà
            propertiesCount = propertyOwnershipRepository.countByPlayerId(player.getId());
        } catch (Exception e) {
            System.err.println("Errore nel conteggio delle proprietà per il giocatore " + player.getId());
            propertiesCount = 0;
        }

        return PlayerDto.builder()
                .id(player.getId())
                .name(player.getName())
                .balance(player.getBalance())
                .color(player.getColor())
                .isHost(player.isHost())
                .propertiesCount(propertiesCount)
                .build();
    }
    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    public GameSessionDto getSession(String sessionCode) {
        System.out.println("=== GETTING SESSION: " + sessionCode + " ===");

        GameSession session = gameSessionRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new SessionNotFoundException("Sessione non trovata"));

        // Forza l'inizializzazione delle collezioni in modo controllato
        session.getPlayers().forEach(player -> {
            try {
                // Inizializza la collezione properties se non è già caricata
                if (player.getProperties() instanceof org.hibernate.collection.spi.PersistentCollection) {
                    org.hibernate.Hibernate.initialize(player.getProperties());
                }
            } catch (Exception e) {
                System.err.println("Errore nell'inizializzazione delle proprietà per il giocatore " + player.getId());
            }
        });

        return mapToDto(session);
    }


    @Transactional
    public void deleteSession(String sessionCode, Long hostPlayerId) {
        System.out.println("=== DELETING SESSION COMPLETELY: " + sessionCode + " ===");

        GameSession session = gameSessionRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new SessionNotFoundException("Sessione non trovata"));

        Player host = playerRepository.findById(hostPlayerId)
                .orElseThrow(() -> new PlayerNotFoundException("Host non trovato"));

        if (!host.isHost() || !host.getGameSession().equals(session)) {
            throw new UnauthorizedException("Solo l'host può eliminare la partita");
        }

        // 1. Notifica a tutti i giocatori che la sessione sta per essere eliminata
        webSocketService.broadcastToSession(sessionCode,
                new WebSocketMessage("SESSION_DELETED", sessionCode, null));

        try {
            Long sessionId = session.getId();
            System.out.println("Deleting all data for session ID: " + sessionId);

            // 2. Usa SQL nativo per eliminare tutto senza controlli di versioning

            // Elimina tutte le proprietà possedute dai giocatori di questa sessione
            entityManager.createNativeQuery(
                            "DELETE FROM property_ownership WHERE player_id IN " +
                                    "(SELECT id FROM players WHERE session_id = ?)")
                    .setParameter(1, sessionId)
                    .executeUpdate();
            System.out.println("Properties deleted");

            // Elimina tutte le transazioni della sessione
            entityManager.createNativeQuery(
                            "DELETE FROM transactions WHERE session_id = ?")
                    .setParameter(1, sessionId)
                    .executeUpdate();
            System.out.println("Transactions deleted");

            // Elimina tutti i giocatori della sessione (SENZA controllo version)
            entityManager.createNativeQuery(
                            "DELETE FROM players WHERE session_id = ?")
                    .setParameter(1, sessionId)
                    .executeUpdate();
            System.out.println("Players deleted");

            // Elimina la sessione stessa
            entityManager.createNativeQuery(
                            "DELETE FROM game_sessions WHERE id = ?")
                    .setParameter(1, sessionId)
                    .executeUpdate();
            System.out.println("Session deleted");

            // Flush finale per assicurarsi che tutto sia persistito
            entityManager.flush();

            System.out.println("=== SESSION " + sessionCode + " DELETED COMPLETELY ===");

        } catch (Exception e) {
            System.err.println("Error during session deletion: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Errore durante l'eliminazione della sessione", e);
        }
    }

    public void endSession(String sessionCode, Long hostPlayerId) {
        System.out.println("=== ENDING SESSION (SOFT): " + sessionCode + " ===");

        GameSession session = gameSessionRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new SessionNotFoundException("Sessione non trovata"));

        Player host = playerRepository.findById(hostPlayerId)
                .orElseThrow(() -> new PlayerNotFoundException("Host non trovato"));

        if (!host.isHost() || !host.getGameSession().equals(session)) {
            throw new UnauthorizedException("Solo l'host può terminare la partita");
        }

        // Cambia solo lo status, mantenendo i dati per eventuali statistiche
        session.setStatus(GameStatus.FINISHED);
        gameSessionRepository.save(session);

        webSocketService.broadcastToSession(sessionCode,
                new WebSocketMessage("GAME_ENDED", sessionCode, null));

        System.out.println("Session ended (status changed to FINISHED)");
    }

    public List<GameSessionDto> getActiveSessions() {
        return gameSessionRepository.findByStatus(GameStatus.WAITING)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Scheduled(fixedRate = 3600000) // Ogni ora
    public void cleanupOldSessions() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        gameSessionRepository.deleteOldSessions(cutoffTime);
    }
}