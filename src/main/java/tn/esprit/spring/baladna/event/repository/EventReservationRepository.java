package tn.esprit.spring.baladna.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.event.entity.EventReservation;
import tn.esprit.spring.baladna.event.entity.Event;
import tn.esprit.spring.baladna.event.entity.enums.ReservationStatus;

import java.util.List;
import java.util.Optional;

@Repository


public interface EventReservationRepository extends JpaRepository<EventReservation, Long> {
    @Query("SELECT r FROM EventReservation r JOIN FETCH r.event WHERE r.id = :id")
    Optional<EventReservation> findWithEvent(@Param("id") Long id);
    
@Query("SELECT r FROM EventReservation r WHERE r.userId = :userId ORDER BY r.createdAt DESC")
List<EventReservation> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT r FROM EventReservation r WHERE r.status IN ('CONFIRMED', 'WAITLISTED') ORDER BY r.createdAt DESC")
    List<EventReservation> findTopConfirmedOrWaitlistedReservations();
    
    // Methods for waitlist management
    List<EventReservation> findByEventAndStatusOrderByCreatedAtAsc(Event event, ReservationStatus status);
    
    List<EventReservation> findByEvent(Event event);
    
    // Get reservations by user and status for reserved event IDs
List<EventReservation> findByUserIdAndStatusIn(Long userId, List<ReservationStatus> statuses);
    
    // Get reservations by user and event (for review eligibility)
    // Note: eventId refers to the Event entity's id field
    List<EventReservation> findByUserIdAndEventId(Long userId, Long eventId);

    // Find a single reservation by eventId and userId (for state logic)
    @Query("SELECT r FROM EventReservation r WHERE r.event.id = :eventId AND r.userId = :userId")
    java.util.Optional<EventReservation> findByEventIdAndUserId(@Param("eventId") Long eventId, @Param("userId") Long userId);
    
    // Alternative query method using @Query for explicit control
@Query("SELECT r FROM EventReservation r WHERE r.userId = :userId AND r.event.id = :eventId")
List<EventReservation> findReservationsByUserAndEvent(@Param("userId") Long userId, @Param("eventId") Long eventId);

    java.util.Optional<EventReservation> findByStripePaymentIntentId(String stripePaymentIntentId);
}
