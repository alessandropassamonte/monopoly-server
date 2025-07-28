package com.monopoly.server.monopoly.classes.dto;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class WebSocketMessage {
    private String type;
    private String sessionCode;
    private Object data;
    private LocalDateTime timestamp;

    public WebSocketMessage(String type, String sessionCode, Object data) {
        this.type = type;
        this.sessionCode = sessionCode;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }


}
