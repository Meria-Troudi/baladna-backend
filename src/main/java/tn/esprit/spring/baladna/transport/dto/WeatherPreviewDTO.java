package tn.esprit.spring.baladna.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.spring.baladna.transport.entity.WeatherCondition;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherPreviewDTO {
    private WeatherCondition weather;
    private String weatherSource;
    private Double weatherTemperature;
    private Double weatherWindSpeed;
    private Double weatherPrecipitation;
    private Integer delayMinutes;
}