package com.angel.update.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Gestionnaire WebSocket pour l'interface d'administration
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminWebSocketHandler extends TextWebSocketHandler {
    
    private final ObjectMapper objectMapper;
    private final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("Admin WebSocket connection established: {}", session.getId());
        
        // Envoyer un message de bienvenue
        sendMessage(session, new WebSocketMessage("connection", "connected", Map.of(
            "sessionId", session.getId(),
            "timestamp", LocalDateTime.now()
        )));
    }
    
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("Received WebSocket message from {}: {}", session.getId(), message.getPayload());
        
        try {
            WebSocketMessage wsMessage = objectMapper.readValue(message.getPayload(), WebSocketMessage.class);
            handleIncomingMessage(session, wsMessage);
        } catch (Exception e) {
            log.error("Error processing WebSocket message from {}", session.getId(), e);
            sendError(session, "Invalid message format");
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("Admin WebSocket connection closed: {} with status: {}", session.getId(), status);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error for session {}", session.getId(), exception);
        sessions.remove(session);
    }
    
    /**
     * Traite les messages entrants
     */
    private void handleIncomingMessage(WebSocketSession session, WebSocketMessage message) {
        switch (message.getType()) {
            case "ping":
                sendMessage(session, new WebSocketMessage("pong", "pong", Map.of("timestamp", LocalDateTime.now())));
                break;
            case "subscribe":
                handleSubscription(session, message);
                break;
            case "unsubscribe":
                handleUnsubscription(session, message);
                break;
            default:
                log.warn("Unknown message type: {}", message.getType());
                sendError(session, "Unknown message type: " + message.getType());
        }
    }
    
    /**
     * Gère les abonnements aux événements
     */
    private void handleSubscription(WebSocketSession session, WebSocketMessage message) {
        String eventType = (String) message.getData().get("eventType");
        if (eventType != null) {
            // Stocker les abonnements en attributs de session
            @SuppressWarnings("unchecked")
            CopyOnWriteArraySet<String> subscriptions = (CopyOnWriteArraySet<String>) 
                session.getAttributes().computeIfAbsent("subscriptions", k -> new CopyOnWriteArraySet<>());
            subscriptions.add(eventType);
            
            log.info("Session {} subscribed to {}", session.getId(), eventType);
            sendMessage(session, new WebSocketMessage("subscribed", eventType, Map.of(
                "eventType", eventType,
                "timestamp", LocalDateTime.now()
            )));
        }
    }
    
    /**
     * Gère les désabonnements
     */
    private void handleUnsubscription(WebSocketSession session, WebSocketMessage message) {
        String eventType = (String) message.getData().get("eventType");
        if (eventType != null) {
            @SuppressWarnings("unchecked")
            CopyOnWriteArraySet<String> subscriptions = (CopyOnWriteArraySet<String>) 
                session.getAttributes().get("subscriptions");
            if (subscriptions != null) {
                subscriptions.remove(eventType);
                log.info("Session {} unsubscribed from {}", session.getId(), eventType);
            }
        }
    }
    
    /**
     * Diffuse un message à toutes les sessions connectées
     */
    public void broadcast(WebSocketMessage message) {
        String messageJson = toJson(message);
        if (messageJson != null) {
            sessions.removeIf(session -> {
                try {
                    if (session.isOpen()) {
                        // Vérifier si la session est abonnée à ce type d'événement
                        if (isSubscribedToEvent(session, message.getType())) {
                            session.sendMessage(new TextMessage(messageJson));
                        }
                        return false;
                    } else {
                        return true; // Supprimer les sessions fermées
                    }
                } catch (IOException e) {
                    log.error("Error sending message to session {}", session.getId(), e);
                    return true; // Supprimer les sessions en erreur
                }
            });
        }
    }
    
    /**
     * Diffuse une mise à jour de collecteur
     */
    public void broadcastCollectorUpdate(String collectorName, String status, String message) {
        broadcast(new WebSocketMessage("collector-update", collectorName, Map.of(
            "collectorName", collectorName,
            "status", status,
            "message", message,
            "timestamp", LocalDateTime.now()
        )));
    }
    
    /**
     * Diffuse une nouvelle entrée de log
     */
    public void broadcastLogEntry(String level, String logger, String message, String exception) {
        broadcast(new WebSocketMessage("new-log", level, Map.of(
            "level", level,
            "logger", logger,
            "message", message,
            "exception", exception != null ? exception : "",
            "timestamp", LocalDateTime.now()
        )));
    }
    
    /**
     * Diffuse une mise à jour des statistiques
     */
    public void broadcastStatsUpdate(Map<String, Object> stats) {
        broadcast(new WebSocketMessage("stats-update", "stats", stats));
    }
    
    /**
     * Diffuse une alerte
     */
    public void broadcastAlert(String level, String title, String description) {
        broadcast(new WebSocketMessage("alert", level, Map.of(
            "level", level,
            "title", title,
            "description", description,
            "timestamp", LocalDateTime.now()
        )));
    }
    
    /**
     * Envoie un message à une session spécifique
     */
    private void sendMessage(WebSocketSession session, WebSocketMessage message) {
        try {
            if (session.isOpen()) {
                String messageJson = toJson(message);
                if (messageJson != null) {
                    session.sendMessage(new TextMessage(messageJson));
                }
            }
        } catch (IOException e) {
            log.error("Error sending message to session {}", session.getId(), e);
        }
    }
    
    /**
     * Envoie un message d'erreur
     */
    private void sendError(WebSocketSession session, String error) {
        sendMessage(session, new WebSocketMessage("error", "error", Map.of(
            "message", error,
            "timestamp", LocalDateTime.now()
        )));
    }
    
    /**
     * Vérifie si une session est abonnée à un type d'événement
     */
    private boolean isSubscribedToEvent(WebSocketSession session, String eventType) {
        @SuppressWarnings("unchecked")
        CopyOnWriteArraySet<String> subscriptions = (CopyOnWriteArraySet<String>) 
            session.getAttributes().get("subscriptions");
        return subscriptions == null || subscriptions.isEmpty() || subscriptions.contains(eventType);
    }
    
    /**
     * Convertit un objet en JSON
     */
    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.error("Error converting object to JSON", e);
            return null;
        }
    }
    
    /**
     * Obtient le nombre de sessions actives
     */
    public int getActiveSessionsCount() {
        return sessions.size();
    }
    
    /**
     * Classe pour les messages WebSocket
     */
    public static class WebSocketMessage {
        private String type;
        private String event;
        private Map<String, Object> data;
        
        public WebSocketMessage() {}
        
        public WebSocketMessage(String type, String event, Map<String, Object> data) {
            this.type = type;
            this.event = event;
            this.data = data;
        }
        
        // Getters et setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getEvent() { return event; }
        public void setEvent(String event) { this.event = event; }
        
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
    }
}