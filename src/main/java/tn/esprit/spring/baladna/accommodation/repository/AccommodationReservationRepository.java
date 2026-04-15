package tn.esprit.spring.baladna.accommodation.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.spring.baladna.accommodation.entity.AccommodationReservation;
import tn.esprit.spring.baladna.accommodation.entity.enums.AccommodationReservationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AccommodationReservationRepository extends JpaRepository<AccommodationReservation, UUID> {

    @Query("""
            SELECT r FROM AccommodationReservation r
            LEFT JOIN FETCH r.assignedRoom
            WHERE r.accommodation.id = :accId
            AND r.status IN :statuses
            AND r.checkIn < :end
            AND r.checkOut > :start
            """)
    List<AccommodationReservation> findOverlapping(
            @Param("accId") UUID accommodationId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("statuses") List<AccommodationReservationStatus> statuses
    );

    List<AccommodationReservation> findByUserIdOrderByCheckInDesc(Long userId);

    @EntityGraph(attributePaths = {"accommodation"})
    @Query("SELECT r FROM AccommodationReservation r WHERE r.userId = :userId ORDER BY r.checkIn DESC")
    List<AccommodationReservation> findByUserIdForSuggestions(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM AccommodationReservation r WHERE r.accommodation.id = :accId")
    void deleteAllByAccommodationId(@Param("accId") UUID accId);

    List<AccommodationReservation> findByBookingGroupIdAndUserId(UUID bookingGroupId, Long userId);

    @Query("""
            SELECT DISTINCT r FROM AccommodationReservation r
            JOIN FETCH r.accommodation a
            LEFT JOIN FETCH r.assignedRoom
            WHERE r.userId = :userId
            ORDER BY r.checkIn DESC
            """)
    List<AccommodationReservation> findMineDetailed(@Param("userId") Long userId);

    @Query("""
            SELECT DISTINCT r FROM AccommodationReservation r
            JOIN FETCH r.accommodation a
            LEFT JOIN FETCH r.assignedRoom
            WHERE a.hostId = :hostId
            ORDER BY r.createdAt DESC
            """)
    List<AccommodationReservation> findByHostIdForDashboard(@Param("hostId") Long hostId);
}
