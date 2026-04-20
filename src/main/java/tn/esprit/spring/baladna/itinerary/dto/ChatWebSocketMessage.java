package tn.esprit.spring.baladna.itinerary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WebSocket message DTO for real-time chat
 */
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatWebSocketMessage {

    /**
     * Type of message: "SEND", "EDIT", "DELETE", "USER_JOINED", "USER_LEFT", "TYPING"
     */
    @JsonProperty("type")
    private String type;

    /**
     * The actual message ID (for EDIT and DELETE operations)
     */
    @JsonProperty("id")
    private UUID id;

    /**
     * The itinerary ID
     */
    @JsonProperty("itineraryId")
    private UUID itineraryId;

    /**
     * The sender's user ID
     */
    @JsonProperty("senderId")
    private Long senderId;

    /**
     * The sender's name
     */
    @JsonProperty("senderName")
    private String senderName;

    /**
     * Message content
     */
    @JsonProperty("content")
    private String content;

    /**
     * Timestamp
     */
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    /**
     * Error message (if any)
     */
    @JsonProperty("error")
    private String error;

    /**
     * Status of the message
     */
    @JsonProperty("status")
    private String status; // "SUCCESS", "FAILED", etc.
}
