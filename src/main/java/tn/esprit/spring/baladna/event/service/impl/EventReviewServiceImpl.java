package tn.esprit.spring.baladna.event.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.event.dto.EligibilityResponse;
import tn.esprit.spring.baladna.event.dto.EventReviewDTO;
import tn.esprit.spring.baladna.event.entity.Event;
import tn.esprit.spring.baladna.event.entity.EventReservation;
import tn.esprit.spring.baladna.event.entity.EventReview;
import tn.esprit.spring.baladna.event.repository.EventRepository;
import tn.esprit.spring.baladna.event.repository.EventReservationRepository;
import tn.esprit.spring.baladna.event.repository.EventReviewRepository;
import tn.esprit.spring.baladna.event.service.interfaces.IEventReviewService;

import java.util.List;

@AllArgsConstructor
@Service
public class EventReviewServiceImpl implements IEventReviewService {

    private final EventReviewRepository reviewRepository;
    private final EventRepository eventRepository;
    private final EventReservationRepository reservationRepository;

    @Override
    public List<EventReview> retrieveEventReviews() {
        return reviewRepository.findAll();
    }

    @Override
    public EventReview addEventReview(EventReviewDTO review) {
        // Validate event exists
        Event event = eventRepository.findById(review.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found"));
        
        // Validate reservation exists
        EventReservation reservation = reservationRepository.findById(review.getReservationId())
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
        
        // ❗ FIX 2: Validate reservation ownership
        if (!reservation.getUserId().equals(review.getUserId())) {
            throw new RuntimeException("Invalid reservation ownership");
        }
        // ❗ ENFORCE: Reservation must be PAID before review
        if (reservation.getPaymentStatus() != tn.esprit.spring.baladna.event.entity.enums.PaymentStatus.PAID) {
            throw new RuntimeException("Payment required before review");
        }
        
        // ❗ FIX 3: Validate reservation is for the correct event
        if (!reservation.getEvent().getId().equals(review.getEventId())) {
            throw new RuntimeException("Reservation does not belong to this event");
        }
        
        // Check if user already reviewed this event
        if (reviewRepository.existsByEventIdAndUserId(review.getEventId(), review.getUserId())) {
            throw new RuntimeException("User has already reviewed this event");
        }
        
        // ❗ FIX 4: Calculate sentiment score based on rating and comment keywords
        Float sentimentScore = calculateSentimentScore(review.getRating(), review.getComment());
        
        // Create the review
        EventReview entity = EventReview.builder()
                .event(event)
                .userId(review.getUserId())
                .reservation(reservation)
                .rating(review.getRating())
                .comment(review.getComment())
                .sentimentScore(sentimentScore)
                .build();
        
        // ❗ FIX 1: Handle DB constraint exception for race condition
        try {
            return reviewRepository.save(entity);
        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("User already reviewed this event");
        }
    }

    @Override
    public EventReview updateEventReview(EventReviewDTO review) {
        EventReview entity = reviewRepository.findById(review.getId())
                .orElseThrow(() -> new RuntimeException("Review not found"));
        
        if (review.getRating() != null) {
            entity.setRating(review.getRating());
        }
        if (review.getComment() != null) {
            entity.setComment(review.getComment());
        }
        // Recalculate sentiment score with updated rating and comment
        entity.setSentimentScore(calculateSentimentScore(entity.getRating(), entity.getComment()));
        
        return reviewRepository.save(entity);
    }

    @Override
    public EventReview retrieveEventReview(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));
    }

    @Override
    public void removeEventReview(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new RuntimeException("Review not found");
        }
        reviewRepository.deleteById(id);
    }

    @Override
    public List<EventReview> findByEventId(Long eventId) {
        return reviewRepository.findByEventId(eventId);
    }

    @Override
    public EventReview findByEventIdAndUserId(Long eventId, Long userId) {
        return reviewRepository.findByEventIdAndUserId(eventId, userId).orElse(null);
    }

    @Override
    public boolean existsByEventIdAndUserId(Long eventId, Long userId) {
        return reviewRepository.existsByEventIdAndUserId(eventId, userId);
    }

    @Override
    public double getAverageRating(Long eventId) {
        return reviewRepository.getAverageRatingByEventId(eventId);
    }

    @Override
    public long getCountByEventId(Long eventId) {
        return reviewRepository.countByEventId(eventId);
    }

    @Override
    public List<EventReview> findTopReviewsByEventId(Long eventId, int limit) {
        return reviewRepository.findTopReviewsByEventId(eventId, PageRequest.of(0, limit));
    }

    @Override
    public EligibilityResponse checkEligibility(Long userId, Long eventId) {
        // Find reservations for this user and event (any status)
        List<EventReservation> reservations =
                reservationRepository.findReservationsByUserAndEvent(userId, eventId);

             // Only allow review if at least one reservation is CONFIRMED and PAID
             EventReservation eligibleReservation = reservations.stream()
                 .filter(r -> r.getPaymentStatus() == tn.esprit.spring.baladna.event.entity.enums.PaymentStatus.PAID
                           && r.getStatus() == tn.esprit.spring.baladna.event.entity.enums.ReservationStatus.CONFIRMED)
                 .findFirst()
                 .orElse(null);

             boolean hasEligibleReservation = eligibleReservation != null;
        boolean alreadyReviewed = reviewRepository.existsByEventIdAndUserId(eventId, userId);

             Long reservationId = null;
             if (hasEligibleReservation) {
                 reservationId = eligibleReservation.getId();
             }

             return EligibilityResponse.builder()
                 .canReview(hasEligibleReservation && !alreadyReviewed)
                 .reservationId(reservationId)
                 .alreadyReviewed(alreadyReviewed)
                 .build();
    }

    @Override
    public EventReview addHostResponse(Long reviewId, String hostResponse) {
        EventReview entity = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        
        entity.setHostResponse(hostResponse);
        return reviewRepository.save(entity);
    }

    /**
     * Calculate sentiment score based on rating and optional keyword analysis.
     * Returns a value between -1.0 (negative) and 1.0 (positive).
     * Simple keyword detection makes it appear "AI-powered" in demos.
     */
    private Float calculateSentimentScore(Integer rating, String comment) {
        if (rating == null) return 0.0f;
        
        float sentiment = 0.0f;
        
        // Base sentiment from rating
        if (rating >= 4) {
            sentiment = 0.7f;  // Positive
        } else if (rating <= 2) {
            sentiment = -0.7f; // Negative
        } else {
            sentiment = 0.0f;  // Neutral (rating = 3)
        }
        
        // Keyword boost/cap for additional "AI" feel
        if (comment != null && !comment.isBlank()) {
            String lowerComment = comment.toLowerCase();
            
            // Positive keywords boost sentiment
            if (lowerComment.contains("amazing") || lowerComment.contains("perfect") ||
                lowerComment.contains("excellent") || lowerComment.contains("wonderful") ||
                lowerComment.contains("fantastic") || lowerComment.contains("loved")) {
                sentiment = Math.min(sentiment + 0.2f, 1.0f);
            }
            
            // Negative keywords reduce sentiment
            if (lowerComment.contains("bad") || lowerComment.contains("worst") ||
                lowerComment.contains("terrible") || lowerComment.contains("awful") ||
                lowerComment.contains("disappointing") || lowerComment.contains("hated")) {
                sentiment = Math.max(sentiment - 0.2f, -1.0f);
            }
        }
        
        return sentiment;
    }
    
    /**
     * Overload for backward compatibility (rating only)
     */
    private Float calculateSentimentScore(Integer rating) {
        return calculateSentimentScore(rating, null);
    }
}