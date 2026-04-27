package tn.esprit.spring.baladna.itinerary.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for a single recommendation
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {

    /**
     * ID of the recommended itinerary
     */
    private UUID itineraryId;

    /**
     * Title of the itinerary
     */
    private String title;

    /**
     * Description of the itinerary
     */
    private String description;

    /**
     * Destination region
     */
    private String destination;

    /**
     * Estimated budget
     */
    private BigDecimal budget;

    /**
     * Duration in days
     */
    private Integer durationDays;

    /**
     * Start date
     */
    private LocalDate startDate;

    /**
     * End date
     */
    private LocalDate endDate;

    /**
     * Number of steps/activities
     */
    private Integer numSteps;

    /**
     * Average daily cost
     */
    private BigDecimal avgDailyCost;

    /**
     * Similarity score (0.0 to 1.0) - higher means more similar to search criteria
     */
    private BigDecimal similarityScore;

    /**
     * Average rating of this itinerary (0.0 to 5.0)
     */
    private BigDecimal rating;

    /**
     * Number of collaborators on this itinerary
     */
    private Integer numCollaborators;

    /**
     * Reason why this was recommended
     */
    private String recommendationReason;
}
