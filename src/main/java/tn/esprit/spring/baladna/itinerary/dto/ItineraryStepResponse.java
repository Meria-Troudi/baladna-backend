package tn.esprit.spring.baladna.itinerary.dto;

import lombok.Builder;
import lombok.Data;
import tn.esprit.spring.baladna.itinerary.entity.enums.ServiceType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ItineraryStepResponse {

    private UUID id;
    private Long addedByUserId;
    private ServiceType serviceType;
    private String serviceRefId;
    private String title;
    private String notes;
    private LocalDateTime plannedDate;
    private Integer position;
    private BigDecimal estimatedCost;
    private BigDecimal actualCost;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}