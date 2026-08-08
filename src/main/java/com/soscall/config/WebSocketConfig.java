package com.soscall.config;

import com.soscall.websocket.SOSWebSocketHandler;
import com.soscall.websocket.AuthHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SOSWebSocketHandler sosHandler;
    private final AuthHandshakeInterceptor authInterceptor;

    public WebSocketConfig(SOSWebSocketHandler sosHandler, AuthHandshakeInterceptor authInterceptor) {
        this.sosHandler = sosHandler;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(sosHandler, "/ws/sos")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins("*");
    }
}
