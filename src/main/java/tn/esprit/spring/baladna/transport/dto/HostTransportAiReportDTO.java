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
public class HostTransportAiReportDTO {

    private LocalDateTime generatedAt;
    private Integer totalUpcomingTransports;
    private Integer activeBookings;
    private Integer atRiskTransports;
    private Integer lowOccupancyTransports;
    private Integer averageOccupancyRate;
    private Double estimatedRevenue;
    private String topRouteLabel;
    private Integer topRouteBookingCount;
    private List<String> recommendations;
}
