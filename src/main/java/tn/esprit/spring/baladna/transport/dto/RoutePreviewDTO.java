package tn.esprit.spring.baladna.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutePreviewDTO {
    private Double distanceKm;
    private Integer estimatedDurationMinutes;
    private String routeGeoJson;
}
