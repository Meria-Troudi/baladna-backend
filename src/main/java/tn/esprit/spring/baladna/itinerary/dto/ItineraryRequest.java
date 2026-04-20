package tn.esprit.spring.baladna.itinerary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tn.esprit.spring.baladna.itinerary.entity.enums.ItineraryStatus;
import tn.esprit.spring.baladna.itinerary.entity.enums.Visibility;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ItineraryRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private String destinationRegion;

    private BigDecimal estimatedBudget;

    private ItineraryStatus status = ItineraryStatus.DRAFT;

    private Visibility visibility = Visibility.PRIVATE;
}