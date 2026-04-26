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
public class WeatherInfo {
    private Integer weatherCode;
    private WeatherCondition condition;
    private Double temperature;
    private Double windSpeed;
    private Double precipitation;
}