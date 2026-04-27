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
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/explain")
    public ResponseEntity<Map<String, Object>> explain(
            Authentication authentication,
            @RequestParam Long eventId) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(aiClient.explain(userId, eventId));
    }

    @GetMapping("/trending")
    public ResponseEntity<List<Map<String, Object>>> trending() {
        return ResponseEntity.ok(aiClient.trending());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(aiClient.health());
    }
}