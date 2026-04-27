package tn.esprit.spring.baladna.event.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/host/analytics")
@CrossOrigin(origins = "*")
@AllArgsConstructor
public class HostAnalyticsController {

    private final RestTemplate restTemplate;

    @GetMapping("/predict/{eventId}")
    public ResponseEntity<Object> predictEventPerformance(@PathVariable Long eventId) {
        String url = "http://localhost:5000/host/predict/" + eventId;
        return ResponseEntity.ok(restTemplate.getForObject(url, Object.class));
    }

    @PostMapping("/sentiment/analyze")
    public ResponseEntity<Object> analyzeSentiment(@RequestBody Object request) {
        String url = "http://localhost:5000/sentiment/analyze";
        return ResponseEntity.ok(restTemplate.postForObject(url, request, Object.class));
    }
}