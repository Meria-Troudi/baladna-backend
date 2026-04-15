package tn.esprit.spring.baladna.event.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.event.dto.EventReviewDTO;
import tn.esprit.spring.baladna.event.entity.Event;
import tn.esprit.spring.baladna.event.entity.EventReservation;
import tn.esprit.spring.baladna.event.entity.EventReview;
import tn.esprit.spring.baladna.event.repository.EventRepository;
import tn.esprit.spring.baladna.event.repository.EventReservationRepository;
import tn.esprit.spring.baladna.event.repository.EventReviewRepository;
import tn.esprit.spring.baladna.event.service.IEventReviewService;

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
        Event event = eventRepository.findById(review.getEventId()).orElse(null);
        EventReservation reservation = reservationRepository.findById(review.getReservationId()).orElse(null);
        EventReview entity = EventReview.builder()
                .event(event)
                .userId(review.getUserId())
                .reservation(reservation)
                .rating(review.getRating())
                .comment(review.getComment())
                .build();
        return reviewRepository.save(entity);
    }

    @Override
    public EventReview updateEventReview(EventReviewDTO review) {
        EventReview entity = reviewRepository.findById(review.getId()).orElse(null);
        if (entity == null) {
            return null;
        }
        if (review.getEventId() != null) {
            entity.setEvent(eventRepository.findById(review.getEventId()).orElse(null));
        }
        if (review.getReservationId() != null) {
            entity.setReservation(reservationRepository.findById(review.getReservationId()).orElse(null));
        }
        entity.setUserId(review.getUserId());
        entity.setRating(review.getRating());
        entity.setComment(review.getComment());
        return reviewRepository.save(entity);
    }

    @Override
    public EventReview retrieveEventReview(Long id) {
        return reviewRepository.findById(id).orElse(null);
    }

    @Override
    public void removeEventReview(Long id) {
        reviewRepository.deleteById(id);
    }
}
