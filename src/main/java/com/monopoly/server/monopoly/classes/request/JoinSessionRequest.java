package com.monopoly.server.monopoly.classes.request;

import com.monopoly.server.monopoly.enums.PlayerColor;
import lombok.Data;

@Data
public class JoinSessionRequest {
    private String playerName;
    private PlayerColor color;
}
