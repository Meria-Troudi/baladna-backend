package tn.esprit.spring.baladna.transport.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.spring.baladna.transport.dto.OsrmResponse;
import tn.esprit.spring.baladna.transport.dto.RouteInfo;
import tn.esprit.spring.baladna.transport.entity.Station;

@Service
public class RoutingService {

    private static final String OSRM_URL_TEMPLATE =
            "https://router.project-osrm.org/route/v1/driving/%s,%s;%s,%s?overview=full&geometries=geojson";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RoutingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public RouteInfo getRouteInfo(Station departureStation, Station arrivalStation) {
        if (!hasCoordinates(departureStation) || !hasCoordinates(arrivalStation)) {
            return null;
        }

        String url = OSRM_URL_TEMPLATE.formatted(
                departureStation.getLongitude(),
                departureStation.getLatitude(),
                arrivalStation.getLongitude(),
                arrivalStation.getLatitude()
        );

        try {
            OsrmResponse response = restTemplate.getForObject(url, OsrmResponse.class);
            if (response == null || response.getRoutes() == null || response.getRoutes().isEmpty()) {
                return null;
            }

            OsrmResponse.RouteData route = response.getRoutes().get(0);

            return RouteInfo.builder()
                    .distanceKm(roundDistanceKm(route.getDistance()))
                    .durationMinutes(roundDurationMinutes(route.getDuration()))
                    .routeGeoJson(writeGeometry(route.getGeometry()))
                    .build();
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean hasCoordinates(Station station) {
        return station != null
                && station.getLatitude() != null
                && station.getLongitude() != null
                && station.getLatitude() >= -90
                && station.getLatitude() <= 90
                && station.getLongitude() >= -180
                && station.getLongitude() <= 180;
    }

    private Double roundDistanceKm(Double distanceMeters) {
        if (distanceMeters == null) {
            return null;
        }
        return Math.round((distanceMeters / 1000.0) * 100.0) / 100.0;
    }

    private Integer roundDurationMinutes(Double durationSeconds) {
        if (durationSeconds == null) {
            return null;
        }
        return Math.max(1, (int) Math.round(durationSeconds / 60.0));
    }

    private String writeGeometry(Object geometry) {
        if (geometry == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(geometry);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }
}
