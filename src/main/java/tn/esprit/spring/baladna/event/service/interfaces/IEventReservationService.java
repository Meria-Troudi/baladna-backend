package tn.esprit.spring.baladna.event.service.interfaces;

import tn.esprit.spring.baladna.event.entity.EventReservation;
import tn.esprit.spring.baladna.event.dto.ReservationWithEventDTO;
import java.util.List;

public interface IEventReservationService {
    EventReservation findWithEvent(Long reservationId);
    // New method for last reservation tab - get latest confirmed/waitlisted reservation
    List<EventReservation> getLastConfirmedOrWaitlistedReservations();
    
    // New methods for reservation with availability check and waitlist
    EventReservation createReservation(Long eventId, Long userId, int personsCount);

    // Deprecated: findByStripeId removed, use findByStripePaymentIntentId only

    void cancelReservation(Long reservationId);
    List<EventReservation> getUserReservations(Long userId);
    
    // Get event reservations for host view
    List<EventReservation> getEventReservations(Long eventId);

    // Get reserved event IDs for a user
    List<Long> getReservedEventIdsByUser(Long userId);

    // Update reservation (e.g., change person count)
    EventReservation updateReservation(Long eventId, Long reservationId, int personsCount);

    // Get reservation with event details (for edit mode - avoids @JsonBackReference issues)
    ReservationWithEventDTO getReservationWithEvent(Long reservationId);

    // Get all reservations (for admin dashboard)
    List<EventReservation> getAllReservations();

    // Get reservations with full event details (avoids @JsonBackReference issues)
    List<ReservationWithEventDTO> getUserReservationsWithEvent(Long userId);
    List<ReservationWithEventDTO> getEventReservationsWithEvent(Long eventId);
    List<ReservationWithEventDTO> getAllReservationsWithEvent();

    // Needed for payment flow
    EventReservation findById(Long id);
    EventReservation save(EventReservation reservation);
    java.util.Optional<EventReservation> findByStripePaymentIntentId(String stripePaymentIntentId);
    // Deprecated: findByStripeId removed, use findByStripePaymentIntentId only
}
