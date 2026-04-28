package tn.esprit.spring.baladna.event.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class AiRecommendationClient {

    private final RestTemplate restTemplate;
    @Value("${ai.service.url:http://localhost:5000}")
    private String aiServiceUrl;

    @Autowired
    public AiRecommendationClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    public List<Map<String, Object>> recommend(Long userId) {
        String url = aiServiceUrl + "/recommend/" + userId;
        Map response = restTemplate.getForObject(url, Map.class);
        return (List<Map<String, Object>>) response.get("recommendations");
    }

    /**
     * Sends a list of candidate event IDs to the AI service for ranking.
     *
     * @param userId       the user ID
     * @param candidateIds list of candidate event IDs
     * @return ranked list of eventId/score maps
     */
    public List<Map<String, Object>> rank(Long userId, List<Long> candidateIds) {
        String url = aiServiceUrl + "/rank";
        Map<String, Object> payload = Map.of(
                "userId", userId,
                "candidateIds", candidateIds
        );
        return restTemplate.postForObject(url, payload, List.class);
    }

    public Map<String, Object> explain(Long userId, Long eventId) {
        String url = aiServiceUrl + "/explain/" + userId + "/" + eventId;
        return restTemplate.getForObject(url, Map.class);
    }

    public List<Map<String, Object>> trending() {
        String url = aiServiceUrl + "/trending";
        return restTemplate.getForObject(url, List.class);
    }

    public Map<String, Object> health() {
        String url = aiServiceUrl + "/health";
        return restTemplate.getForObject(url, Map.class);
    }

    // ---------------- Host-side per-event predictions ----------------

    public Map<String, Object> fillRatePrediction(Long eventId) {
        String url = aiServiceUrl + "/host/fill-rate-prediction/" + eventId;
        return restTemplate.getForObject(url, Map.class);
    }

    public Map<String, Object> revenueForecast(Long eventId) {
        String url = aiServiceUrl + "/host/revenue-forecast/" + eventId;
        return restTemplate.getForObject(url, Map.class);
    }

    public Map<String, Object> ratingPrediction(Long eventId) {
        String url = aiServiceUrl + "/host/rating-prediction/" + eventId;
        return restTemplate.getForObject(url, Map.class);
    }

    public Map<String, Object> actionableTips(Long eventId) {
        String url = aiServiceUrl + "/host/actionable-tips/" + eventId;
        return restTemplate.getForObject(url, Map.class);
    }
}