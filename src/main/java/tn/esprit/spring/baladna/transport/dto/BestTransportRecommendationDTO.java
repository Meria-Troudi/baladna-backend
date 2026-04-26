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
public class BestTransportRecommendationDTO {

    private LocalDateTime generatedAt;
    private String departureQuery;
    private String arrivalQuery;
    private Integer analyzedCount;
    private String summary;
    private Long recommendationFeedbackId;
    private TransportAiOptionDTO recommended;
    private List<TransportAiOptionDTO> alternatives;
}
