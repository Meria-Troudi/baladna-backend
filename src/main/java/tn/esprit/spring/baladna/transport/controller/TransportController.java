package tn.esprit.spring.baladna.transport.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.transport.dto.TransportDTO;
import tn.esprit.spring.baladna.transport.entity.Trajet;
import tn.esprit.spring.baladna.transport.entity.Transport;
import tn.esprit.spring.baladna.transport.service.TrajetService;
import tn.esprit.spring.baladna.transport.service.TransportService;

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
                .delayMinutes(transport.calculateDelay())
                .trajetId(transport.getTrajet().getId())
                .trajetDescription(
                        transport.getTrajet().getDepartureStation().getName()
                                + " → " +
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
                .trajet(trajet)
                .build();
    }

    @GetMapping
    public List<TransportDTO> getAllTransports() {
        return transportService.getAllTransports().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/available")
    public List<TransportDTO> getAvailableTransports() {
        return transportService.getAvailableTransports().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransportDTO> getTransportById(@PathVariable Long id) {
        Transport transport = transportService.getTransportById(id);
        if (transport == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(transport));
    }

    @GetMapping("/trajet/{trajetId}")
    public List<TransportDTO> getTransportsByTrajet(@PathVariable Long trajetId) {
        return transportService.getTransportsByTrajet(trajetId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/search")
    public List<TransportDTO> searchTransports(@RequestParam String departureCity,
                                               @RequestParam String arrivalCity) {
        return transportService.getTransportsByCities(departureCity, arrivalCity).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<TransportDTO> createTransport(@Valid @RequestBody TransportDTO dto) {
        Transport created = transportService.createTransport(toEntity(dto));
        return ResponseEntity.ok(toDTO(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransportDTO> updateTransport(@PathVariable Long id,
                                                        @Valid @RequestBody TransportDTO dto) {
        Transport updated = transportService.updateTransport(id, toEntity(dto));
        if (updated == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransport(@PathVariable Long id) {
        transportService.deleteTransport(id);
        return ResponseEntity.noContent().build();
    }
}