package tn.esprit.spring.baladna.event.service;

import tn.esprit.spring.baladna.event.dto.EventReviewDTO;
import tn.esprit.spring.baladna.event.entity.EventReview;
import java.util.List;

public interface IEventReviewService {
    List<EventReview> retrieveEventReviews();
    EventReview addEventReview(EventReviewDTO review);
    EventReview updateEventReview(EventReviewDTO review);
    EventReview retrieveEventReview(Long id);
    void removeEventReview(Long id);
}
