package tn.esprit.spring.baladna.itinerary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a chat message
 */
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("itineraryId")
    private UUID itineraryId;

    @JsonProperty("senderId")
    private Long senderId;

    @JsonProperty("senderName")
    private String senderName; // User's display name or email

    @JsonProperty("content")
    private String content;

    @JsonProperty("isDeleted")
    private Boolean isDeleted;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
}
