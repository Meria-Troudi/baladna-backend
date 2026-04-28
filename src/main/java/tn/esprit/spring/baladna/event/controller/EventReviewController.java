package tn.esprit.spring.baladna.event.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.baladna.event.dto.EligibilityResponse;
import tn.esprit.spring.baladna.event.dto.EventReviewDTO;
import tn.esprit.spring.baladna.event.entity.EventReview;
import tn.esprit.spring.baladna.event.service.interfaces.IEventReviewService;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@RestController
@RequestMapping("/api/events/event-review")
public class EventReviewController {

    private final IEventReviewService reviewService;
    private final UserRepository userRepository;

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return user.getId();
    }

    @GetMapping("/list")
    public List<EventReview> retrieveEventReviews() {
        return reviewService.retrieveEventReviews();
    }

    @GetMapping("/get/{id}")
    public EventReview retrieveEventReview(@PathVariable Long id) {
        return reviewService.retrieveEventReview(id);
    }

    @PostMapping("/add")
    @ResponseStatus(HttpStatus.CREATED)
    public EventReview addEventReview(@RequestBody EventReviewDTO review, Authentication authentication) {
        review.setUserId(resolveUserId(authentication));
        return reviewService.addEventReview(review);
    }

    @PutMapping("/update")
    public EventReview updateEventReview(@RequestBody EventReviewDTO review) {
        return reviewService.updateEventReview(review);
    }

    @DeleteMapping("/delete/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeEventReview(@PathVariable Long id) {
        reviewService.removeEventReview(id);
    }

    @GetMapping("/by-event/{eventId}")
    public List<EventReview> getReviewsByEvent(@PathVariable Long eventId) {
        return reviewService.findByEventId(eventId);
    }

    @GetMapping("/by-event-user/{eventId}/me")
    public EventReview getMyReview(
            @PathVariable Long eventId,
            Authentication authentication) {
        return reviewService.findByEventIdAndUserId(eventId, resolveUserId(authentication));
    }

    @GetMapping("/average/{eventId}")
    public Map<String, Object> getAverageRating(@PathVariable Long eventId) {
        double average = reviewService.getAverageRating(eventId);
        long count = reviewService.getCountByEventId(eventId);

        Map<String, Object> response = new HashMap<>();
        response.put("averageRating", Math.round(average * 10.0) / 10.0);
        response.put("totalReviews", count);

        return response;
    }

    @GetMapping("/eligibility/{eventId}/me")
    public EligibilityResponse checkEligibility(
            @PathVariable Long eventId,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        EligibilityResponse response = reviewService.checkEligibility(userId, eventId);
        response.setUserId(userId); // Add userId to the response
        return response;
    }

    @GetMapping("/top/{eventId}")
    public List<EventReview> getTopReviews(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "2") int limit) {
        return reviewService.findTopReviewsByEventId(eventId, limit);
    }

    @PostMapping("/host-response/{reviewId}")
    public EventReview addHostResponse(
            @PathVariable Long reviewId,
            @RequestBody Map<String, String> requestBody) {
        String response = requestBody.get("hostResponse");
        if (response == null || response.trim().isEmpty()) {
            throw new IllegalArgumentException("Host response cannot be empty");
        }
        return reviewService.addHostResponse(reviewId, response);
    }

    @GetMapping("/summary/{eventId}")
    public Map<String, Object> getReviewSummary(@PathVariable Long eventId) {
        double average = reviewService.getAverageRating(eventId);
        long count = reviewService.getCountByEventId(eventId);

        Map<String, Object> summary = new HashMap<>();
        summary.put("averageRating", Math.round(average * 10.0) / 10.0);
        summary.put("totalReviews", count);

        return summary;
    }
}