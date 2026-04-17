package tn.esprit.spring.baladna.transport.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OpenMeteoResponse {

    private HourlyData hourly;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyData {

        private List<String> time;

        @JsonProperty("weather_code")
        private List<Integer> weatherCode;

        @JsonProperty("temperature_2m")
        private List<Double> temperature2m;

        @JsonProperty("wind_speed_10m")
        private List<Double> windSpeed10m;

        private List<Double> precipitation;
    }
}