package tn.esprit.spring.baladna.itinerary.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Response DTO for chat service info and current user details
 */
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatInfoResponse {

    @JsonProperty("status")
    private String status; // "active", "maintenance", etc.

    @JsonProperty("userId")
    private Long userId; // Current authenticated user ID

    @JsonProperty("userName")
    private String userName; // Current user's name

    @JsonProperty("available")
    private boolean available; // Whether chat service is available

    @JsonProperty("message")
    private String message; // Status message
}
