package tn.esprit.spring.baladna.event.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.baladna.event.dto.ReservationWithEventDTO;
import tn.esprit.spring.baladna.event.entity.EventReservation;
import tn.esprit.spring.baladna.event.service.interfaces.IEventReservationService;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/events/event-reservation")
public class EventReservationController {

    private final IEventReservationService reservationService;
    private final UserRepository userRepository;

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return user.getId();
    }

    @GetMapping("/confirmed-waitlisted")
    public List<EventReservation> getLastConfirmedOrWaitlistedReservations() {
        return reservationService.getLastConfirmedOrWaitlistedReservations();
    }

    @PostMapping("/events/{eventId}/reserve")
    public EventReservation createReservation(
            @PathVariable Long eventId,
            @RequestParam int persons,
            Authentication authentication) {
        return reservationService.createReservation(eventId, resolveUserId(authentication), persons);
    }

    @PutMapping("/reservations/{id}/cancel")
    public void cancelReservation(@PathVariable Long id) {
        reservationService.cancelReservation(id);
    }

    @GetMapping("/users/me/reservations")
    public List<EventReservation> getMyReservations(Authentication authentication) {
        return reservationService.getUserReservations(resolveUserId(authentication));
    }

    @GetMapping("/events/{eventId}/reservations")
    public List<ReservationWithEventDTO> getEventReservations(@PathVariable Long eventId) {
        return reservationService.getEventReservationsWithEvent(eventId);
    }

    @GetMapping("/users/me/reserved-event-ids")
    public List<Long> getReservedEventIds(Authentication authentication) {
        return reservationService.getReservedEventIdsByUser(resolveUserId(authentication));
    }

    @PutMapping("/events/{eventId}/reservations/{reservationId}")
    public EventReservation updateReservation(
            @PathVariable Long eventId,
            @PathVariable Long reservationId,
            @RequestParam int persons) {
        return reservationService.updateReservation(eventId, reservationId, persons);
    }

    @GetMapping("/with-event/{id}")
    public ReservationWithEventDTO getReservationWithEvent(@PathVariable Long id) {
        return reservationService.getReservationWithEvent(id);
    }

    @GetMapping("/all")
    public List<ReservationWithEventDTO> getAllReservations() {
        return reservationService.getAllReservationsWithEvent();
    }
}