package tn.esprit.spring.baladna.itinerary.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.baladna.itinerary.dto.ChatHistoryResponse;
import tn.esprit.spring.baladna.itinerary.dto.ChatMessageRequest;
import tn.esprit.spring.baladna.itinerary.dto.ChatMessageResponse;
import tn.esprit.spring.baladna.itinerary.entity.ChatMessage;
import tn.esprit.spring.baladna.itinerary.entity.Itinerary;
import tn.esprit.spring.baladna.itinerary.repository.ChatMessageRepository;
import tn.esprit.spring.baladna.itinerary.repository.ItineraryCollaboratorRepository;
import tn.esprit.spring.baladna.itinerary.repository.ItineraryRepository;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing chat messages between itinerary collaborators
 */
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ItineraryRepository itineraryRepository;
    private final UserRepository userRepository;
    private final ItineraryCollaboratorRepository collaboratorRepository;

    /**
     * Send a new message in an itinerary's chat
     *
     * @param itineraryId the itinerary ID
     * @param senderId    the sender's user ID
     * @param request     the message request
     * @return the created message response
     */
    @Transactional
    public ChatMessageResponse sendMessage(UUID itineraryId, Long senderId, ChatMessageRequest request) {
        // Verify itinerary exists
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("Itinerary not found: " + itineraryId));

        // Verify sender is a collaborator (or owner)
        boolean isCollaborator = collaboratorRepository.findByItineraryIdAndUserId(itineraryId, senderId).isPresent();
        if (!isCollaborator && !itinerary.getOwnerId().equals(senderId)) {
            throw new IllegalArgumentException("User is not a collaborator in this itinerary");
        }

        // Create and save message
        ChatMessage message = ChatMessage.builder()
                .itinerary(itinerary)
                .senderId(senderId)
                .content(request.getContent())
                .isDeleted(false)
                .build();

        ChatMessage savedMessage = chatMessageRepository.save(message);
        return mapToResponse(savedMessage);
    }

    /**
     * Get chat history for an itinerary (paginated)
     *
     * @param itineraryId the itinerary ID
     * @param page        page number (0-indexed)
     * @param size        page size
     * @return paginated chat history
     */
    @Transactional(readOnly = true)
    public ChatHistoryResponse getChatHistory(UUID itineraryId, Integer page, Integer size) {
        // Verify itinerary exists
        itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("Itinerary not found: " + itineraryId));

        Pageable pageable = PageRequest.of(Math.max(0, page != null ? page : 0), Math.max(1, size != null ? size : 20));
        Page<ChatMessage> messagePage = chatMessageRepository.findByItineraryIdAndNotDeleted(itineraryId, pageable);

        List<ChatMessageResponse> messages = messagePage.getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ChatHistoryResponse.builder()
                .itineraryId(itineraryId)
                .messages(messages)
                .totalMessages(messagePage.getTotalElements())
                .page(page != null ? page : 0)
                .size(size != null ? size : 20)
                .build();
    }

    /**
     * Get a specific message
     *
     * @param messageId the message ID
     * @return the message response
     */
    @Transactional(readOnly = true)
    public ChatMessageResponse getMessageById(UUID messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        if (message.getIsDeleted()) {
            throw new IllegalArgumentException("Message has been deleted");
        }

        return mapToResponse(message);
    }

    /**
     * Update a message (only the sender can update)
     *
     * @param messageId   the message ID
     * @param senderId    the current user ID
     * @param request     the updated message request
     * @return the updated message response
     */
    @Transactional
    public ChatMessageResponse updateMessage(UUID messageId, Long senderId, ChatMessageRequest request) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        if (message.getIsDeleted()) {
            throw new IllegalArgumentException("Cannot update a deleted message");
        }

        // Only the sender can update the message
        if (!message.getSenderId().equals(senderId)) {
            throw new IllegalArgumentException("Only the sender can update this message");
        }

        message.setContent(request.getContent());
        ChatMessage updatedMessage = chatMessageRepository.save(message);
        return mapToResponse(updatedMessage);
    }

    /**
     * Delete a message (soft delete - only the sender or itinerary owner can delete)
     *
     * @param messageId   the message ID
     * @param senderId    the current user ID
     * @param itineraryId the itinerary ID
     */
    @Transactional
    public void deleteMessage(UUID messageId, Long senderId, UUID itineraryId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found: " + messageId));

        // Verify itinerary exists
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new IllegalArgumentException("Itinerary not found: " + itineraryId));

        // Only the sender or itinerary owner can delete
        if (!message.getSenderId().equals(senderId) && !itinerary.getOwnerId().equals(senderId)) {
            throw new IllegalArgumentException("Only the sender or itinerary owner can delete this message");
        }

        message.setIsDeleted(true);
        chatMessageRepository.save(message);
    }

    /**
     * Get latest message in an itinerary (for new chat indicators)
     *
     * @param itineraryId the itinerary ID
     * @return the latest message response or null
     */
    @Transactional(readOnly = true)
    public ChatMessageResponse getLatestMessage(UUID itineraryId) {
        ChatMessage message = chatMessageRepository.findLatestMessageByItineraryId(itineraryId);
        return message != null ? mapToResponse(message) : null;
    }

    /**
     * Get total count of messages in an itinerary
     *
     * @param itineraryId the itinerary ID
     * @return count of messages
     */
    @Transactional(readOnly = true)
    public Long getMessageCount(UUID itineraryId) {
        return chatMessageRepository.countByItineraryIdAndNotDeleted(itineraryId);
    }

    /**
     * Map ChatMessage entity to ChatMessageResponse DTO
     *
     * @param message the chat message entity
     * @return the response DTO
     */
    private ChatMessageResponse mapToResponse(ChatMessage message) {
        // Get sender information
        User sender = userRepository.findById(message.getSenderId()).orElse(null);
        String senderName = sender != null ? buildDisplayName(sender) : "Unknown User";

        return ChatMessageResponse.builder()
                .id(message.getId())
                .itineraryId(message.getItinerary().getId())
                .senderId(message.getSenderId())
                .senderName(senderName)
                .content(message.getContent())
                .isDeleted(message.getIsDeleted())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .build();
    }

    /**
     * Build a display name from user first and last names
     *
     * @param user the user
     * @return the display name
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
