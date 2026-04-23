package tn.esprit.spring.baladna.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for checking if a user is eligible to review an event.
 * A user can review IF:
 * - They have at least one reservation for the event (any status)
 * - They haven't already submitted a review
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EligibilityResponse {
    
    /**
     * Whether the user is eligible to submit a review
     */
    private boolean canReview;
    
    /**
     * The reservation ID (required to submit a review)
     */
    private Long reservationId;
    
    /**
     * Whether the user has already reviewed this event
     */
    private boolean alreadyReviewed;
    /**
     * Sets the current user ID for the response.
     */
private Long userId;

public void setUserId(Long userId) {
this.userId = userId;
    }

public Long getUserId() {
return userId;
    }
}
