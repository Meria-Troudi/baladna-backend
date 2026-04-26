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
public class TransportAiDelayModelSummaryDTO {

    private Long modelId;
    private boolean trained;
    private Integer sampleCount;
    private Double meanAbsoluteError;
    private Double rootMeanSquaredError;
    private LocalDateTime trainedAt;
    private String predictionSource;
    private String notes;
}
