package tn.esprit.spring.baladna.transport.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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
                .build();
    }

    @GetMapping
    public List<TrajetDTO> getAllTrajets() {
        return trajetService.getAllTrajets().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TrajetDTO> getTrajetById(@PathVariable Long id) {
        Trajet trajet = trajetService.getTrajetById(id);
        if (trajet == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(trajet));
    }

    @GetMapping("/departure/{stationId}")
    public List<TrajetDTO> getTrajetsByDeparture(@PathVariable Long stationId) {
        return trajetService.getTrajetsByDepartureStation(stationId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/arrival/{stationId}")
    public List<TrajetDTO> getTrajetsByArrival(@PathVariable Long stationId) {
        return trajetService.getTrajetsByArrivalStation(stationId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/between")
    public List<TrajetDTO> getTrajetsBetweenStations(@RequestParam Long departureId,
                                                     @RequestParam Long arrivalId) {
        return trajetService.getTrajetsBetweenStations(departureId, arrivalId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<TrajetDTO> createTrajet(@Valid @RequestBody TrajetDTO dto) {
        Trajet created = trajetService.createTrajet(toEntity(dto));
        return ResponseEntity.ok(toDTO(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TrajetDTO> updateTrajet(@PathVariable Long id, @Valid @RequestBody TrajetDTO dto) {
        Trajet updated = trajetService.updateTrajet(id, toEntity(dto));
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTrajet(@PathVariable Long id) {
        trajetService.deleteTrajet(id);
        return ResponseEntity.noContent().build();
    }
}