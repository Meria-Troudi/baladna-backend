package tn.esprit.spring.baladna.transport.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.transport.dto.StationDTO;
import tn.esprit.spring.baladna.transport.entity.Station;
import tn.esprit.spring.baladna.transport.service.StationService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class StationController {

    private final StationService stationService;

    private StationDTO toDTO(Station station) {
        return StationDTO.builder()
                .id(station.getId())
                .name(station.getName())
                .city(station.getCity())
                .surcharge(station.getSurcharge())
                .downtown(station.getDowntown())
                .build();
    }

    private Station toEntity(StationDTO dto) {
        return Station.builder()
                .id(dto.getId())
                .name(dto.getName())
                .city(dto.getCity())
                .surcharge(dto.getSurcharge())
                .downtown(dto.getDowntown())
                .build();
    }

    @GetMapping
    public List<StationDTO> getAllStations() {
        return stationService.getAllStations().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StationDTO> getStationById(@PathVariable Long id) {
        Station station = stationService.getStationById(id);
        if (station == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDTO(station));
    }

    @GetMapping("/city/{city}")
    public List<StationDTO> getStationsByCity(@PathVariable String city) {
        return stationService.getStationsByCity(city).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/downtown")
    public List<StationDTO> getDowntownStations() {
        return stationService.getDowntownStations().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/search")
    public List<StationDTO> searchStationsByName(@RequestParam String name) {
        return stationService.searchByName(name).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<StationDTO> createStation(@Valid @RequestBody StationDTO dto) {
        Station created = stationService.createStation(toEntity(dto));
        return ResponseEntity.ok(toDTO(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StationDTO> updateStation(@PathVariable Long id, @Valid @RequestBody StationDTO dto) {
        Station updated = stationService.updateStation(id, toEntity(dto));
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
        return ResponseEntity.noContent().build();
    }
}