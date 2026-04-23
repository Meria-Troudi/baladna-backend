package tn.esprit.spring.baladna.itinerary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tn.esprit.spring.baladna.itinerary.entity.enums.ServiceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ItineraryStepRequest {

    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @NotBlank(message = "Service reference ID is required")
    private String serviceRefId;

    @NotBlank(message = "Title is required")
    private String title;

    private String notes;

    private LocalDateTime plannedDate;

    private Integer position;

    private BigDecimal estimatedCost;

    private BigDecimal actualCost;

    private BigDecimal latitude;

    private BigDecimal longitude;
}