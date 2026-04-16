package tn.esprit.spring.baladna.transport.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.transport.dto.RouteInfo;
import tn.esprit.spring.baladna.transport.dto.RoutePreviewDTO;
import tn.esprit.spring.baladna.transport.dto.TrajetDTO;
import tn.esprit.spring.baladna.transport.entity.Station;
import tn.esprit.spring.baladna.transport.entity.Trajet;
import tn.esprit.spring.baladna.transport.service.StationService;
import tn.esprit.spring.baladna.transport.service.TrajetService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/trajets")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TrajetController {

    private final TrajetService trajetService;
    private final StationService stationService;

    private RoutePreviewDTO toRoutePreviewDTO(RouteInfo routeInfo) {
        if (routeInfo == null) {
            return null;
        }

        return RoutePreviewDTO.builder()
                .distanceKm(routeInfo.getDistanceKm())
                .estimatedDurationMinutes(routeInfo.getDurationMinutes())
                .routeGeoJson(routeInfo.getRouteGeoJson())
                .build();
    }

    private TrajetDTO toDTO(Trajet trajet) {
        return TrajetDTO.builder()
                .id(trajet.getId())
                .departureStationId(trajet.getDepartureStation().getId())
                .departureStationName(trajet.getDepartureStation().getName())
                .arrivalStationId(trajet.getArrivalStation().getId())
                .arrivalStationName(trajet.getArrivalStation().getName())
                .distanceKm(trajet.getDistanceKm())
                .estimatedDurationMinutes(trajet.getEstimatedDurationMinutes())
                .pricePerKm(trajet.getPricePerKm())
                .basePrice(trajet.getBasePrice())
                .routeGeoJson(trajet.getRouteGeoJson())
                .build();
    }

    private Trajet toEntity(TrajetDTO dto) {
        Station departure = stationService.getStationById(dto.getDepartureStationId());
        Station arrival = stationService.getStationById(dto.getArrivalStationId());

        if (departure == null || arrival == null) {
            throw new RuntimeException("Stations invalides");
        }

        return Trajet.builder()
                .id(dto.getId())
                .departureStation(departure)
                .arrivalStation(arrival)
                .distanceKm(dto.getDistanceKm())
                .estimatedDurationMinutes(dto.getEstimatedDurationMinutes())
                .pricePerKm(dto.getPricePerKm())
                .routeGeoJson(dto.getRouteGeoJson())
                .build();
    }

    @GetMapping
    public List<TrajetDTO> getAllTrajets(Authentication authentication) {
        List<Trajet> trajets = isHost(authentication)
                ? trajetService.getTrajetsForHost(authentication.getName())
                : trajetService.getAllTrajets();

        return trajets.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrajetDTO> getTrajetById(@PathVariable Long id, Authentication authentication) {
        Trajet trajet = isHost(authentication)
                ? trajetService.getTrajetByIdForHost(id, authentication.getName())
                : trajetService.getTrajetById(id);
        if (trajet == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(trajet));
    }

    @GetMapping("/departure/{stationId}")
    public List<TrajetDTO> getTrajetsByDeparture(@PathVariable Long stationId, Authentication authentication) {
        List<Trajet> trajets = isHost(authentication)
                ? trajetService.getTrajetsByDepartureStationForHost(stationId, authentication.getName())
                : trajetService.getTrajetsByDepartureStation(stationId);

        return trajets.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/arrival/{stationId}")
    public List<TrajetDTO> getTrajetsByArrival(@PathVariable Long stationId, Authentication authentication) {
        List<Trajet> trajets = isHost(authentication)
                ? trajetService.getTrajetsByArrivalStationForHost(stationId, authentication.getName())
                : trajetService.getTrajetsByArrivalStation(stationId);

        return trajets.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/between")
    public List<TrajetDTO> getTrajetsBetweenStations(@RequestParam Long departureId,
                                                     @RequestParam Long arrivalId,
                                                     Authentication authentication) {
        List<Trajet> trajets = isHost(authentication)
                ? trajetService.getTrajetsBetweenStationsForHost(departureId, arrivalId, authentication.getName())
                : trajetService.getTrajetsBetweenStations(departureId, arrivalId);

        return trajets.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/preview")
    public ResponseEntity<RoutePreviewDTO> previewRoute(@RequestParam Long departureStationId,
                                                        @RequestParam Long arrivalStationId) {
        RouteInfo routeInfo = trajetService.previewRoute(departureStationId, arrivalStationId);
        if (routeInfo == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(toRoutePreviewDTO(routeInfo));
    }

    @PostMapping
    public ResponseEntity<TrajetDTO> createTrajet(@Valid @RequestBody TrajetDTO dto, Authentication authentication) {
        Trajet created = trajetService.createTrajet(toEntity(dto), authentication.getName());
        return ResponseEntity.ok(toDTO(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrajetDTO> updateTrajet(@PathVariable Long id, @Valid @RequestBody TrajetDTO dto, Authentication authentication) {
        Trajet updated = trajetService.updateTrajet(id, toEntity(dto), authentication.getName());
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrajet(@PathVariable Long id, Authentication authentication) {
        trajetService.deleteTrajet(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    private boolean isHost(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_HOST".equals(authority.getAuthority()));
    }
}
