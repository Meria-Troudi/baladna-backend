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
public class TransportDelayPredictionDTO {

    private Long transportId;
    private Integer ruleBasedDelayMinutes;
    private Integer predictedDelayMinutes;
    private String riskLevel;
    private Integer confidencePercent;
    private String primaryReason;
    private LocalDateTime estimatedArrivalDate;
    private String predictionSource;
    private Long trainedModelId;
    private Integer trainedModelSampleCount;
    private Double trainedModelMeanAbsoluteError;
    private String explanation;
    private String recommendation;
    private List<String> factors;
}
