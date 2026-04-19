package tn.esprit.spring.baladna.event.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.event.dto.EventReviewDTO;
import tn.esprit.spring.baladna.event.entity.EventReview;
import tn.esprit.spring.baladna.event.service.IEventReviewService;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/events/event-review")
public class EventReviewController {

    private final IEventReviewService reviewService;

    @GetMapping("/list")
    public List<EventReview> retrieveEventReviews() {
        return reviewService.retrieveEventReviews();
    }

    @GetMapping("/get/{id}")
    public EventReview retrieveEventReview(@PathVariable Long id) {
        return reviewService.retrieveEventReview(id);
    }

    @PostMapping("/add")
    public EventReview addEventReview(@RequestBody EventReviewDTO review) {
        return reviewService.addEventReview(review);
    }

    @PutMapping("/update")
    public EventReview updateEventReview(@RequestBody EventReviewDTO review) {
        return reviewService.updateEventReview(review);
    }

    @DeleteMapping("/delete/{id}")
    public void removeEventReview(@PathVariable Long id) {
        reviewService.removeEventReview(id);
    }
}
