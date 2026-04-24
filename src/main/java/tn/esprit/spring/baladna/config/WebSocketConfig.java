package tn.esprit.spring.baladna.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket STOMP configuration for real-time chat messaging
 * 
 * This configuration sets up Spring's built-in message broker for handling
 * real-time communication between the backend and frontend using STOMP protocol
 * over WebSocket with SockJS fallback.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ChatWebSocketHandshakeInterceptor handshakeInterceptor;
    private final TaskScheduler taskScheduler;

    /**
     * Configure STOMP endpoints and enable SockJS fallback
     * 
     * Frontend connects to: ws://localhost:8081/api/chat?userId={userId}
     * or with SockJS: http://localhost:8081/api/chat?userId={userId}
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/api/chat")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .withSockJS();
    }

    /**
     * Configure message broker for STOMP messaging
     * 
     * - Application destination prefix: /app (for client to server messages)
     * - User destination prefix: /user (for private messages to specific users)
     * - Broker destination prefix: /topic (for broadcast messages)
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple message broker for /topic prefix with TaskScheduler
        config.enableSimpleBroker("/topic", "/queue")
                .setTaskScheduler(taskScheduler)
                .setHeartbeatValue(new long[]{25000, 25000});

        // Set prefix for send destinations (client to server)
        config.setApplicationDestinationPrefixes("/app");

        // Set prefix for receive destinations (server to client)
        config.setUserDestinationPrefix("/user");
    }
}

