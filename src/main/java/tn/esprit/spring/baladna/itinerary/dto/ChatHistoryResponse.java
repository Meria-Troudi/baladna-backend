package tn.esprit.spring.baladna.itinerary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for paginated chat messages
 */
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatHistoryResponse {

    @JsonProperty("itineraryId")
    private UUID itineraryId;

    @JsonProperty("messages")
    private List<ChatMessageResponse> messages;

    @JsonProperty("totalMessages")
    private Long totalMessages;

    @JsonProperty("page")
    private Integer page;

    @JsonProperty("size")
    private Integer size;
}
