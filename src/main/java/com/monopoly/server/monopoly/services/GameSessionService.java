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

    public GameSessionDto createSession(String hostName) {
        String sessionCode = generateSessionCode();
        GameSession session = new GameSession(sessionCode, hostName);
        session = gameSessionRepository.save(session);

        // Crea il player host
        Player hostPlayer = new Player(hostName, PlayerColor.RED, session, true);
        playerRepository.save(hostPlayer);

        return mapToDto(session);
    }

    public GameSessionDto joinSession(String sessionCode, String playerName, PlayerColor color) {
        GameSession session = gameSessionRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new SessionNotFoundException("Sessione non trovata"));

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

        Player newPlayer = new Player(playerName, color, session, false);
        playerRepository.save(newPlayer);

        // Notifica agli altri giocatori
        webSocketService.broadcastToSession(sessionCode,
                new WebSocketMessage("PLAYER_JOINED", sessionCode, mapToPlayerDto(newPlayer)));

        return mapToDto(session);
    }

    public void startGame(String sessionCode, Long hostPlayerId) {
        GameSession session = gameSessionRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new SessionNotFoundException("Sessione non trovata"));

        Player host = playerRepository.findById(hostPlayerId)
                .orElseThrow(() -> new PlayerNotFoundException("Host non trovato"));

        if (!host.isHost() || !host.getGameSession().equals(session)) {
            throw new UnauthorizedException("Solo l'host può iniziare la partita");
        }

        if (session.getPlayers().size() < 2) {
            throw new NotEnoughPlayersException("Servono almeno 2 giocatori");
        }

        session.setStatus(GameStatus.IN_PROGRESS);
        gameSessionRepository.save(session);

        webSocketService.broadcastToSession(sessionCode,
                new WebSocketMessage("GAME_STARTED", sessionCode, null));
    }

    private String generateSessionCode() {
        return StringUtils.leftPad(String.valueOf((int)(Math.random() * 10000)), 4, "0");
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
        GameSession session = gameSessionRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new SessionNotFoundException("Sessione non trovata"));
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