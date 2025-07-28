package com.monopoly.server.monopoly.repositories;

import com.monopoly.server.monopoly.entities.GameSession;
import com.monopoly.server.monopoly.enums.GameStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {
    Optional<GameSession> findBySessionCode(String sessionCode);
    List<GameSession> findByStatus(GameStatus status);

    @Modifying
    @Query("DELETE FROM GameSession g WHERE g.createdAt < :cutoffTime")
    void deleteOldSessions(@Param("cutoffTime") LocalDateTime cutoffTime);
}
