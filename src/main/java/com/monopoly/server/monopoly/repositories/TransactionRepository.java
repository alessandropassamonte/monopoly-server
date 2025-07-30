package com.monopoly.server.monopoly.repositories;
import com.monopoly.server.monopoly.entities.GameSession;
import com.monopoly.server.monopoly.entities.Player;
import com.monopoly.server.monopoly.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByGameSessionOrderByTimestampDesc(GameSession gameSession);
    List<Transaction> findByFromPlayerOrToPlayerOrderByTimestampDesc(Player fromPlayer, Player toPlayer);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.toPlayer = :player AND t.type = 'BANK_TO_PLAYER'")
    BigDecimal sumBankPaymentsToPlayer(@Param("player") Player player);

    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.fromPlayer = :player AND t.type = 'PLAYER_TO_BANK'")
    BigDecimal sumPlayerPaymentsToBank(@Param("player") Player player);

    @Modifying
    @Query("DELETE FROM Transaction t WHERE t.gameSession.id = :sessionId")
    void deleteByGameSessionId(@Param("sessionId") Long sessionId);
}
