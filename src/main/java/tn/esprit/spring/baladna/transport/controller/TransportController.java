package tn.esprit.spring.baladna.transport.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.transport.dto.TransportDTO;
import tn.esprit.spring.baladna.transport.dto.WeatherPreviewDTO;
import tn.esprit.spring.baladna.transport.entity.Trajet;
import tn.esprit.spring.baladna.transport.entity.Transport;
import tn.esprit.spring.baladna.transport.service.TrajetService;
import tn.esprit.spring.baladna.transport.service.TransportService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transports")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TransportController {

    private final TransportService transportService;
    private final TrajetService trajetService;

    private TransportDTO toDTO(Transport transport) {
        return TransportDTO.builder()
                .id(transport.getId())
                .departurePoint(transport.getDeparturePoint())
                .departureDate(transport.getDepartureDate())
                .realDepartureDate(transport.getRealDepartureDate())
                .totalCapacity(transport.getTotalCapacity())
                .availableSeats(transport.getAvailableSeats())
                .status(transport.getStatus())
                .basePrice(transport.getBasePrice())
                .trafficJam(transport.getTrafficJam())
                .weather(transport.getWeather())
                .weatherSource(transport.getWeatherSource())
                .weatherTemperature(transport.getWeatherTemperature())
                .weatherWindSpeed(transport.getWeatherWindSpeed())
                .weatherPrecipitation(transport.getWeatherPrecipitation())
                .delayMinutes(transport.calculateDelay())
                .trajetId(transport.getTrajet().getId())
                .trajetDescription(
                        transport.getTrajet().getDepartureStation().getName()
                                + " -> " +
                                transport.getTrajet().getArrivalStation().getName()
                )
                .build();
    }

    private Transport toEntity(TransportDTO dto) {
        Trajet trajet = trajetService.getTrajetById(dto.getTrajetId());

        if (trajet == null) {
            throw new RuntimeException("Trajet invalide");
        }

        return Transport.builder()
                .id(dto.getId())
                .departurePoint(dto.getDeparturePoint())
                .departureDate(dto.getDepartureDate())
                .totalCapacity(dto.getTotalCapacity())
                .availableSeats(dto.getAvailableSeats())
                .status(dto.getStatus())
                .basePrice(dto.getBasePrice())
                .trafficJam(dto.getTrafficJam())
                .weather(dto.getWeather())
                .weatherSource(dto.getWeatherSource())
                .trajet(trajet)
                .build();
    }

    @GetMapping
    public List<TransportDTO> getAllTransports(Authentication authentication) {
        List<Transport> transports = isHost(authentication)
                ? transportService.getTransportsForHost(authentication.getName())
                : transportService.getAllTransports();

        return transports.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/available")
    public List<TransportDTO> getAvailableTransports(Authentication authentication) {
        List<Transport> transports = isHost(authentication)
                ? transportService.getAvailableTransportsForHost(authentication.getName())
                : transportService.getAvailableTransports();

        return transports.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransportDTO> getTransportById(@PathVariable Long id, Authentication authentication) {
        Transport transport = isHost(authentication)
                ? transportService.getTransportByIdForHost(id, authentication.getName())
                : transportService.getTransportById(id);
        if (transport == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(transport));
    }

    @GetMapping("/trajet/{trajetId}")
    public List<TransportDTO> getTransportsByTrajet(@PathVariable Long trajetId, Authentication authentication) {
        List<Transport> transports = isHost(authentication)
                ? transportService.getTransportsByTrajetForHost(trajetId, authentication.getName())
                : transportService.getTransportsByTrajet(trajetId);

        return transports.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/search")
    public List<TransportDTO> searchTransports(@RequestParam String departureCity,
                                               @RequestParam String arrivalCity,
                                               Authentication authentication) {
        List<Transport> transports = isHost(authentication)
                ? transportService.getTransportsByCitiesForHost(departureCity, arrivalCity, authentication.getName())
                : transportService.getTransportsByCities(departureCity, arrivalCity);

        return transports.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/weather-preview")
    public ResponseEntity<WeatherPreviewDTO> previewWeather(
            @RequestParam Long trajetId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime departureDate,
            Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        WeatherPreviewDTO preview = transportService.previewWeather(
                trajetId,
                departureDate,
                authentication.getName()
        );

        return ResponseEntity.ok(preview);
    }

    @PostMapping
    public ResponseEntity<TransportDTO> createTransport(@Valid @RequestBody TransportDTO dto, Authentication authentication) {
        Transport created = transportService.createTransport(toEntity(dto), authentication.getName());
        return ResponseEntity.ok(toDTO(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransportDTO> updateTransport(@PathVariable Long id,
                                                        @Valid @RequestBody TransportDTO dto,
                                                        Authentication authentication) {
        Transport updated = transportService.updateTransport(id, toEntity(dto), authentication.getName());
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransport(@PathVariable Long id, Authentication authentication) {
        transportService.deleteTransport(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    private boolean isHost(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream().anyMatch(authority -> "ROLE_HOST".equals(authority.getAuthority()));
    }
}