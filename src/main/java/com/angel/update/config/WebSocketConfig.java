package com.angel.update.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.angel.update.websocket.AdminWebSocketHandler;

/**
 * Configuration WebSocket pour le monitoring temps réel
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    private final AdminWebSocketHandler adminWebSocketHandler;
    
    public WebSocketConfig(AdminWebSocketHandler adminWebSocketHandler) {
        this.adminWebSocketHandler = adminWebSocketHandler;
    }
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(adminWebSocketHandler, "/ws/admin")
                .setAllowedOrigins("*"); // En production, spécifier les origines autorisées
    }
}