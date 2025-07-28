package com.monopoly.server.monopoly.services;

import com.monopoly.server.monopoly.classes.dto.GameSessionDto;
import com.monopoly.server.monopoly.classes.dto.PlayerDto;
import com.monopoly.server.monopoly.classes.dto.WebSocketMessage;
import com.monopoly.server.monopoly.entities.GameSession;
import com.monopoly.server.monopoly.entities.Player;
import com.monopoly.server.monopoly.enums.GameStatus;
import com.monopoly.server.monopoly.enums.PlayerColor;
import com.monopoly.server.monopoly.exceptions.*;
import com.monopoly.server.monopoly.repositories.GameSessionRepository;
import com.monopoly.server.monopoly.repositories.PlayerRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
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
        return PlayerDto.builder()
                .id(player.getId())
                .name(player.getName())
                .balance(player.getBalance())
                .color(player.getColor())
                .isHost(player.isHost())
                .propertiesCount(player.getProperties().size())
                .build();
    }

    public GameSessionDto getSession(String sessionCode) {
        System.out.println("=== GETTING SESSION: " + sessionCode + " ===");

        GameSession session = gameSessionRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new SessionNotFoundException("Sessione non trovata"));

        System.out.println("Session found, players count: " + session.getPlayers().size());
        session.getPlayers().forEach(p ->
                System.out.println("Player: " + p.getName() + ", isHost: " + p.isHost())
        );

        return mapToDto(session);
    }

    public void endSession(String sessionCode, Long hostPlayerId) {
        GameSession session = gameSessionRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new SessionNotFoundException("Sessione non trovata"));

        Player host = playerRepository.findById(hostPlayerId)
                .orElseThrow(() -> new PlayerNotFoundException("Host non trovato"));

        if (!host.isHost() || !host.getGameSession().equals(session)) {
            throw new UnauthorizedException("Solo l'host può terminare la partita");
        }

        session.setStatus(GameStatus.FINISHED);
        gameSessionRepository.save(session);

        webSocketService.broadcastToSession(sessionCode,
                new WebSocketMessage("GAME_ENDED", sessionCode, null));
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