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
public class TransportAiDatasetSummaryDTO {

    private String hostEmail;
    private Long tripRecords;
    private Long trainingReadyTripRecords;
    private Long realTripRecords;
    private Long bootstrapTripRecords;
    private Long realTrainingReadyTripRecords;
    private Long bootstrapTrainingReadyTripRecords;
    private Long incidentRecords;
    private Long hostMetricRecords;
    private Long recommendationRecords;
    private LocalDateTime generatedAt;
}
