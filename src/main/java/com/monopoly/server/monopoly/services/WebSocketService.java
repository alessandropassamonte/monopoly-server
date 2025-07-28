package com.monopoly.server.monopoly.services;

import com.monopoly.server.monopoly.classes.dto.WebSocketMessage;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void broadcastToSession(String sessionCode, WebSocketMessage message) {
        messagingTemplate.convertAndSend("/topic/session/" + sessionCode, message);
    }

    public void sendToPlayer(String sessionCode, Long playerId, WebSocketMessage message) {
        messagingTemplate.convertAndSend(
                "/topic/session/" + sessionCode + "/player/" + playerId,
                message
        );
    }
}