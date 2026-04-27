package tn.esprit.spring.baladna.itinerary.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.itinerary.dto.RecommendationRequest;
import tn.esprit.spring.baladna.itinerary.dto.RecommendationResponse;
import tn.esprit.spring.baladna.itinerary.entity.Itinerary;
import tn.esprit.spring.baladna.itinerary.entity.TrainingDataset;
import tn.esprit.spring.baladna.itinerary.repository.ItineraryRepository;
import tn.esprit.spring.baladna.itinerary.repository.TrainingDatasetRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-powered recommendation service using k-NN collaborative filtering
 * Recommends itineraries based on price, location, and other features
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ItineraryRecommendationService {

    private final TrainingDatasetRepository trainingDatasetRepository;
    private final ItineraryRepository itineraryRepository;

    /**
     * Get recommendations based on user criteria
     * Uses k-nearest neighbors algorithm to find similar itineraries
     */
    public List<RecommendationResponse> getRecommendations(RecommendationRequest request) {
        log.info("Generating recommendations for location: {}, budget: {}", 
                 request.getLocation(), request.getMaxBudget());

        // Get candidate itineraries based on filters
        List<TrainingDataset> candidates = getCandidates(request);

        if (candidates.isEmpty()) {
            log.warn("No candidates found for recommendations");
            return new ArrayList<>();
        }

        // Calculate similarity scores
        Map<UUID, Double> similarityScores = calculateSimilarityScores(candidates, request);

        // Get top k recommendations
        int limit = request.getLimit() != null ? request.getLimit() : 5;
        List<RecommendationResponse> recommendations = similarityScores.entrySet()
            .stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
            .limit(limit)
            .map(entry -> buildRecommendationResponse(entry.getKey(), entry.getValue(), request))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        log.info("Generated {} recommendations", recommendations.size());
        return recommendations;
    }

    /**
     * Get candidates based on filters
     */
    private List<TrainingDataset> getCandidates(RecommendationRequest request) {
        List<TrainingDataset> candidates = new ArrayList<>();

        // Filter by location
        if (request.getLocation() != null && !request.getLocation().isEmpty()) {
            if (Boolean.TRUE.equals(request.getExactLocationMatch())) {
                candidates = trainingDatasetRepository.findByLocation(request.getLocation());
            } else {
                // Get all and filter by similar location (case-insensitive)
                String locationLower = request.getLocation().toLowerCase();
                candidates = trainingDatasetRepository.findAll()
                    .stream()
                    .filter(t -> t.getLocation().toLowerCase().contains(locationLower) ||
                               locationLower.contains(t.getLocation().toLowerCase()))
                    .collect(Collectors.toList());
            }
        } else {
            candidates = trainingDatasetRepository.findAll();
        }

        // Filter by budget
        if (request.getMaxBudget() != null) {
            candidates = candidates.stream()
                .filter(t -> t.getBudget().compareTo(request.getMaxBudget()) <= 0)
                .collect(Collectors.toList());
        }

        // Filter by duration
        if (request.getMinDuration() != null && request.getMaxDuration() != null) {
            candidates = candidates.stream()
                .filter(t -> t.getDurationDays() >= request.getMinDuration() &&
                           t.getDurationDays() <= request.getMaxDuration())
                .collect(Collectors.toList());
        }

        // Filter by minimum rating if specified
        if (request.getMinRating() != null) {
            candidates = candidates.stream()
                .filter(t -> t.getRating() != null && 
                           t.getRating().compareTo(request.getMinRating()) >= 0)
                .collect(Collectors.toList());
        }

        return candidates;
    }

    /**
     * Calculate similarity scores using cosine similarity
     * Considers budget, duration, and complexity
     */
    private Map<UUID, Double> calculateSimilarityScores(
        List<TrainingDataset> candidates,
        RecommendationRequest request) {

        Map<UUID, Double> scores = new HashMap<>();

        // Create normalized request vector
        double[] requestVector = normalizeRequestVector(request);

        for (TrainingDataset candidate : candidates) {
            // Skip candidates without an associated itinerary
            if (candidate.getItinerary() == null) {
                continue;
            }

            // Create normalized candidate vector
            double[] candidateVector = normalizeCandidateVector(candidate);

            // Calculate cosine similarity
            double similarity = cosineSimilarity(requestVector, candidateVector);

            // Boost score based on rating if available
            if (candidate.getRating() != null) {
                double ratingBoost = candidate.getRating().doubleValue() / 5.0; // 0.0 to 1.0
                similarity = similarity * 0.8 + ratingBoost * 0.2; // 80% similarity, 20% rating
            }

            scores.put(candidate.getItinerary().getId(), similarity);
        }

        return scores;
    }

    /**
     * Normalize request into feature vector
     */
    private double[] normalizeRequestVector(RecommendationRequest request) {
        double[] vector = new double[3];

        // Budget (normalize to 0-1)
        BigDecimal maxBudget = trainingDatasetRepository.getMaxBudget();
        if (request.getMaxBudget() != null && maxBudget.compareTo(BigDecimal.ZERO) > 0) {
            vector[0] = request.getMaxBudget().divide(maxBudget, 4, RoundingMode.HALF_UP)
                              .doubleValue();
            vector[0] = Math.min(vector[0], 1.0);
        }

        // Duration (normalize to 0-1)
        Integer maxDuration = trainingDatasetRepository.getMaxDuration();
        if (request.getMaxDuration() != null && maxDuration > 0) {
            vector[1] = (double) request.getMaxDuration() / maxDuration;
            vector[1] = Math.min(vector[1], 1.0);
        } else {
            vector[1] = 0.5; // Default to middle if not specified
        }

        // Complexity/number of features (default medium)
        vector[2] = 0.5;

        normalizeVector(vector);
        return vector;
    }

    /**
     * Normalize candidate itinerary into feature vector
     */
    private double[] normalizeCandidateVector(TrainingDataset candidate) {
        double[] vector = new double[3];

        // Budget (normalize to 0-1)
        BigDecimal maxBudget = trainingDatasetRepository.getMaxBudget();
        if (maxBudget.compareTo(BigDecimal.ZERO) > 0) {
            vector[0] = candidate.getBudget().divide(maxBudget, 4, RoundingMode.HALF_UP)
                                 .doubleValue();
            vector[0] = Math.min(vector[0], 1.0);
        }

        // Duration (normalize to 0-1)
        Integer maxDuration = trainingDatasetRepository.getMaxDuration();
        if (maxDuration > 0) {
            vector[1] = (double) candidate.getDurationDays() / maxDuration;
            vector[1] = Math.min(vector[1], 1.0);
        }

        // Complexity (normalized number of steps and collaborators)
        int maxSteps = trainingDatasetRepository.findAll().stream()
            .mapToInt(TrainingDataset::getNumSteps)
            .max()
            .orElse(1);
        double complexityScore = (double) candidate.getNumSteps() / Math.max(maxSteps, 1);
        complexityScore += (double) candidate.getNumCollaborators() / 10.0; // Weight collaborators
        vector[2] = Math.min(complexityScore / 2.0, 1.0);

        normalizeVector(vector);
        return vector;
    }

    /**
     * Normalize vector to unit length
     */
    private void normalizeVector(double[] vector) {
        double magnitude = Math.sqrt(vector[0] * vector[0] + 
                                   vector[1] * vector[1] + 
                                   vector[2] * vector[2]);
        if (magnitude > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= magnitude;
            }
        }
    }

    /**
     * Calculate cosine similarity between two vectors
     */
    private double cosineSimilarity(double[] vector1, double[] vector2) {
        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;

        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            magnitude1 += vector1[i] * vector1[i];
            magnitude2 += vector2[i] * vector2[i];
        }

        magnitude1 = Math.sqrt(magnitude1);
        magnitude2 = Math.sqrt(magnitude2);

        if (magnitude1 == 0.0 || magnitude2 == 0.0) {
            return 0.0;
        }

        return dotProduct / (magnitude1 * magnitude2);
    }

    /**
     * Build recommendation response
     */
    private RecommendationResponse buildRecommendationResponse(
        UUID itineraryId,
        Double similarityScore,
        RecommendationRequest request) {

        Optional<Itinerary> itineraryOpt = itineraryRepository.findById(itineraryId);
        if (itineraryOpt.isEmpty()) {
            return null;
        }

        Itinerary itinerary = itineraryOpt.get();
        long durationDays = ChronoUnit.DAYS.between(
            itinerary.getStartDate(),
            itinerary.getEndDate()
        );

        BigDecimal avgDailyCost = BigDecimal.ZERO;
        if (durationDays > 0 && itinerary.getEstimatedBudget() != null) {
            avgDailyCost = itinerary.getEstimatedBudget()
                .divide(BigDecimal.valueOf(durationDays), 2, RoundingMode.HALF_UP);
        }

        String reason = generateRecommendationReason(itinerary, similarityScore, request);

        return RecommendationResponse.builder()
            .itineraryId(itinerary.getId())
            .title(itinerary.getTitle())
            .description(itinerary.getDescription())
            .destination(itinerary.getDestinationRegion())
            .budget(itinerary.getEstimatedBudget())
            .durationDays((int) durationDays)
            .startDate(itinerary.getStartDate())
            .endDate(itinerary.getEndDate())
            .numSteps(itinerary.getSteps().size())
            .avgDailyCost(avgDailyCost)
            .similarityScore(BigDecimal.valueOf(similarityScore)
                           .setScale(4, RoundingMode.HALF_UP))
            .rating(BigDecimal.valueOf(4.5)) // TODO: Calculate from actual ratings
            .numCollaborators(itinerary.getCollaborators().size())
            .recommendationReason(reason)
            .build();
    }

    /**
     * Generate human-readable reason for recommendation
     */
    private String generateRecommendationReason(
        Itinerary itinerary,
        Double similarityScore,
        RecommendationRequest request) {

        StringBuilder reason = new StringBuilder();

        if (similarityScore > 0.8) {
            reason.append("Highly relevant itinerary. ");
        } else if (similarityScore > 0.6) {
            reason.append("Well-matched itinerary. ");
        } else {
            reason.append("Recommended itinerary. ");
        }

        reason.append("Budget: ").append(itinerary.getEstimatedBudget());

        if (request.getLocation() != null && itinerary.getDestinationRegion() != null) {
            if (itinerary.getDestinationRegion().toLowerCase()
                .contains(request.getLocation().toLowerCase())) {
                reason.append(". Located in your preferred region.");
            }
        }

        if (itinerary.getSteps().size() > 0) {
            reason.append(". Includes ").append(itinerary.getSteps().size())
                   .append(" activities.");
        }

        return reason.toString();
    }

    /**
     * Get recommendations similar to a specific itinerary
     */
    public List<RecommendationResponse> getRelatedRecommendations(UUID itineraryId, int limit) {
        Optional<Itinerary> itineraryOpt = itineraryRepository.findById(itineraryId);
        if (itineraryOpt.isEmpty()) {
            return new ArrayList<>();
        }

        Itinerary itinerary = itineraryOpt.get();

        // Create a recommendation request based on the itinerary
        RecommendationRequest request = RecommendationRequest.builder()
            .location(itinerary.getDestinationRegion())
            .maxBudget(itinerary.getEstimatedBudget() != null ? 
                      itinerary.getEstimatedBudget().multiply(BigDecimal.valueOf(1.2)) : 
                      null)
            .limit(limit + 1) // +1 to account for the original itinerary
            .build();

        List<RecommendationResponse> recommendations = getRecommendations(request);

        // Remove the original itinerary if it appears in results
        return recommendations.stream()
            .filter(r -> !r.getItineraryId().equals(itineraryId))
            .limit(limit)
            .collect(Collectors.toList());
    }
}
