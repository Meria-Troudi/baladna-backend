package tn.esprit.spring.baladna.transport.dto;

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
        private List<Integer> weather_code;
    }
}
