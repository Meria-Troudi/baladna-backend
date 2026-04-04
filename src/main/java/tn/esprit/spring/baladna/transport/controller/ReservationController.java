package tn.esprit.spring.baladna.transport.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.transport.dto.ReservationDTO;
import tn.esprit.spring.baladna.transport.dto.ReservationRequestDTO;
import tn.esprit.spring.baladna.transport.entity.Reservation;
import tn.esprit.spring.baladna.transport.service.ReservationService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ReservationController {

    private final ReservationService reservationService;

    private ReservationDTO toDTO(Reservation reservation) {
        return ReservationDTO.builder()
                .id(reservation.getId())
                .reservedSeats(reservation.getReservedSeats())
                .totalPrice(reservation.getTotalPrice())
                .pricePerSeat(reservation.getPricePerSeat())
                .reservationDate(reservation.getReservationDate())
                .boardingPoint(reservation.getBoardingPoint())
                .status(reservation.getStatus())
                .transportId(reservation.getTransport().getId())
                .transportDeparturePoint(reservation.getTransport().getDeparturePoint())
                .transportRoute(
                        reservation.getTransport().getTrajet().getDepartureStation().getName()
                                + " → " +
                                reservation.getTransport().getTrajet().getArrivalStation().getName()
                )
                .userId(reservation.getUser().getId())
                .userFullName(reservation.getUser().getFirstName() + " " + reservation.getUser().getLastName())
                .userEmail(reservation.getUser().getEmail())
                .build();
    }

    // HOST
    @GetMapping
    public List<ReservationDTO> getAllReservations() {
        return reservationService.getAllReservations().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // HOST
    @GetMapping("/{id}")
    public ResponseEntity<ReservationDTO> getReservationById(@PathVariable Long id) {
        Reservation reservation = reservationService.getReservationById(id);
        if (reservation == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(reservation));
    }

    // HOST
    @GetMapping("/transport/{transportId}")
    public List<ReservationDTO> getReservationsByTransport(@PathVariable Long transportId) {
        return reservationService.getReservationsByTransport(transportId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // TOURIST
    @GetMapping("/me")
    public List<ReservationDTO> getMyReservations(Authentication authentication) {
        String userEmail = authentication.getName();
        return reservationService.getReservationsByUserEmail(userEmail).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // TOURIST
    @PostMapping
    public ResponseEntity<?> makeReservation(@Valid @RequestBody ReservationRequestDTO request,
                                             Authentication authentication) {
        try {
            String userEmail = authentication.getName();

            Reservation reservation = reservationService.makeReservation(
                    request.getTransportId(),
                    userEmail,
                    request.getBoardingPoint(),
                    request.getSeatsCount()
            );

            return ResponseEntity.ok(toDTO(reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // TOURIST
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelReservation(@PathVariable Long id, Authentication authentication) {
        try {
            String userEmail = authentication.getName();
            Reservation reservation = reservationService.cancelReservation(id, userEmail);
            return ResponseEntity.ok(toDTO(reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // HOST
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }
}