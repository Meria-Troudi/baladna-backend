package tn.esprit.spring.baladna.itinerary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.itinerary.entity.TrainingDataset;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface TrainingDatasetRepository extends JpaRepository<TrainingDataset, UUID> {

    /**
     * Find training data by location
     */
    List<TrainingDataset> findByLocation(String location);

    /**
     * Find training data by budget range
     */
    List<TrainingDataset> findByBudgetBetween(BigDecimal minBudget, BigDecimal maxBudget);

    /**
     * Find training data by duration range
     */
    List<TrainingDataset> findByDurationDaysBetween(Integer minDays, Integer maxDays);

    /**
     * Find training data by location and budget range
     */
    @Query("SELECT t FROM TrainingDataset t WHERE t.location = :location " +
           "AND t.budget BETWEEN :minBudget AND :maxBudget")
    List<TrainingDataset> findByLocationAndBudgetRange(
        @Param("location") String location,
        @Param("minBudget") BigDecimal minBudget,
        @Param("maxBudget") BigDecimal maxBudget
    );

    /**
     * Find top rated training data
     */
    @Query("SELECT t FROM TrainingDataset t WHERE t.rating IS NOT NULL " +
           "ORDER BY t.rating DESC LIMIT :limit")
    List<TrainingDataset> findTopRatedByLimit(@Param("limit") int limit);

    /**
     * Find all unique locations in training dataset
     */
    @Query("SELECT DISTINCT t.location FROM TrainingDataset t ORDER BY t.location")
    List<String> findAllUniqueLocations();

    /**
     * Get statistics for normalization
     */
    @Query("SELECT COALESCE(MAX(t.budget), 0) FROM TrainingDataset t")
    BigDecimal getMaxBudget();

    @Query("SELECT COALESCE(MIN(t.budget), 0) FROM TrainingDataset t")
    BigDecimal getMinBudget();

    @Query("SELECT COALESCE(MAX(t.durationDays), 0) FROM TrainingDataset t")
    Integer getMaxDuration();

    @Query("SELECT COALESCE(MIN(t.durationDays), 1) FROM TrainingDataset t")
    Integer getMinDuration();

    /**
     * Get average rating by location
     */
    @Query("SELECT AVG(t.rating) FROM TrainingDataset t WHERE t.location = :location")
    BigDecimal getAverageRatingByLocation(@Param("location") String location);

    /**
     * Count training records by location
     */
    @Query("SELECT COUNT(t) FROM TrainingDataset t WHERE t.location = :location")
    Long countByLocation(@Param("location") String location);
}
