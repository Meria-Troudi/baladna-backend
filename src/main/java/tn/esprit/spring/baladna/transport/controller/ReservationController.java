package tn.esprit.spring.baladna.transport.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.transport.dto.ReservationDTO;
import tn.esprit.spring.baladna.transport.dto.ReservationRequestDTO;
import tn.esprit.spring.baladna.transport.dto.ReservationTicketValidationRequestDTO;
import tn.esprit.spring.baladna.transport.dto.ReservationTicketValidationResponseDTO;
import tn.esprit.spring.baladna.transport.entity.Reservation;
import tn.esprit.spring.baladna.transport.entity.Transport;
import tn.esprit.spring.baladna.transport.service.ReservationService;
import tn.esprit.spring.baladna.transport.service.ReservationTicketService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ReservationController {

    private final ReservationService reservationService;
    private final ReservationTicketService reservationTicketService;

    private ReservationDTO toDTO(Reservation reservation) {
        Transport transport = reservation.getTransport();

        return ReservationDTO.builder()
                .id(reservation.getId())
                .ticketCode(reservationTicketService.generateTicketCode(reservation))
                .reservedSeats(reservation.getReservedSeats())
                .totalPrice(reservation.getTotalPrice())
                .pricePerSeat(reservation.getPricePerSeat())
                .reservationDate(reservation.getReservationDate())
                .boardingPoint(reservation.getBoardingPoint())
                .status(reservation.getStatus())
                .transportId(transport.getId())
                .transportDeparturePoint(transport.getDeparturePoint())
                .transportRoute(
                        transport.getTrajet().getDepartureStation().getName()
                                + " -> " +
                                transport.getTrajet().getArrivalStation().getName()
                )
                .transportDepartureDate(transport.getDepartureDate())
                .transportWeather(transport.getWeather())
                .transportWeatherTemperature(transport.getWeatherTemperature())
                .transportDelayMinutes(transport.calculateDelay())
                .userId(reservation.getUser().getId())
                .userFullName(reservation.getUser().getFirstName() + " " + reservation.getUser().getLastName())
                .userEmail(reservation.getUser().getEmail())
                .build();
    }

    private boolean hasRole(Authentication authentication, String roleName) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        String expected = "ROLE_" + roleName;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(expected::equals);
    }

    @GetMapping
    public List<ReservationDTO> getReservations(Authentication authentication) {
        List<Reservation> reservations;

        if (hasRole(authentication, "TOURIST")) {
            reservations = reservationService.getReservationsByUserEmail(authentication.getName());
        } else {
            reservations = reservationService.getReservationsForHost(authentication.getName());
        }

        return reservations.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/pending")
    public List<ReservationDTO> getPendingReservations(Authentication authentication) {
        return reservationService.getPendingReservationsForHost(authentication.getName()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationDTO> getReservationById(@PathVariable Long id, Authentication authentication) {
        Reservation reservation = reservationService.getReservationByIdForHost(id, authentication.getName());
        if (reservation == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(toDTO(reservation));
    }

    @GetMapping("/transport/{transportId}")
    public List<ReservationDTO> getReservationsByTransport(@PathVariable Long transportId, Authentication authentication) {
        return reservationService.getReservationsByTransportForHost(transportId, authentication.getName()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/me")
    public List<ReservationDTO> getMyReservations(Authentication authentication) {
        return reservationService.getReservationsByUserEmail(authentication.getName()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<?> makeReservation(@Valid @RequestBody ReservationRequestDTO request,
                                             Authentication authentication) {
        try {
            Reservation reservation = reservationService.makeReservation(
                    request.getTransportId(),
                    authentication.getName(),
                    request.getBoardingPoint(),
                    request.getSeatsCount()
            );
            return ResponseEntity.ok(toDTO(reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelReservation(@PathVariable Long id, Authentication authentication) {
        try {
            Reservation reservation = reservationService.cancelReservation(id, authentication.getName());
            return ResponseEntity.ok(toDTO(reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveReservation(@PathVariable Long id, Authentication authentication) {
        try {
            Reservation reservation = reservationService.approveReservation(id, authentication.getName());
            return ResponseEntity.ok(toDTO(reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectReservation(@PathVariable Long id, Authentication authentication) {
        try {
            Reservation reservation = reservationService.rejectReservation(id, authentication.getName());
            return ResponseEntity.ok(toDTO(reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/board")
    public ResponseEntity<?> markReservationAsBoarded(@PathVariable Long id, Authentication authentication) {
        try {
            Reservation reservation = reservationService.markAsBoarded(id, authentication.getName());
            return ResponseEntity.ok(toDTO(reservation));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/validate-ticket")
    public ResponseEntity<ReservationTicketValidationResponseDTO> validateTicket(
            @Valid @RequestBody ReservationTicketValidationRequestDTO request,
            Authentication authentication
    ) {
        ReservationTicketValidationResponseDTO response = reservationTicketService.validateTicketCode(
                request.getTicketCode(),
                authentication.getName()
        );
        if (response.isValid()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.badRequest().body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReservation(@PathVariable Long id, Authentication authentication) {
        try {
            if (hasRole(authentication, "TOURIST")) {
                reservationService.deleteReservationForTourist(id, authentication.getName());
            } else {
                reservationService.deleteReservation(id, authentication.getName());
            }
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}