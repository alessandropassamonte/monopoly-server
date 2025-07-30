package com.monopoly.server.monopoly.repositories;

import com.monopoly.server.monopoly.entities.Player;
import com.monopoly.server.monopoly.entities.Property;
import com.monopoly.server.monopoly.entities.PropertyOwnership;
import com.monopoly.server.monopoly.enums.PropertyColor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PropertyOwnershipRepository extends JpaRepository<PropertyOwnership, Long> {

    @Query("SELECT po FROM PropertyOwnership po join po.player pl join po.property pr  WHERE po.player.id = :playerId order by pr.colorGroup")
    List<PropertyOwnership> findByPlayerById(@Param("playerId") Long playerId);

    List<PropertyOwnership> findByPlayer(Player player);

    Optional<PropertyOwnership> findByProperty(Property property);
    List<PropertyOwnership> findByPlayerAndProperty_ColorGroup(Player player, PropertyColor colorGroup);
    Optional<PropertyOwnership> findByPlayerAndProperty(Player player, Property property);
    boolean existsByProperty(Property property);

    @Query("SELECT COUNT(po) FROM PropertyOwnership po join po.player pl WHERE po.player.id = :playerId order by pl.color")
    int countByPlayerId(@Param("playerId") Long playerId);

    List<PropertyOwnership> findByPlayerId(Long playerId);

    @Modifying
    @Query("DELETE FROM PropertyOwnership po WHERE po.player.id = :playerId")
    void deleteByPlayerId(@Param("playerId") Long playerId);
}
