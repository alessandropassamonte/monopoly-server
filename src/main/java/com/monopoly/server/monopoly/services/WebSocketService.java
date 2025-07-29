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
        try {
            System.out.println("📡 Broadcasting WebSocket message to session " + sessionCode + ": " + message.getType());
            messagingTemplate.convertAndSend("/topic/session/" + sessionCode, message);
            System.out.println("✅ WebSocket message sent successfully");
        } catch (Exception e) {
            System.err.println("❌ Error sending WebSocket message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendToPlayer(String sessionCode, Long playerId, WebSocketMessage message) {
        try {
            System.out.println("📡 Sending WebSocket message to player " + playerId + " in session " + sessionCode);
            messagingTemplate.convertAndSend(
                    "/topic/session/" + sessionCode + "/player/" + playerId,
                    message
            );
            System.out.println("✅ WebSocket message sent to player successfully");
        } catch (Exception e) {
            System.err.println("❌ Error sending WebSocket message to player: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // AGGIUNTO: Metodo per inviare aggiornamenti di sessione
    public void broadcastSessionUpdate(String sessionCode, Object sessionData) {
        WebSocketMessage message = new WebSocketMessage("SESSION_UPDATE", sessionCode, sessionData);
        broadcastToSession(sessionCode, message);
    }

    // AGGIUNTO: Metodo per inviare aggiornamenti di balance
    public void broadcastBalanceUpdate(String sessionCode, Object balanceData) {
        WebSocketMessage message = new WebSocketMessage("BALANCE_UPDATE", sessionCode, balanceData);
        broadcastToSession(sessionCode, message);
    }

    // AGGIUNTO: Metodo per inviare aggiornamenti di proprietà
    public void broadcastPropertyUpdate(String sessionCode, Object propertyData) {
        WebSocketMessage message = new WebSocketMessage("PROPERTY_UPDATE", sessionCode, propertyData);
        broadcastToSession(sessionCode, message);
    }

    // AGGIUNTO: Metodo per inviare notifiche di transazione
    public void broadcastTransactionUpdate(String sessionCode, Object transactionData) {
        WebSocketMessage message = new WebSocketMessage("TRANSACTION_UPDATE", sessionCode, transactionData);
        broadcastToSession(sessionCode, message);
    }
}