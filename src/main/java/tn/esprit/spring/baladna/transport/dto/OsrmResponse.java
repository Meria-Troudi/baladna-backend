package tn.esprit.spring.baladna.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OsrmResponse {
    private List<RouteData> routes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteData {
        private Double distance;
        private Double duration;
        private Map<String, Object> geometry;
    }
}
