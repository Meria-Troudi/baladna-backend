package tn.esprit.spring.baladna.event.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;
import tn.esprit.spring.baladna.event.service.CandidateService;

import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
public class RecommendationController {

    private final AiRecommendationClient aiClient;
    private final UserRepository userRepository;
    private final tn.esprit.spring.baladna.event.service.CandidateService candidateService;

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("Unauthorized");
        }
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Unauthorized"));
        return user.getId();
    }

    @GetMapping("/personalized")
    public ResponseEntity<?> personalized(Authentication authentication) {
        Long userId;
        try {
            userId = resolveUserId(authentication);
        } catch (Exception e) {
            // For testing: use default user ID if not authenticated
            userId = 1L;
        }
        
        try {
            // Use /recommend endpoint to get personalized recommendations
            List<Map<String, Object>> recommendations = aiClient.recommend(userId);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            // AI microservice may not be running in dev; degrade gracefully so the
            // tourist landing page just hides the "Recommended for you" section
            // instead of showing a 500 in the browser console.
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/explain")
public ResponseEntity<Map<String, Object>> explain(Authentication authentication, @RequestParam Long eventId) {
    Long userId = resolveUserId(authentication);
    return ResponseEntity.ok(aiClient.explain(userId, eventId));
}

// Add endpoint to get explanation for a single recommended event
@GetMapping("/explain/{eventId}")
public ResponseEntity<Map<String, Object>> explainForEvent(Authentication authentication, @PathVariable Long eventId) {
    Long userId = resolveUserId(authentication);
    return ResponseEntity.ok(aiClient.explain(userId, eventId));
}

    @GetMapping("/trending")
    public ResponseEntity<List<Map<String, Object>>> trending() {
        return ResponseEntity.ok(aiClient.trending());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        try {
            Map<String, Object> body = aiClient.health();
            body.put("available", true);
            body.put("modelName", "LGBMRanker");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "available", false,
                    "modelName", "LGBMRanker",
                    "error", e.getMessage()
            ));
        }
    }

    // ---------------- Host AI insights (per-event predictions) ----------------

    @GetMapping("/host/fill-rate/{eventId}")
    public ResponseEntity<Map<String, Object>> fillRate(@PathVariable Long eventId) {
        try {
            return ResponseEntity.ok(aiClient.fillRatePrediction(eventId));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("error", "AI service unavailable"));
        }
    }

    @GetMapping("/host/revenue-forecast/{eventId}")
    public ResponseEntity<Map<String, Object>> revenueForecast(@PathVariable Long eventId) {
        try {
            return ResponseEntity.ok(aiClient.revenueForecast(eventId));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("error", "AI service unavailable"));
        }
    }

    @GetMapping("/host/rating-prediction/{eventId}")
    public ResponseEntity<Map<String, Object>> ratingPrediction(@PathVariable Long eventId) {
        try {
            return ResponseEntity.ok(aiClient.ratingPrediction(eventId));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("error", "AI service unavailable"));
        }
    }

    @GetMapping("/host/tips/{eventId}")
    public ResponseEntity<Map<String, Object>> actionableTips(@PathVariable Long eventId) {
        try {
            return ResponseEntity.ok(aiClient.actionableTips(eventId));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("error", "AI service unavailable"));
        }
    }
}