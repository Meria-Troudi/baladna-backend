package tn.esprit.spring.baladna.accommodation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.spring.baladna.accommodation.entity.AccommodationReview;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccommodationReviewRepository extends JpaRepository<AccommodationReview, UUID> {

    Optional<AccommodationReview> findByUserIdAndReservationId(Long userId, UUID reservationId);

    @Query("""
            SELECT r FROM AccommodationReview r
            JOIN FETCH r.accommodation a
            WHERE a.hostId = :hostId
            ORDER BY r.createdAt DESC
            """)
    List<AccommodationReview> findAllForHost(@Param("hostId") Long hostId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM AccommodationReview ar WHERE ar.accommodation.id = :accId")
    void deleteByAccommodationId(@Param("accId") UUID accId);
}
