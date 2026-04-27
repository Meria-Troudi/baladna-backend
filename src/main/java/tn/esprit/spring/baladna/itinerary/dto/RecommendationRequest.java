package tn.esprit.spring.baladna.itinerary.dto;

import lombok.*;
import java.math.BigDecimal;

/**
 * Request DTO for itinerary recommendations
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationRequest {

    /**
     * Maximum budget for recommendations
     */
    private BigDecimal maxBudget;

    /**
     * Preferred location/region
     */
    private String location;

    /**
     * Number of recommendations to return
     */
    @Builder.Default
    private Integer limit = 5;

    /**
     * Minimum duration in days (optional)
     */
    private Integer minDuration;

    /**
     * Maximum duration in days (optional)
     */
    private Integer maxDuration;

    /**
     * k value for k-NN algorithm (optional)
     */
    @Builder.Default
    private Integer kNeighbors = 5;

    /**
     * Minimum acceptable rating for recommendations
     */
    private BigDecimal minRating;

    /**
     * If true, only exact location matches. If false, include similar locations
     */
    @Builder.Default
    private Boolean exactLocationMatch = false;
}
