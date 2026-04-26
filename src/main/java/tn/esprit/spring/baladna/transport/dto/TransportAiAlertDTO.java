package tn.esprit.spring.baladna.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportAiAlertDTO {

    private Long transportId;
    private String severity;
    private String title;
    private String message;
    private String suggestedAction;
    private String routeLabel;
    private LocalDateTime departureDate;
    private Integer predictedDelayMinutes;
}
