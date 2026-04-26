package tn.esprit.spring.baladna.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportAiOptionDTO {

    private Long transportId;
    private String routeLabel;
    private String departurePoint;
    private String departureCity;
    private String arrivalCity;
    private LocalDateTime departureDate;
    private Integer availableSeats;
    private Integer totalCapacity;
    private Double basePrice;
    private String weather;
    private Integer predictedDelayMinutes;
    private Integer aiScore;
    private List<String> reasons;
    private List<String> warnings;
}
