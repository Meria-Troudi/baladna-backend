package tn.esprit.spring.baladna.itinerary.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for AI recommendation operations
 * Provides helper methods for feature extraction and calculation
 */
public class RecommendationUtils {

    /**
     * Calculate Euclidean distance between two points
     */
    public static double euclideanDistance(double[] point1, double[] point2) {
        double sumOfSquares = 0.0;
        for (int i = 0; i < point1.length; i++) {
            double diff = point1[i] - point2[i];
            sumOfSquares += diff * diff;
        }
        return Math.sqrt(sumOfSquares);
    }

    /**
     * Calculate Manhattan distance between two points
     */
    public static double manhattanDistance(double[] point1, double[] point2) {
        double sum = 0.0;
        for (int i = 0; i < point1.length; i++) {
            sum += Math.abs(point1[i] - point2[i]);
        }
        return sum;
    }

    /**
     * Normalize a value between 0 and 1
     */
    public static double normalize(double value, double min, double max) {
        if (max <= min) {
            return 0.0;
        }
        double normalized = (value - min) / (max - min);
        return Math.max(0.0, Math.min(1.0, normalized)); // Clamp to [0, 1]
    }

    /**
     * Denormalize a value from [0, 1] back to original range
     */
    public static double denormalize(double normalized, double min, double max) {
        return min + (normalized * (max - min));
    }

    /**
     * Calculate Z-score (standard score)
     */
    public static double calculateZScore(double value, double mean, double stdDev) {
        if (stdDev == 0.0) {
            return 0.0;
        }
        return (value - mean) / stdDev;
    }

    /**
     * Calculate standard deviation
     */
    public static double calculateStandardDeviation(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }

        double mean = values.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        double sumOfSquaredDiffs = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .sum();

        return Math.sqrt(sumOfSquaredDiffs / values.size());
    }

    /**
     * Calculate mean
     */
    public static double calculateMean(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        return values.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
    }

    /**
     * Calculate Pearson correlation coefficient
     */
    public static double calculatePearsonCorrelation(List<Double> x, List<Double> y) {
        if (x.size() != y.size() || x.isEmpty()) {
            return 0.0;
        }

        double meanX = calculateMean(x);
        double meanY = calculateMean(y);

        double numerator = 0.0;
        double sumXDiffSquared = 0.0;
        double sumYDiffSquared = 0.0;

        for (int i = 0; i < x.size(); i++) {
            double xDiff = x.get(i) - meanX;
            double yDiff = y.get(i) - meanY;

            numerator += xDiff * yDiff;
            sumXDiffSquared += xDiff * xDiff;
            sumYDiffSquared += yDiff * yDiff;
        }

        double denominator = Math.sqrt(sumXDiffSquared * sumYDiffSquared);
        if (denominator == 0.0) {
            return 0.0;
        }

        return numerator / denominator;
    }

    /**
     * Convert BigDecimal to double safely
     */
    public static double toDouble(BigDecimal value) {
        return value != null ? value.doubleValue() : 0.0;
    }

    /**
     * Convert double to BigDecimal with 2 decimal places
     */
    public static BigDecimal toBigDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Generate recommendation explanation based on similarity score
     */
    public static String generateSimilarityExplanation(double similarityScore) {
        if (similarityScore >= 0.9) {
            return "Excellent match! Very similar to your preferences.";
        } else if (similarityScore >= 0.8) {
            return "Great match! Highly similar to your criteria.";
        } else if (similarityScore >= 0.7) {
            return "Good match! Well suited to your preferences.";
        } else if (similarityScore >= 0.6) {
            return "Fair match! Reasonably similar to your requirements.";
        } else if (similarityScore >= 0.5) {
            return "Moderate match. May still interest you.";
        } else {
            return "Lower similarity but worth considering.";
        }
    }

    /**
     * Validate budget range
     */
    public static boolean isValidBudgetRange(BigDecimal minBudget, BigDecimal maxBudget) {
        if (minBudget == null || maxBudget == null) {
            return false;
        }
        return minBudget.compareTo(BigDecimal.ZERO) >= 0 &&
               maxBudget.compareTo(minBudget) >= 0;
    }

    /**
     * Validate duration range
     */
    public static boolean isValidDurationRange(Integer minDays, Integer maxDays) {
        if (minDays == null || maxDays == null) {
            return false;
        }
        return minDays > 0 && maxDays >= minDays;
    }

    /**
     * Create weight vector for multi-criteria scoring
     */
    public static double[] createWeightVector(
        double budgetWeight,
        double durationWeight,
        double complexityWeight) {

        double[] weights = {budgetWeight, durationWeight, complexityWeight};
        normalizeWeights(weights);
        return weights;
    }

    /**
     * Normalize weight vector to sum to 1.0
     */
    private static void normalizeWeights(double[] weights) {
        double sum = 0.0;
        for (double w : weights) {
            sum += w;
        }

        if (sum > 0) {
            for (int i = 0; i < weights.length; i++) {
                weights[i] /= sum;
            }
        }
    }

    /**
     * Calculate weighted score
     */
    public static double calculateWeightedScore(double[] scores, double[] weights) {
        if (scores.length != weights.length) {
            throw new IllegalArgumentException("Scores and weights must have same length");
        }

        double weightedScore = 0.0;
        for (int i = 0; i < scores.length; i++) {
            weightedScore += scores[i] * weights[i];
        }

        return Math.min(1.0, Math.max(0.0, weightedScore)); // Clamp to [0, 1]
    }
}
