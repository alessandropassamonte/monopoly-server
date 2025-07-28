package com.monopoly.server.monopoly.repositories;

import com.monopoly.server.monopoly.entities.Player;
import com.monopoly.server.monopoly.entities.Property;
import com.monopoly.server.monopoly.entities.PropertyOwnership;
import com.monopoly.server.monopoly.enums.PropertyColor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyOwnershipRepository extends JpaRepository<PropertyOwnership, Long> {
    List<PropertyOwnership> findByPlayer(Player player);
    Optional<PropertyOwnership> findByProperty(Property property);
    List<PropertyOwnership> findByPlayerAndProperty_ColorGroup(Player player, PropertyColor colorGroup);
    Optional<PropertyOwnership> findByPlayerAndProperty(Player player, Property property);
    boolean existsByProperty(Property property);

    @Query("SELECT COUNT(po) FROM PropertyOwnership po WHERE po.player.id = :playerId")
    int countByPlayerId(@Param("playerId") Long playerId);

    List<PropertyOwnership> findByPlayerId(Long playerId);
}
