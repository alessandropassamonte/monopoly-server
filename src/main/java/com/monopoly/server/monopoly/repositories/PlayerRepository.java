package com.monopoly.server.monopoly.repositories;
import com.monopoly.server.monopoly.entities.GameSession;
import com.monopoly.server.monopoly.entities.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {
    List<Player> findByGameSession(GameSession gameSession);
    List<Player> findByGameSessionAndIsHost(GameSession gameSession, boolean isHost);
    Optional<Player> findByGameSessionAndName(GameSession gameSession, String name);
}
