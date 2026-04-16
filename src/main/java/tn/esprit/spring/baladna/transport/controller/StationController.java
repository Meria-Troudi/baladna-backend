package tn.esprit.spring.baladna.transport.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.transport.dto.GeoLocationDTO;
import tn.esprit.spring.baladna.transport.dto.StationDTO;
import tn.esprit.spring.baladna.transport.entity.Station;
import tn.esprit.spring.baladna.transport.service.GeocodingService;
import tn.esprit.spring.baladna.transport.service.StationService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stations")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class StationController {

    private final StationService stationService;
    private final GeocodingService geocodingService;

    private StationDTO toDTO(Station station) {
        return StationDTO.builder()
                .id(station.getId())
                .name(station.getName())
                .city(station.getCity())
                .surcharge(station.getSurcharge())
                .downtown(station.getDowntown())
                .latitude(station.getLatitude())
                .longitude(station.getLongitude())
                .build();
    }

    private Station toEntity(StationDTO dto) {
        return Station.builder()
                .id(dto.getId())
                .name(dto.getName())
                .city(dto.getCity())
                .surcharge(dto.getSurcharge())
                .downtown(dto.getDowntown())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .build();
    }

    @GetMapping
    public List<StationDTO> getAllStations(Authentication authentication) {
        List<Station> stations = isHost(authentication)
                ? stationService.getStationsForHost(authentication.getName())
                : stationService.getAllStations();

        return stations.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StationDTO> getStationById(@PathVariable Long id, Authentication authentication) {
        Station station = isHost(authentication)
                ? stationService.getStationByIdForHost(id, authentication.getName())
                : stationService.getStationById(id);

        if (station == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDTO(station));
    }

    @GetMapping("/city/{city}")
    public List<StationDTO> getStationsByCity(@PathVariable String city, Authentication authentication) {
        List<Station> stations = isHost(authentication)
                ? stationService.getStationsForHost(authentication.getName()).stream()
                    .filter(station -> station.getCity() != null && station.getCity().toLowerCase().contains(city.toLowerCase()))
                    .collect(Collectors.toList())
                : stationService.getStationsByCity(city);

        return stations.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/downtown")
    public List<StationDTO> getDowntownStations(Authentication authentication) {
        List<Station> stations = isHost(authentication)
                ? stationService.getStationsForHost(authentication.getName()).stream()
                    .filter(station -> Boolean.TRUE.equals(station.getDowntown()))
                    .collect(Collectors.toList())
                : stationService.getDowntownStations();

        return stations.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/search")
    public List<StationDTO> searchStationsByName(@RequestParam String name, Authentication authentication) {
        List<Station> stations = isHost(authentication)
                ? stationService.getStationsForHost(authentication.getName()).stream()
                    .filter(station -> containsIgnoreCase(station.getName(), name) || containsIgnoreCase(station.getCity(), name))
                    .collect(Collectors.toList())
                : stationService.searchByName(name);

        return stations.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/geocode")
    public List<GeoLocationDTO> geocodeLocation(@RequestParam String query) {
        return geocodingService.searchLocations(query);
    }

    @GetMapping("/reverse-geocode")
    public ResponseEntity<GeoLocationDTO> reverseGeocodeLocation(@RequestParam Double lat, @RequestParam Double lng) {
        GeoLocationDTO result = geocodingService.reverseGeocode(lat, lng);
        return result == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(result);
    }

    @PostMapping
    public ResponseEntity<StationDTO> createStation(@Valid @RequestBody StationDTO dto, Authentication authentication) {
        Station created = stationService.createStation(toEntity(dto), authentication.getName());
        return ResponseEntity.ok(toDTO(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<StationDTO> updateStation(@PathVariable Long id, @Valid @RequestBody StationDTO dto, Authentication authentication) {
        Station updated = stationService.updateStation(id, toEntity(dto), authentication.getName());
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStation(@PathVariable Long id, Authentication authentication) {
        stationService.deleteStation(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    private boolean isHost(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_HOST".equals(authority.getAuthority()));
    }

    private boolean containsIgnoreCase(String source, String query) {
        return source != null && query != null && source.toLowerCase().contains(query.toLowerCase());
    }
}
