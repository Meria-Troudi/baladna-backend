package tn.esprit.spring.baladna.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.event.entity.EventReview;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventReviewRepository extends JpaRepository<EventReview, Long> {
    
    /**
     * Find all reviews for a specific event
     */
    List<EventReview> findByEventId(Long eventId);
    
    /**
     * Find a review by event and user (to check for duplicates)
     */
    Optional<EventReview> findByEventIdAndUserId(Long eventId, Long userId);
    
    /**
     * Check if a user has already reviewed an event
     */
    boolean existsByEventIdAndUserId(Long eventId, Long userId);
    
    /**
     * Find all reviews by a specific user
     */
    List<EventReview> findByUserId(Long userId);
    
    /**
     * Get the average rating for an event
     */
    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM EventReview r WHERE r.event.id = :eventId")
    double getAverageRatingByEventId(@Param("eventId") Long eventId);
    
    /**
     * Get the count of reviews for an event
     */
    long countByEventId(Long eventId);
    
    /**
     * Find top reviews for an event (ordered by most recent)
     */
    List<EventReview> findTopByEventIdOrderByCreatedAtDesc(Long eventId, org.springframework.data.domain.Pageable pageable);
    
    /**
     * Find top N reviews for an event
     */
    @Query("SELECT r FROM EventReview r WHERE r.event.id = :eventId ORDER BY r.createdAt DESC")
    List<EventReview> findTopReviewsByEventId(@Param("eventId") Long eventId, org.springframework.data.domain.Pageable pageable);
}