package tn.esprit.spring.baladna.event.service.interfaces;

import tn.esprit.spring.baladna.event.dto.EligibilityResponse;
import tn.esprit.spring.baladna.event.dto.EventReviewDTO;
import tn.esprit.spring.baladna.event.entity.EventReview;

import java.util.List;

public interface IEventReviewService {
    
    // Basic CRUD operations
    List<EventReview> retrieveEventReviews();
    EventReview addEventReview(EventReviewDTO review);
    EventReview updateEventReview(EventReviewDTO review);
    EventReview retrieveEventReview(Long id);
    void removeEventReview(Long id);
    
    // New methods for review system
    List<EventReview> findByEventId(Long eventId);
    EventReview findByEventIdAndUserId(Long eventId, Long userId);
    boolean existsByEventIdAndUserId(Long eventId, Long userId);
    double getAverageRating(Long eventId);
    long getCountByEventId(Long eventId);
    List<EventReview> findTopReviewsByEventId(Long eventId, int limit);
    
    // Eligibility check
    EligibilityResponse checkEligibility(Long userId, Long eventId);
    
    // Host response
    EventReview addHostResponse(Long reviewId, String hostResponse);
}