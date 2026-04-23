package tn.esprit.spring.baladna.itinerary.dto;

import lombok.Builder;
import lombok.Data;
import tn.esprit.spring.baladna.itinerary.entity.enums.ItineraryStatus;
import tn.esprit.spring.baladna.itinerary.entity.enums.Visibility;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ItineraryResponse {

    private UUID id;
    private Long ownerId;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private String destinationRegion;
    private BigDecimal estimatedBudget;
    private BigDecimal actualBudget;
    private ItineraryStatus status;
    private Visibility visibility;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ItineraryStepResponse> steps;
    private List<CollaboratorResponse> collaborators;
}