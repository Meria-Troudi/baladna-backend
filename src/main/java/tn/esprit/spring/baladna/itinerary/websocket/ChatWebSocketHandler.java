package tn.esprit.spring.baladna.itinerary.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tn.esprit.spring.baladna.itinerary.dto.ChatMessageRequest;
import tn.esprit.spring.baladna.itinerary.dto.ChatMessageResponse;
import tn.esprit.spring.baladna.itinerary.dto.ChatWebSocketMessage;
import tn.esprit.spring.baladna.itinerary.service.ChatMessageService;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * WebSocket handler for real-time chat messaging between itinerary collaborators
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatMessageService chatMessageService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // Map to store sessions: itineraryId -> (userId -> WebSocketSession)
    private static final Map<String, Map<Long, WebSocketSession>> sessions = new ConcurrentHashMap<>();

    // Map to track user info: sessionId -> (itineraryId, userId)
    private static final Map<String, ChatSessionInfo> sessionInfo = new ConcurrentHashMap<>();

    /**
     * Handle a new WebSocket connection
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String itineraryId = extractItineraryId(session);
        Long userId = extractUserId(session);

        if (itineraryId == null || userId == null) {
            log.warn("Invalid connection attempt: missing itineraryId or userId");
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        // Store the session
        sessions.computeIfAbsent(itineraryId, k -> new ConcurrentHashMap<>())
                .put(userId, session);

        sessionInfo.put(session.getId(), new ChatSessionInfo(itineraryId, userId));

        log.info("User {} connected to itinerary chat {}, total users: {}",
                userId, itineraryId, sessions.get(itineraryId).size());

        // Notify others that a user joined
        broadcastUserJoined(itineraryId, userId);
    }

    /**
     * Handle incoming WebSocket message
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            ChatSessionInfo info = sessionInfo.get(session.getId());
            if (info == null) {
                log.warn("Received message from unknown session");
                return;
            }

            ChatWebSocketMessage wsMessage = objectMapper.readValue(
                    message.getPayload(), ChatWebSocketMessage.class);

            String type = wsMessage.getType();

            switch (type) {
                case "SEND":
                    handleSendMessage(info.getItineraryId(), info.getUserId(), wsMessage);
                    break;
                case "EDIT":
                    handleEditMessage(info.getItineraryId(), info.getUserId(), wsMessage);
                    break;
                case "DELETE":
                    handleDeleteMessage(info.getItineraryId(), info.getUserId(), wsMessage);
                    break;
                case "TYPING":
                    broadcastTypingStatus(info.getItineraryId(), info.getUserId(), wsMessage.getSenderName());
                    break;
                default:
                    log.warn("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            log.error("Error handling WebSocket message", e);
            sendErrorMessage(session, "Error processing message: " + e.getMessage());
        }
    }

    /**
     * Handle WebSocket connection closure
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        ChatSessionInfo info = sessionInfo.remove(session.getId());

        if (info != null) {
            String itineraryId = info.getItineraryId();
            Long userId = info.getUserId();

            // Remove the user session
            Map<Long, WebSocketSession> userSessions = sessions.get(itineraryId);
            if (userSessions != null) {
                userSessions.remove(userId);

                if (userSessions.isEmpty()) {
                    sessions.remove(itineraryId);
                }
            }

            log.info("User {} disconnected from itinerary chat {}", userId, itineraryId);

            // Notify others that user left
            broadcastUserLeft(itineraryId, userId);
        }
    }

    /**
     * Handle error in WebSocket connection
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket error for session {}: {}", session.getId(), exception.getMessage());
    }

    /**
     * Handle sending a new message
     */
    private void handleSendMessage(String itineraryId, Long senderId,
                                    ChatWebSocketMessage wsMessage) throws IOException {
        try {
            ChatMessageRequest request = new ChatMessageRequest();
            request.setContent(wsMessage.getContent());

            UUID itineraryUUID = UUID.fromString(itineraryId);
            ChatMessageResponse response = chatMessageService.sendMessage(
                    itineraryUUID, senderId, request);

            // Prepare WebSocket response
            ChatWebSocketMessage broadcastMessage = ChatWebSocketMessage.builder()
                    .type("SEND")
                    .id(response.getId())
                    .itineraryId(response.getItineraryId())
                    .senderId(response.getSenderId())
                    .senderName(response.getSenderName())
                    .content(response.getContent())
                    .timestamp(response.getCreatedAt())
                    .status("SUCCESS")
                    .build();

            broadcastToItinerary(itineraryId, broadcastMessage);

        } catch (Exception e) {
            log.error("Error sending message", e);
            ChatWebSocketMessage errorMessage = ChatWebSocketMessage.builder()
                    .type("ERROR")
                    .status("FAILED")
                    .error(e.getMessage())
                    .build();
            broadcastToUser(itineraryId, senderId, errorMessage);
        }
    }

    /**
     * Handle editing a message
     */
    private void handleEditMessage(String itineraryId, Long senderId,
                                    ChatWebSocketMessage wsMessage) throws IOException {
        try {
            ChatMessageRequest request = new ChatMessageRequest();
            request.setContent(wsMessage.getContent());

            ChatMessageResponse response = chatMessageService.updateMessage(
                    wsMessage.getId(), senderId, request);

            ChatWebSocketMessage broadcastMessage = ChatWebSocketMessage.builder()
                    .type("EDIT")
                    .id(response.getId())
                    .itineraryId(response.getItineraryId())
                    .senderId(response.getSenderId())
                    .senderName(response.getSenderName())
                    .content(response.getContent())
                    .timestamp(response.getUpdatedAt())
                    .status("SUCCESS")
                    .build();

            broadcastToItinerary(itineraryId, broadcastMessage);

        } catch (Exception e) {
            log.error("Error editing message", e);
            ChatWebSocketMessage errorMessage = ChatWebSocketMessage.builder()
                    .type("ERROR")
                    .status("FAILED")
                    .error(e.getMessage())
                    .build();
            broadcastToUser(itineraryId, senderId, errorMessage);
        }
    }

    /**
     * Handle deleting a message
     */
    private void handleDeleteMessage(String itineraryId, Long senderId,
                                      ChatWebSocketMessage wsMessage) throws IOException {
        try {
            UUID itineraryUUID = UUID.fromString(itineraryId);
            chatMessageService.deleteMessage(wsMessage.getId(), senderId, itineraryUUID);

            ChatWebSocketMessage broadcastMessage = ChatWebSocketMessage.builder()
                    .type("DELETE")
                    .id(wsMessage.getId())
                    .itineraryId(UUID.fromString(itineraryId))
                    .senderId(senderId)
                    .timestamp(LocalDateTime.now())
                    .status("SUCCESS")
                    .build();

            broadcastToItinerary(itineraryId, broadcastMessage);

        } catch (Exception e) {
            log.error("Error deleting message", e);
            ChatWebSocketMessage errorMessage = ChatWebSocketMessage.builder()
                    .type("ERROR")
                    .status("FAILED")
                    .error(e.getMessage())
                    .build();
            broadcastToUser(itineraryId, senderId, errorMessage);
        }
    }

    /**
     * Broadcast user joined event
     */
    private void broadcastUserJoined(String itineraryId, Long userId) throws IOException {
        User user = userRepository.findById(userId).orElse(null);
        String userName = user != null ? buildDisplayName(user) : "Unknown User";

        ChatWebSocketMessage message = ChatWebSocketMessage.builder()
                .type("USER_JOINED")
                .senderId(userId)
                .senderName(userName)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToItinerary(itineraryId, message);
    }

    /**
     * Broadcast user left event
     */
    private void broadcastUserLeft(String itineraryId, Long userId) throws IOException {
        ChatWebSocketMessage message = ChatWebSocketMessage.builder()
                .type("USER_LEFT")
                .senderId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToItinerary(itineraryId, message);
    }

    /**
     * Broadcast typing status
     */
    private void broadcastTypingStatus(String itineraryId, Long userId, String userName) throws IOException {
        ChatWebSocketMessage message = ChatWebSocketMessage.builder()
                .type("TYPING")
                .senderId(userId)
                .senderName(userName)
                .timestamp(LocalDateTime.now())
                .build();

        broadcastToItineraryExcept(itineraryId, userId, message);
    }

    /**
     * Broadcast message to all users in an itinerary
     */
    private void broadcastToItinerary(String itineraryId, ChatWebSocketMessage message) throws IOException {
        Map<Long, WebSocketSession> userSessions = sessions.get(itineraryId);

        if (userSessions != null) {
            String payload = objectMapper.writeValueAsString(message);
            List<WebSocketSession> disconnectedSessions = new ArrayList<>();

            for (WebSocketSession session : userSessions.values()) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(payload));
                    } catch (IOException e) {
                        log.warn("Failed to send message to session {}: {}",
                                session.getId(), e.getMessage());
                        disconnectedSessions.add(session);
                    }
                } else {
                    disconnectedSessions.add(session);
                }
            }

            // Clean up disconnected sessions
            disconnectedSessions.forEach(session -> {
                try {
                    afterConnectionClosed(session, CloseStatus.NORMAL);
                } catch (Exception e) {
                    log.error("Error closing session", e);
                }
            });
        }
    }

    /**
     * Broadcast message to all users in an itinerary except one
     */
    private void broadcastToItineraryExcept(String itineraryId, Long excludeUserId,
                                             ChatWebSocketMessage message) throws IOException {
        Map<Long, WebSocketSession> userSessions = sessions.get(itineraryId);

        if (userSessions != null) {
            String payload = objectMapper.writeValueAsString(message);

            for (Map.Entry<Long, WebSocketSession> entry : userSessions.entrySet()) {
                if (!entry.getKey().equals(excludeUserId) && entry.getValue().isOpen()) {
                    try {
                        entry.getValue().sendMessage(new TextMessage(payload));
                    } catch (IOException e) {
                        log.warn("Failed to send message to user {}: {}",
                                entry.getKey(), e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Send message to a specific user
     */
    private void broadcastToUser(String itineraryId, Long userId, ChatWebSocketMessage message) throws IOException {
        Map<Long, WebSocketSession> userSessions = sessions.get(itineraryId);

        if (userSessions != null) {
            WebSocketSession session = userSessions.get(userId);
            if (session != null && session.isOpen()) {
                String payload = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(payload));
            }
        }
    }

    /**
     * Send error message to a specific session
     */
    private void sendErrorMessage(WebSocketSession session, String errorMessage) {
        try {
            ChatWebSocketMessage message = ChatWebSocketMessage.builder()
                    .type("ERROR")
                    .error(errorMessage)
                    .timestamp(LocalDateTime.now())
                    .build();

            String payload = objectMapper.writeValueAsString(message);
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(payload));
            }
        } catch (IOException e) {
            log.error("Error sending error message", e);
        }
    }

    /**
     * Extract itinerary ID from WebSocket session
     */
    private String extractItineraryId(WebSocketSession session) {
        String uri = session.getUri().toString();
        // Extract from path: /api/chat/itineraries/{itineraryId}
        String[] parts = uri.split("/");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return null;
    }

    /**
     * Extract user ID from WebSocket session headers or query params
     * Note: This depends on how you pass the user ID in your frontend
     * You can modify this to extract from JWT token or query parameter
     */
    private Long extractUserId(WebSocketSession session) {
        // Option 1: Extract from query parameter
        String query = session.getUri().getQuery();
        if (query != null && query.contains("userId=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("userId=")) {
                    try {
                        return Long.parseLong(param.substring(7));
                    } catch (NumberFormatException e) {
                        log.warn("Invalid userId in query parameter");
                    }
                }
            }
        }

        // Option 2: Extract from headers
        List<String> authHeaders = session.getHandshakeHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            // Extract userId from JWT token (implement JWT parsing here)
            // For now, return null and rely on query parameter
        }

        return null;
    }

    /**
     * Build display name from user
     */
    private String buildDisplayName(User user) {
        StringBuilder displayName = new StringBuilder();

        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
            displayName.append(user.getFirstName());
        }

        if (user.getLastName() != null && !user.getLastName().isEmpty()) {
            if (displayName.length() > 0) {
                displayName.append(" ");
            }
            displayName.append(user.getLastName());
        }

        if (displayName.length() == 0 && user.getEmail() != null) {
            displayName.append(user.getEmail());
        }

        return displayName.toString();
    }

    /**
     * Inner class to store chat session information
     */
    @Getter
    @RequiredArgsConstructor
    private static class ChatSessionInfo {
        private final String itineraryId;
        private final Long userId;
    }
}
