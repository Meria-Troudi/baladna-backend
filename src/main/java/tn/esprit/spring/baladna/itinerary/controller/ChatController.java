package tn.esprit.spring.baladna.itinerary.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.itinerary.dto.ChatHistoryResponse;
import tn.esprit.spring.baladna.itinerary.dto.ChatMessageRequest;
import tn.esprit.spring.baladna.itinerary.dto.ChatMessageResponse;
import tn.esprit.spring.baladna.itinerary.service.ChatMessageService;
import tn.esprit.spring.baladna.itinerary.util.SecurityUtils;

import java.util.UUID;

/**
 * REST endpoints for chat functionality between itinerary collaborators.
 * All endpoints require a valid JWT in the Authorization header:
 *   Authorization: Bearer <token>
 *
 * The currently logged-in user's ID is extracted automatically
 * from the security context via SecurityUtils.
 */
@RestController
@RequestMapping("/api/itineraries/{itineraryId}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessageService chatMessageService;
    private final SecurityUtils securityUtils;

    /**
     * Send a new message in the itinerary's chat
     *
     * POST /api/itineraries/{itineraryId}/chat/messages
     *
     * @param itineraryId the itinerary ID
     * @param request     the message content
     * @return the created message
     */
    @PostMapping("/messages")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @PathVariable UUID itineraryId,
            @Valid @RequestBody ChatMessageRequest request) {
        Long senderId = securityUtils.getCurrentUserId();
        ChatMessageResponse response = chatMessageService.sendMessage(itineraryId, senderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get chat history for an itinerary (paginated)
     *
     * GET /api/itineraries/{itineraryId}/chat/history?page=0&size=20
     *
     * @param itineraryId the itinerary ID
     * @param page        page number (0-indexed, default 0)
     * @param size        page size (default 20)
     * @return paginated chat history
     */
    @GetMapping("/history")
    public ResponseEntity<ChatHistoryResponse> getChatHistory(
            @PathVariable UUID itineraryId,
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size) {
        Long userId = securityUtils.getCurrentUserId();
        ChatHistoryResponse response = chatMessageService.getChatHistory(itineraryId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get a specific message by ID
     *
     * GET /api/itineraries/{itineraryId}/chat/messages/{messageId}
     *
     * @param itineraryId the itinerary ID
     * @param messageId   the message ID
     * @return the message details
     */
    @GetMapping("/messages/{messageId}")
    public ResponseEntity<ChatMessageResponse> getMessage(
            @PathVariable UUID itineraryId,
            @PathVariable UUID messageId) {
        Long userId = securityUtils.getCurrentUserId();
        ChatMessageResponse response = chatMessageService.getMessageById(messageId);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a message (only sender can update)
     *
     * PUT /api/itineraries/{itineraryId}/chat/messages/{messageId}
     *
     * @param itineraryId the itinerary ID
     * @param messageId   the message ID
     * @param request     the updated message content
     * @return the updated message
     */
    @PutMapping("/messages/{messageId}")
    public ResponseEntity<ChatMessageResponse> updateMessage(
            @PathVariable UUID itineraryId,
            @PathVariable UUID messageId,
            @Valid @RequestBody ChatMessageRequest request) {
        Long senderId = securityUtils.getCurrentUserId();
        ChatMessageResponse response = chatMessageService.updateMessage(messageId, senderId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a message (soft delete - only sender or itinerary owner can delete)
     *
     * DELETE /api/itineraries/{itineraryId}/chat/messages/{messageId}
     *
     * @param itineraryId the itinerary ID
     * @param messageId   the message ID
     * @return no content
     */
    @DeleteMapping("/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable UUID itineraryId,
            @PathVariable UUID messageId) {
        Long senderId = securityUtils.getCurrentUserId();
        chatMessageService.deleteMessage(messageId, senderId, itineraryId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get the latest message in an itinerary (useful for new chat indicators)
     *
     * GET /api/itineraries/{itineraryId}/chat/latest
     *
     * @param itineraryId the itinerary ID
     * @return the latest message or null if no messages
     */
    @GetMapping("/latest")
    public ResponseEntity<ChatMessageResponse> getLatestMessage(
            @PathVariable UUID itineraryId) {
        Long userId = securityUtils.getCurrentUserId();
        ChatMessageResponse response = chatMessageService.getLatestMessage(itineraryId);
        if (response != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    /**
     * Get the total count of messages in an itinerary
     *
     * GET /api/itineraries/{itineraryId}/chat/count
     *
     * @param itineraryId the itinerary ID
     * @return message count
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getMessageCount(
            @PathVariable UUID itineraryId) {
        Long userId = securityUtils.getCurrentUserId();
        Long count = chatMessageService.getMessageCount(itineraryId);
        return ResponseEntity.ok(count);
    }
}
