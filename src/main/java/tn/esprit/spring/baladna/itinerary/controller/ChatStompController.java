package tn.esprit.spring.baladna.itinerary.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import tn.esprit.spring.baladna.itinerary.dto.ChatMessageRequest;
import tn.esprit.spring.baladna.itinerary.dto.ChatMessageResponse;
import tn.esprit.spring.baladna.itinerary.dto.ChatWebSocketMessage;
import tn.esprit.spring.baladna.itinerary.service.ChatMessageService;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * STOMP controller for real-time chat messaging
 * 
 * Handles WebSocket STOMP messages for chat functionality
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatMessageService chatMessageService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handle incoming chat message
     * 
     * Frontend sends to: /app/chat/itineraries/{itineraryId}/send
     * Broadcast to: /topic/itineraries/{itineraryId}/messages
     */
    @MessageMapping("/chat/itineraries/{itineraryId}/send")
    @SendTo("/topic/itineraries/{itineraryId}/messages")
    public ChatWebSocketMessage sendMessage(
            @DestinationVariable String itineraryId,
            @Payload ChatMessageRequest request,
            StompHeaderAccessor headerAccessor) {
        try {
            Long senderId = extractUserIdFromSession(headerAccessor);
            String sessionItineraryId = extractItineraryIdFromSession(headerAccessor);
            
            if (senderId == null) {
                log.warn("User ID not found in WebSocket session");
                return buildErrorMessage("User ID not found");
            }

            // Verify itineraryId matches
            if (!itineraryId.equals(sessionItineraryId)) {
                log.warn("Itinerary ID mismatch - path: {}, session: {}", itineraryId, sessionItineraryId);
                return buildErrorMessage("Itinerary ID mismatch");
            }

            // Save message via service
            UUID itineraryUUID = UUID.fromString(itineraryId);
            ChatMessageResponse response = chatMessageService.sendMessage(
                    itineraryUUID, senderId, request);

            // Format response for WebSocket
            return ChatWebSocketMessage.builder()
                    .type("SEND")
                    .id(response.getId())
                    .itineraryId(response.getItineraryId())
                    .senderId(response.getSenderId())
                    .senderName(response.getSenderName())
                    .content(response.getContent())
                    .timestamp(response.getCreatedAt())
                    .status("SUCCESS")
                    .build();

        } catch (Exception e) {
            log.error("Error sending message for itinerary {}: {}", itineraryId, e.getMessage());
            return buildErrorMessage(e.getMessage());
        }
    }

    /**
     * Handle message edit
     * 
     * Frontend sends to: /app/chat/itineraries/{itineraryId}/edit
     * Broadcast to: /topic/itineraries/{itineraryId}/messages
     */
    @MessageMapping("/chat/itineraries/{itineraryId}/edit")
    @SendTo("/topic/itineraries/{itineraryId}/messages")
    public ChatWebSocketMessage editMessage(
            @DestinationVariable String itineraryId,
            @Payload ChatWebSocketMessage wsMessage,
            StompHeaderAccessor headerAccessor) {
        try {
            Long senderId = extractUserIdFromSession(headerAccessor);
            String sessionItineraryId = extractItineraryIdFromSession(headerAccessor);

            if (senderId == null) {
                return buildErrorMessage("User ID not found");
            }

            if (!itineraryId.equals(sessionItineraryId)) {
                return buildErrorMessage("Itinerary ID mismatch");
            }

            // Update message via service
            ChatMessageRequest request = new ChatMessageRequest();
            request.setContent(wsMessage.getContent());

            ChatMessageResponse response = chatMessageService.updateMessage(
                    wsMessage.getId(), senderId, request);

            return ChatWebSocketMessage.builder()
                    .type("EDIT")
                    .id(response.getId())
                    .itineraryId(response.getItineraryId())
                    .senderId(response.getSenderId())
                    .senderName(response.getSenderName())
                    .content(response.getContent())
                    .timestamp(response.getUpdatedAt())
                    .status("SUCCESS")
                    .build();

        } catch (Exception e) {
            log.error("Error editing message: {}", e.getMessage());
            return buildErrorMessage(e.getMessage());
        }
    }

    /**
     * Handle message delete
     * 
     * Frontend sends to: /app/chat/itineraries/{itineraryId}/delete
     * Broadcast to: /topic/itineraries/{itineraryId}/messages
     */
    @MessageMapping("/chat/itineraries/{itineraryId}/delete")
    @SendTo("/topic/itineraries/{itineraryId}/messages")
    public ChatWebSocketMessage deleteMessage(
            @DestinationVariable String itineraryId,
            @Payload ChatWebSocketMessage wsMessage,
            StompHeaderAccessor headerAccessor) {
        try {
            Long senderId = extractUserIdFromSession(headerAccessor);
            String sessionItineraryId = extractItineraryIdFromSession(headerAccessor);

            if (senderId == null) {
                return buildErrorMessage("User ID not found");
            }

            if (!itineraryId.equals(sessionItineraryId)) {
                return buildErrorMessage("Itinerary ID mismatch");
            }

            UUID itineraryUUID = UUID.fromString(itineraryId);
            chatMessageService.deleteMessage(wsMessage.getId(), senderId, itineraryUUID);

            return ChatWebSocketMessage.builder()
                    .type("DELETE")
                    .id(wsMessage.getId())
                    .itineraryId(UUID.fromString(itineraryId))
                    .senderId(senderId)
                    .timestamp(LocalDateTime.now())
                    .status("SUCCESS")
                    .build();

        } catch (Exception e) {
            log.error("Error deleting message: {}", e.getMessage());
            return buildErrorMessage(e.getMessage());
        }
    }

    /**
     * Handle typing indicator
     * 
     * Frontend sends to: /app/chat/itineraries/{itineraryId}/typing
     * Broadcast to: /topic/itineraries/{itineraryId}/typing (excluding sender)
     */
    @MessageMapping("/chat/itineraries/{itineraryId}/typing")
    public void sendTypingIndicator(
            @DestinationVariable String itineraryId,
            @Payload ChatWebSocketMessage wsMessage,
            StompHeaderAccessor headerAccessor) {
        try {
            Long senderId = extractUserIdFromSession(headerAccessor);
            String sessionItineraryId = extractItineraryIdFromSession(headerAccessor);

            if (senderId == null || !itineraryId.equals(sessionItineraryId)) {
                return;
            }

            User user = userRepository.findById(senderId).orElse(null);
            String userName = user != null ? buildDisplayName(user) : "Unknown User";

            ChatWebSocketMessage typingMessage = ChatWebSocketMessage.builder()
                    .type("TYPING")
                    .senderId(senderId)
                    .senderName(userName)
                    .timestamp(LocalDateTime.now())
                    .build();

            // Send to all except sender
            messagingTemplate.convertAndSend(
                    "/topic/itineraries/" + itineraryId + "/typing",
                    typingMessage);

        } catch (Exception e) {
            log.error("Error sending typing indicator: {}", e.getMessage());
        }
    }

    /**
     * Extract user ID from WebSocket session attributes
     */
    private Long extractUserIdFromSession(StompHeaderAccessor headerAccessor) {
        Object userIdObj = headerAccessor.getSessionAttributes().get("userId");
        if (userIdObj instanceof Long) {
            return (Long) userIdObj;
        }
        if (userIdObj instanceof String) {
            try {
                return Long.parseLong((String) userIdObj);
            } catch (NumberFormatException e) {
                log.warn("Invalid userId format in session: {}", userIdObj);
            }
        }
        return null;
    }

    /**
     * Extract itinerary ID from WebSocket session attributes
     */
    private String extractItineraryIdFromSession(StompHeaderAccessor headerAccessor) {
        Object itineraryIdObj = headerAccessor.getSessionAttributes().get("itineraryId");
        if (itineraryIdObj instanceof String) {
            return (String) itineraryIdObj;
        }
        return null;
    }

    /**
     * Build error message
     */
    private ChatWebSocketMessage buildErrorMessage(String errorMsg) {
        return ChatWebSocketMessage.builder()
                .type("ERROR")
                .status("FAILED")
                .error(errorMsg)
                .timestamp(LocalDateTime.now())
                .build();
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
}
