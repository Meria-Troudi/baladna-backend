package tn.esprit.spring.baladna.itinerary.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Configuration for AI recommendation system
 */
@Component
@Getter
public class RecommendationConfig {

    /**
     * Default number of recommendations to return
     */
    @Value("${recommendation.default-limit:5}")
    private Integer defaultLimit;

    /**
     * Maximum number of recommendations
     */
    @Value("${recommendation.max-limit:50}")
    private Integer maxLimit;

    /**
     * K value for k-NN algorithm
     */
    @Value("${recommendation.k-neighbors:5}")
    private Integer kNeighbors;

    /**
     * Weight for budget similarity (0.0 to 1.0)
     */
    @Value("${recommendation.weights.budget:0.4}")
    private Double budgetWeight;

    /**
     * Weight for duration similarity (0.0 to 1.0)
     */
    @Value("${recommendation.weights.duration:0.3}")
    private Double durationWeight;

    /**
     * Weight for complexity similarity (0.0 to 1.0)
     */
    @Value("${recommendation.weights.complexity:0.3}")
    private Double complexityWeight;

    /**
     * Weight for rating in final score (0.0 to 1.0)
     */
    @Value("${recommendation.weights.rating:0.2}")
    private Double ratingWeight;

    /**
     * Minimum similarity threshold for recommendations
     */
    @Value("${recommendation.min-similarity-threshold:0.4}")
    private Double minSimilarityThreshold;

    /**
     * Enable caching of recommendations
     */
    @Value("${recommendation.enable-caching:true}")
    private Boolean enableCaching;

    /**
     * Cache duration in minutes
     */
    @Value("${recommendation.cache-duration-minutes:60}")
    private Integer cacheDurationMinutes;

    /**
     * Enable synthetic data generation endpoint
     */
    @Value("${recommendation.enable-synthetic-generation:true}")
    private Boolean enableSyntheticGeneration;

    /**
     * Maximum number of synthetic records to generate at once
     */
    @Value("${recommendation.max-synthetic-records:10000}")
    private Integer maxSyntheticRecords;

    /**
     * Enable logging of recommendations
     */
    @Value("${recommendation.enable-logging:true}")
    private Boolean enableLogging;

    /**
     * Algorithm to use: COSINE_SIMILARITY, EUCLIDEAN, MANHATTAN
     */
    @Value("${recommendation.algorithm:COSINE_SIMILARITY}")
    private String algorithm;

    /**
     * Boost score if recommendation is exact location match
     */
    @Value("${recommendation.location-match-boost:0.1}")
    private Double locationMatchBoost;

    /**
     * Minimum rating threshold (0.0 to 5.0)
     */
    @Value("${recommendation.min-rating-threshold:0.0}")
    private Double minRatingThreshold;
}
