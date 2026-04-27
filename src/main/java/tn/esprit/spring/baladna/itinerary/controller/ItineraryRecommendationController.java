package tn.esprit.spring.baladna.itinerary.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.itinerary.dto.RecommendationRequest;
import tn.esprit.spring.baladna.itinerary.dto.RecommendationResponse;
import tn.esprit.spring.baladna.itinerary.service.ItineraryRecommendationService;
import tn.esprit.spring.baladna.itinerary.service.TrainingDataGeneratorService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for AI-powered itinerary recommendations
 */
@RestController
@RequestMapping("/api/itinerary/recommendations")
@RequiredArgsConstructor
@Slf4j
public class ItineraryRecommendationController {

    private final ItineraryRecommendationService recommendationService;
    private final TrainingDataGeneratorService trainingDataGeneratorService;

    /**
     * Get itinerary recommendations based on criteria
     * POST /api/itinerary/recommendations/search
     */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> getRecommendations(
        @RequestBody RecommendationRequest request) {

        log.info("Received recommendation request for location: {}, budget: {}",
                 request.getLocation(), request.getMaxBudget());

        try {
            List<RecommendationResponse> recommendations = 
                recommendationService.getRecommendations(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", recommendations.size());
            response.put("recommendations", recommendations);
            response.put("message", recommendations.isEmpty() ? 
                        "No recommendations found matching your criteria" : 
                        "Found " + recommendations.size() + " recommendations");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error generating recommendations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error generating recommendations: " + e.getMessage()));
        }
    }

    /**
     * Get recommendations similar to a specific itinerary
     * GET /api/itinerary/recommendations/similar/{itineraryId}
     */
    @GetMapping("/similar/{itineraryId}")
    public ResponseEntity<Map<String, Object>> getSimilarRecommendations(
        @PathVariable UUID itineraryId,
        @RequestParam(defaultValue = "5") int limit) {

        log.info("Getting similar recommendations for itinerary: {}", itineraryId);

        try {
            List<RecommendationResponse> recommendations = 
                recommendationService.getRelatedRecommendations(itineraryId, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("count", recommendations.size());
            response.put("recommendations", recommendations);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting similar recommendations", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error getting similar recommendations: " + e.getMessage()));
        }
    }

    /**
     * Generate training data from existing itineraries
     * POST /api/itinerary/recommendations/train/generate
     * (Admin only - should be protected)
     */
    @PostMapping("/train/generate")
    public ResponseEntity<Map<String, Object>> generateTrainingData() {
        log.info("Generating training data from existing itineraries");

        try {
            int generated = trainingDataGeneratorService.generateTrainingDataFromAllItineraries();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("recordsGenerated", generated);
            response.put("message", "Training data generated for " + generated + " itineraries");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error generating training data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error generating training data: " + e.getMessage()));
        }
    }

    /**
     * Generate synthetic training data for testing
     * POST /api/itinerary/recommendations/train/synthetic
     * (Admin only - should be protected)
     */
    @PostMapping("/train/synthetic")
    public ResponseEntity<Map<String, Object>> generateSyntheticData(
        @RequestParam(defaultValue = "100") int count) {

        log.info("Generating {} synthetic training records", count);

        try {
            int generated = trainingDataGeneratorService.generateSyntheticTrainingData(count);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("recordsGenerated", generated);
            response.put("message", "Synthetic training data generated");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error generating synthetic training data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error generating synthetic data: " + e.getMessage()));
        }
    }

    /**
     * Export training data as CSV
     * GET /api/itinerary/recommendations/train/export
     */
    @GetMapping("/train/export")
    public ResponseEntity<String> exportTrainingDataAsCSV() {
        log.info("Exporting training data as CSV");

        try {
            String csvData = trainingDataGeneratorService.exportTrainingDataAsCSV();

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                       "attachment; filename=\"training_data.csv\"")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=utf-8")
                .body(csvData);

        } catch (Exception e) {
            log.error("Error exporting training data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error exporting training data: " + e.getMessage());
        }
    }

    /**
     * Get training data statistics
     * GET /api/itinerary/recommendations/train/statistics
     */
    @GetMapping("/train/statistics")
    public ResponseEntity<Map<String, Object>> getTrainingDataStatistics() {
        log.info("Getting training data statistics");

        try {
            String statistics = trainingDataGeneratorService.getTrainingDataStatistics();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", statistics);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error getting training data statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error getting statistics: " + e.getMessage()));
        }
    }

    /**
     * Normalize training data
     * POST /api/itinerary/recommendations/train/normalize
     * (Admin only - should be protected)
     */
    @PostMapping("/train/normalize")
    public ResponseEntity<Map<String, Object>> normalizeTrainingData() {
        log.info("Normalizing training data");

        try {
            trainingDataGeneratorService.normalizeAllTrainingData();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Training data normalized successfully");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error normalizing training data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error normalizing training data: " + e.getMessage()));
        }
    }

    /**
     * Helper method to create error responses
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }
}
