package tn.esprit.spring.baladna.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.spring.baladna.transport.entity.TrafficCongestionLevel;
import tn.esprit.spring.baladna.transport.entity.WeatherCondition;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherPreviewDTO {
    private WeatherCondition weather;
    private String weatherSource;
    private String routingSource;
    private Double weatherTemperature;
    private Double weatherWindSpeed;
    private Double weatherPrecipitation;
    private Double routeDistanceKm;
    private Integer estimatedDurationMinutes;
    private TrafficCongestionLevel trafficCongestionLevel;
    private Integer weatherDelayMinutes;
    private Integer trafficDelayMinutes;
    private Integer delayMinutes;
    private Integer confidencePercent;
    private String primaryReason;
}
