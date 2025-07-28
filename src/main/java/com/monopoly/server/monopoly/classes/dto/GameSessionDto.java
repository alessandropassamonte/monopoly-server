package com.monopoly.server.monopoly.classes.dto;

import com.monopoly.server.monopoly.enums.GameStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
public class GameSessionDto {
    private Long id;
    private String sessionCode;
    private String hostName;
    private GameStatus status;
    private List<PlayerDto> players;
    private LocalDateTime createdAt;

}
