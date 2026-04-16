package tn.esprit.spring.baladna.transport.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ActiveProfiles;
import tn.esprit.spring.baladna.GestionUserApplication;
import tn.esprit.spring.baladna.transport.dto.RouteInfo;
import tn.esprit.spring.baladna.transport.entity.Station;
import tn.esprit.spring.baladna.transport.entity.Trajet;
import tn.esprit.spring.baladna.transport.repository.StationRepository;
import tn.esprit.spring.baladna.transport.repository.TrajetRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
        GestionUserApplication.class,
        TrajetStationSynchronizationIntegrationTest.TestRoutingConfiguration.class
})
@ActiveProfiles("test")
class TrajetStationSynchronizationIntegrationTest {

    @Autowired
    private TrajetService trajetService;

    @Autowired
    private StationService stationService;

    @Autowired
    private StationRepository stationRepository;

    @Autowired
    private TrajetRepository trajetRepository;

    @Autowired
    private RoutingService routingService;

    @BeforeEach
    void cleanDatabase() {
        trajetRepository.deleteAll();
        stationRepository.deleteAll();
    }

    @Test
    void createTrajetShouldPersistRouteDataFromRoutingService() {
        Station departure = stationRepository.save(buildStation("Tunis Gare", "Tunis", 36.8, 10.18));
        Station arrival = stationRepository.save(buildStation("Sousse Centre", "Sousse", 35.82, 10.63));

        when(routingService.getRouteInfo(any(Station.class), any(Station.class)))
                .thenReturn(RouteInfo.builder()
                        .distanceKm(142.75)
                        .durationMinutes(105)
                        .routeGeoJson("{\"type\":\"LineString\",\"coordinates\":[[10.18,36.8],[10.63,35.82]]}")
                        .build());

        Trajet savedTrajet = trajetService.createTrajet(Trajet.builder()
                .departureStation(departure)
                .arrivalStation(arrival)
                .distanceKm(1.0)
                .estimatedDurationMinutes(1)
                .pricePerKm(0.8)
                .build());

        assertNotNull(savedTrajet.getId());
        assertEquals(142.75, savedTrajet.getDistanceKm());
        assertEquals(105, savedTrajet.getEstimatedDurationMinutes());
        assertEquals("{\"type\":\"LineString\",\"coordinates\":[[10.18,36.8],[10.63,35.82]]}", savedTrajet.getRouteGeoJson());
    }

    @Test
    void updateStationShouldResynchronizeLinkedTrajets() {
        Station departure = stationRepository.save(buildStation("Tunis Gare", "Tunis", 36.8, 10.18));
        Station arrival = stationRepository.save(buildStation("Sousse Centre", "Sousse", 35.82, 10.63));

        when(routingService.getRouteInfo(any(Station.class), any(Station.class)))
                .thenReturn(RouteInfo.builder()
                        .distanceKm(140.0)
                        .durationMinutes(100)
                        .routeGeoJson("{\"type\":\"LineString\",\"coordinates\":[[10.18,36.8],[10.63,35.82]]}")
                        .build());

        Trajet trajet = trajetService.createTrajet(Trajet.builder()
                .departureStation(departure)
                .arrivalStation(arrival)
                .distanceKm(1.0)
                .estimatedDurationMinutes(1)
                .pricePerKm(0.8)
                .build());

        when(routingService.getRouteInfo(any(Station.class), any(Station.class)))
                .thenReturn(RouteInfo.builder()
                        .distanceKm(155.4)
                        .durationMinutes(118)
                        .routeGeoJson("{\"type\":\"LineString\",\"coordinates\":[[10.25,36.81],[10.63,35.82]]}")
                        .build());

        Station updatedDeparture = buildStation("Tunis Gare", "Tunis", 36.81, 10.25);
        updatedDeparture.setSurcharge(departure.getSurcharge());
        updatedDeparture.setDowntown(departure.getDowntown());

        stationService.updateStation(departure.getId(), updatedDeparture);

        Trajet refreshedTrajet = trajetRepository.findById(trajet.getId()).orElseThrow();
        assertEquals(155.4, refreshedTrajet.getDistanceKm());
        assertEquals(118, refreshedTrajet.getEstimatedDurationMinutes());
        assertEquals("{\"type\":\"LineString\",\"coordinates\":[[10.25,36.81],[10.63,35.82]]}", refreshedTrajet.getRouteGeoJson());
    }

    private Station buildStation(String name, String city, double latitude, double longitude) {
        return Station.builder()
                .name(name)
                .city(city)
                .surcharge(2.0)
                .downtown(Boolean.TRUE)
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }

    @TestConfiguration
    static class TestRoutingConfiguration {
        @Bean
        @Primary
        RoutingService routingService() {
            return mock(RoutingService.class);
        }
    }
}
