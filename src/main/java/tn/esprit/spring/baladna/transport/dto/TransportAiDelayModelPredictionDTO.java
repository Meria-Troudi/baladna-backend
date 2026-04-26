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
public class TransportAiDelayModelPredictionDTO {

    private Long modelId;
    private Integer predictionMinutes;
    private Integer sampleCount;
    private Double meanAbsoluteError;
    private LocalDateTime trainedAt;
}
