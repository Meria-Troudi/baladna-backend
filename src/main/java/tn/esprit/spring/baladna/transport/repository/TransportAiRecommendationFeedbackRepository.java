package tn.esprit.spring.baladna.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.transport.entity.TransportAiRecommendationFeedback;

import java.util.Optional;

@Repository
public interface TransportAiRecommendationFeedbackRepository extends JpaRepository<TransportAiRecommendationFeedback, Long> {

    Optional<TransportAiRecommendationFeedback> findByIdAndUserEmail(Long id, String email);

    long countByRecommendedTransportHostEmail(String email);

    void deleteByRecommendedTransportId(Long transportId);

    @Modifying
    @Query("UPDATE TransportAiRecommendationFeedback feedback "
            + "SET feedback.booking = null, feedback.booked = false "
            + "WHERE feedback.booking.id = :reservationId")
    void clearBookingReferenceByReservationId(Long reservationId);
}
