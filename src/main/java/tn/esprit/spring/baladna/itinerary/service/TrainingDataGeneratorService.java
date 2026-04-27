package tn.esprit.spring.baladna.itinerary.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.baladna.itinerary.entity.Itinerary;
import tn.esprit.spring.baladna.itinerary.entity.TrainingDataset;
import tn.esprit.spring.baladna.itinerary.repository.ItineraryRepository;
import tn.esprit.spring.baladna.itinerary.repository.TrainingDatasetRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service to generate training datasets from existing itineraries
 * Extracts features and creates normalized data for the AI model
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TrainingDataGeneratorService {

    private final TrainingDatasetRepository trainingDatasetRepository;
    private final ItineraryRepository itineraryRepository;

    /**
     * Generate training data from all published itineraries
     */
    @Transactional
    public int generateTrainingDataFromAllItineraries() {
        log.info("Starting training data generation from all itineraries");

        List<Itinerary> itineraries = itineraryRepository.findAll();
        int generated = 0;

        for (Itinerary itinerary : itineraries) {
            if (generateTrainingDataForItinerary(itinerary)) {
                generated++;
            }
        }

        // Normalize all generated data
        normalizeAllTrainingData();

        log.info("Generated training data for {} itineraries", generated);
        return generated;
    }

    /**
     * Generate training data for a specific itinerary
     */
    @Transactional
    public boolean generateTrainingDataForItinerary(UUID itineraryId) {
        Optional<Itinerary> itineraryOpt = itineraryRepository.findById(itineraryId);
        if (itineraryOpt.isEmpty()) {
            log.warn("Itinerary not found: {}", itineraryId);
            return false;
        }
        return generateTrainingDataForItinerary(itineraryOpt.get());
    }

    /**
     * Internal method to generate training data for a single itinerary
     */
    @Transactional
    protected boolean generateTrainingDataForItinerary(Itinerary itinerary) {
        try {
            // Check if training data already exists for this itinerary
            Optional<TrainingDataset> existing = trainingDatasetRepository.findAll()
                .stream()
                .filter(t -> t.getItinerary().getId().equals(itinerary.getId()))
                .findFirst();

            TrainingDataset dataset = existing.orElseGet(TrainingDataset::new);

            // Extract features
            dataset.setItinerary(itinerary);
            dataset.setBudget(itinerary.getEstimatedBudget() != null ? 
                            itinerary.getEstimatedBudget() : 
                            BigDecimal.ZERO);
            dataset.setLocation(sanitizeLocation(itinerary.getDestinationRegion()));

            // Calculate duration in days
            long durationDays = ChronoUnit.DAYS.between(
                itinerary.getStartDate(),
                itinerary.getEndDate()
            );
            dataset.setDurationDays(Math.max((int) durationDays, 1));

            // Calculate average daily cost
            BigDecimal avgDailyCost = calculateAverageDailyCost(
                itinerary.getEstimatedBudget(),
                dataset.getDurationDays()
            );
            dataset.setAvgDailyCost(avgDailyCost);

            // Extract complexity features
            dataset.setNumSteps(itinerary.getSteps().size());
            dataset.setNumCollaborators(itinerary.getCollaborators().size());
            dataset.setNumExpenses(itinerary.getExpenses().size());

            // Calculate rating based on complexity and completeness
            BigDecimal rating = calculateRating(itinerary, dataset);
            dataset.setRating(rating);

            // Save the dataset
            trainingDatasetRepository.save(dataset);
            log.debug("Generated training data for itinerary: {}", itinerary.getId());
            return true;

        } catch (Exception e) {
            log.error("Error generating training data for itinerary: {}", 
                     itinerary.getId(), e);
            return false;
        }
    }

    /**
     * Normalize all training data for ML processing
     */
    @Transactional
    public void normalizeAllTrainingData() {
        log.info("Starting normalization of training data");

        List<TrainingDataset> allData = trainingDatasetRepository.findAll();
        if (allData.isEmpty()) {
            log.warn("No training data to normalize");
            return;
        }

        // Get min/max values for normalization
        BigDecimal maxBudget = trainingDatasetRepository.getMaxBudget();
        BigDecimal minBudget = trainingDatasetRepository.getMinBudget();
        Integer maxDuration = trainingDatasetRepository.getMaxDuration();
        Integer minDuration = trainingDatasetRepository.getMinDuration();

        BigDecimal budgetRange = maxBudget.subtract(minBudget);
        int durationRange = Math.max(maxDuration - minDuration, 1);

        // Get max complexity
        int maxComplexity = allData.stream()
            .mapToInt(d -> d.getNumSteps() + d.getNumCollaborators())
            .max()
            .orElse(1);

        // Normalize each dataset
        for (TrainingDataset dataset : allData) {
            // Normalize budget (0.0 to 1.0)
            if (budgetRange.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal normalizedBudget = dataset.getBudget()
                    .subtract(minBudget)
                    .divide(budgetRange, 4, RoundingMode.HALF_UP);
                dataset.setNormalizedBudget(normalizedBudget);
            } else {
                dataset.setNormalizedBudget(BigDecimal.ZERO);
            }

            // Normalize duration (0.0 to 1.0)
            BigDecimal normalizedDuration = BigDecimal.valueOf(
                (double) (dataset.getDurationDays() - minDuration) / durationRange
            ).setScale(4, RoundingMode.HALF_UP);
            dataset.setNormalizedDuration(normalizedDuration);

            // Normalize complexity (0.0 to 1.0)
            int complexity = dataset.getNumSteps() + dataset.getNumCollaborators();
            BigDecimal normalizedComplexity = BigDecimal.valueOf(
                (double) complexity / Math.max(maxComplexity, 1)
            ).setScale(4, RoundingMode.HALF_UP);
            dataset.setNormalizedComplexity(normalizedComplexity);

            trainingDatasetRepository.save(dataset);
        }

        log.info("Normalized {} training records", allData.size());
    }

    /**
     * Calculate average daily cost
     */
    private BigDecimal calculateAverageDailyCost(BigDecimal totalBudget, Integer days) {
        if (totalBudget == null || totalBudget.compareTo(BigDecimal.ZERO) <= 0 || 
            days == null || days <= 0) {
            return BigDecimal.ZERO;
        }

        return totalBudget.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calculate rating based on itinerary quality signals
     * Rating scale: 0.0 to 5.0
     */
    private BigDecimal calculateRating(Itinerary itinerary, TrainingDataset dataset) {
        double rating = 2.5; // Start with average

        // Increase rating based on completeness
        if (itinerary.getDescription() != null && !itinerary.getDescription().isEmpty()) {
            rating += 0.5;
        }

        // Reward having multiple steps
        if (dataset.getNumSteps() >= 5) {
            rating += 1.0;
        } else if (dataset.getNumSteps() >= 3) {
            rating += 0.5;
        }

        // Reward collaboration
        if (dataset.getNumCollaborators() > 1) {
            rating += 0.5;
        }

        // Reward having expenses tracked
        if (dataset.getNumExpenses() > 0) {
            rating += 0.5;
        }

        // Cap rating at 5.0
        rating = Math.min(rating, 5.0);

        return BigDecimal.valueOf(rating).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Generate synthetic training data for testing (when live data is limited)
     */
    @Transactional
    public int generateSyntheticTrainingData(int count) {
        log.info("Generating {} synthetic training records", count);

        String[] locations = {
            "Tunis", "Sousse", "Sfax", "Djerba", "Kairouan",
            "Tozeur", "Nefta", "Gafsa", "Bizerte", "Hammamet",
            "Nabeul", "Monastir", "Mahdia", "Tataouine", "Gafsa"
        };

        int generated = 0;
        for (int i = 0; i < count; i++) {
            try {
                String location = locations[i % locations.length];
                BigDecimal budget = generateRandomBudget();
                int days = generateRandomDays();

                TrainingDataset dataset = TrainingDataset.builder()
                    .budget(budget)
                    .location(location)
                    .durationDays(days)
                    .avgDailyCost(budget.divide(BigDecimal.valueOf(days), 2, RoundingMode.HALF_UP))
                    .numSteps((int) (Math.random() * 15) + 3)
                    .numCollaborators((int) (Math.random() * 5) + 1)
                    .numExpenses((int) (Math.random() * 20) + 5)
                    .rating(BigDecimal.valueOf(Math.random() * 3 + 2.0)
                                     .setScale(2, RoundingMode.HALF_UP))
                    .createdAt(LocalDateTime.now())
                    .build();

                trainingDatasetRepository.save(dataset);
                generated++;

            } catch (Exception e) {
                log.error("Error generating synthetic training data", e);
            }
        }

        // Normalize the new synthetic data
        normalizeAllTrainingData();

        log.info("Generated {} synthetic training records", generated);
        return generated;
    }

    /**
     * Export training data as CSV format string
     */
    public String exportTrainingDataAsCSV() {
        List<TrainingDataset> allData = trainingDatasetRepository.findAll();

        StringBuilder csv = new StringBuilder();
        csv.append("ItineraryID,Location,Budget,DurationDays,AvgDailyCost,NumSteps,")
           .append("NumCollaborators,NumExpenses,Rating,")
           .append("NormalizedBudget,NormalizedDuration,NormalizedComplexity\n");

        for (TrainingDataset dataset : allData) {
            if (dataset.getItinerary() != null) {
                csv.append(dataset.getItinerary().getId()).append(",")
                   .append(dataset.getLocation()).append(",")
                   .append(dataset.getBudget()).append(",")
                   .append(dataset.getDurationDays()).append(",")
                   .append(dataset.getAvgDailyCost()).append(",")
                   .append(dataset.getNumSteps()).append(",")
                   .append(dataset.getNumCollaborators()).append(",")
                   .append(dataset.getNumExpenses()).append(",")
                   .append(dataset.getRating()).append(",")
                   .append(dataset.getNormalizedBudget()).append(",")
                   .append(dataset.getNormalizedDuration()).append(",")
                   .append(dataset.getNormalizedComplexity()).append("\n");
            }
        }

        return csv.toString();
    }

    /**
     * Get training data statistics
     */
    public String getTrainingDataStatistics() {
        List<TrainingDataset> allData = trainingDatasetRepository.findAll();

        StringBuilder stats = new StringBuilder();
        stats.append("=== Training Data Statistics ===\n");
        stats.append("Total Records: ").append(allData.size()).append("\n");

        if (!allData.isEmpty()) {
            BigDecimal avgBudget = allData.stream()
                .map(TrainingDataset::getBudget)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(allData.size()), 2, RoundingMode.HALF_UP);

            Integer avgDuration = (int) allData.stream()
                .mapToInt(TrainingDataset::getDurationDays)
                .average()
                .orElse(0);

            Double avgRating = allData.stream()
                .filter(d -> d.getRating() != null)
                .mapToDouble(d -> d.getRating().doubleValue())
                .average()
                .orElse(0);

            stats.append("Average Budget: ").append(avgBudget).append("\n");
            stats.append("Average Duration: ").append(avgDuration).append(" days\n");
            stats.append("Average Rating: ").append(String.format("%.2f", avgRating)).append("\n");
            stats.append("Unique Locations: ").append(
                trainingDatasetRepository.findAllUniqueLocations().size()
            ).append("\n");
        }

        return stats.toString();
    }

    /**
     * Helper to sanitize location strings
     */
    private String sanitizeLocation(String location) {
        if (location == null || location.isEmpty()) {
            return "Unknown";
        }
        return location.trim();
    }

    /**
     * Generate random budget for synthetic data
     */
    private BigDecimal generateRandomBudget() {
        double budget = (Math.random() * 8000) + 500; // 500 to 8500
        return BigDecimal.valueOf(budget).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Generate random duration for synthetic data
     */
    private int generateRandomDays() {
        return (int) (Math.random() * 20) + 2; // 2 to 22 days
    }
}
