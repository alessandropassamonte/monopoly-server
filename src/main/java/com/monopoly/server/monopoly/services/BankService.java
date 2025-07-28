package com.monopoly.server.monopoly.services;

import com.monopoly.server.monopoly.classes.dto.PlayerDto;
import com.monopoly.server.monopoly.classes.dto.TransactionDto;
import com.monopoly.server.monopoly.classes.dto.WebSocketMessage;
import com.monopoly.server.monopoly.entities.GameSession;
import com.monopoly.server.monopoly.entities.Player;
import com.monopoly.server.monopoly.entities.Transaction;
import com.monopoly.server.monopoly.enums.GameStatus;
import com.monopoly.server.monopoly.enums.PlayerColor;
import com.monopoly.server.monopoly.enums.TransactionType;
import com.monopoly.server.monopoly.exceptions.InsufficientFundsException;
import com.monopoly.server.monopoly.exceptions.InvalidTransactionException;
import com.monopoly.server.monopoly.exceptions.PlayerNotFoundException;
import com.monopoly.server.monopoly.exceptions.SessionNotFoundException;
import com.monopoly.server.monopoly.repositories.GameSessionRepository;
import com.monopoly.server.monopoly.repositories.PlayerRepository;
import com.monopoly.server.monopoly.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class BankService {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private WebSocketService webSocketService;

    public TransactionDto transferMoney(Long fromPlayerId, Long toPlayerId, BigDecimal amount, String description) {
        Player fromPlayer = playerRepository.findById(fromPlayerId)
                .orElseThrow(() -> new PlayerNotFoundException("Giocatore mittente non trovato"));

        Player toPlayer = playerRepository.findById(toPlayerId)
                .orElseThrow(() -> new PlayerNotFoundException("Giocatore destinatario non trovato"));

        if (!fromPlayer.getGameSession().equals(toPlayer.getGameSession())) {
            throw new InvalidTransactionException("I giocatori devono essere nella stessa sessione");
        }

        if (fromPlayer.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Fondi insufficienti");
        }

        // Effettua il trasferimento
        fromPlayer.setBalance(fromPlayer.getBalance().subtract(amount));
        toPlayer.setBalance(toPlayer.getBalance().add(amount));

        playerRepository.save(fromPlayer);
        playerRepository.save(toPlayer);

        // Registra la transazione
        Transaction transaction = new Transaction(
                TransactionType.PLAYER_TO_PLAYER,
                amount,
                fromPlayer,
                toPlayer,
                fromPlayer.getGameSession(),
                description
        );
        transaction = transactionRepository.save(transaction);

        // Notifica via WebSocket
        webSocketService.broadcastToSession(
                fromPlayer.getGameSession().getSessionCode(),
                new WebSocketMessage("BALANCE_UPDATE",
                        fromPlayer.getGameSession().getSessionCode(),
                        Map.of(
                                "fromPlayer", mapToPlayerDto(fromPlayer),
                                "toPlayer", mapToPlayerDto(toPlayer),
                                "transaction", mapToTransactionDto(transaction)
                        ))
        );

        return mapToTransactionDto(transaction);
    }

    public TransactionDto payToBank(Long playerId, BigDecimal amount, String description) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Giocatore non trovato"));

        if (player.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Fondi insufficienti");
        }

        player.setBalance(player.getBalance().subtract(amount));
        playerRepository.save(player);

        Transaction transaction = new Transaction(
                TransactionType.PLAYER_TO_BANK,
                amount,
                player,
                null,
                player.getGameSession(),
                description
        );
        transaction = transactionRepository.save(transaction);

        webSocketService.broadcastToSession(
                player.getGameSession().getSessionCode(),
                new WebSocketMessage("BALANCE_UPDATE",
                        player.getGameSession().getSessionCode(),
                        Map.of("player", mapToPlayerDto(player)))
        );

        return mapToTransactionDto(transaction);
    }

    public TransactionDto payFromBank(Long playerId, BigDecimal amount, String description) {
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new PlayerNotFoundException("Giocatore non trovato"));

        player.setBalance(player.getBalance().add(amount));
        playerRepository.save(player);

        Transaction transaction = new Transaction(
                TransactionType.BANK_TO_PLAYER,
                amount,
                null,
                player,
                player.getGameSession(),
                description
        );
        transaction = transactionRepository.save(transaction);

        webSocketService.broadcastToSession(
                player.getGameSession().getSessionCode(),
                new WebSocketMessage("BALANCE_UPDATE",
                        player.getGameSession().getSessionCode(),
                        Map.of("player", mapToPlayerDto(player)))
        );

        return mapToTransactionDto(transaction);
    }

    public List<TransactionDto> getSessionTransactions(String sessionCode) {
        GameSession session = gameSessionRepository.findBySessionCode(sessionCode)
                .orElseThrow(() -> new SessionNotFoundException("Sessione non trovata"));

        return transactionRepository.findByGameSessionOrderByTimestampDesc(session)
                .stream()
                .map(this::mapToTransactionDto)
                .collect(Collectors.toList());
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

    private TransactionDto mapToTransactionDto(Transaction transaction) {
        return TransactionDto.builder()
                .id(transaction.getId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .fromPlayerName(transaction.getFromPlayer() != null ?
                        transaction.getFromPlayer().getName() : "Banca")
                .toPlayerName(transaction.getToPlayer() != null ?
                        transaction.getToPlayer().getName() : "Banca")
                .description(transaction.getDescription())
                .timestamp(transaction.getTimestamp())
                .build();
    }
}
