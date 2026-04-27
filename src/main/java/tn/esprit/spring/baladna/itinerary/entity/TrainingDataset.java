package tn.esprit.spring.baladna.itinerary.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity to store training data extracted from existing itineraries
 * Used to train the AI recommendation model
 */
@Entity
@Table(name = "training_dataset")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingDataset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    // Reference to the original itinerary (nullable for synthetic training data)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = true)
    private Itinerary itinerary;

    // Features for ML model
    @Column(name = "budget", nullable = false, precision = 10, scale = 2)
    private BigDecimal budget;

    @Column(name = "location", nullable = false, length = 100)
    private String location;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "avg_daily_cost", precision = 10, scale = 2)
    private BigDecimal avgDailyCost;

    @Column(name = "num_steps", nullable = false)
    private Integer numSteps;

    @Column(name = "num_collaborators", nullable = false)
    private Integer numCollaborators;

    @Column(name = "num_expenses", nullable = false)
    private Integer numExpenses;

    // Rating/score derived from the itinerary
    @Column(name = "rating", precision = 3, scale = 2)
    private BigDecimal rating; // 0.0 to 5.0

    // Normalized features for ML (0.0 to 1.0)
    @Column(name = "normalized_budget")
    private BigDecimal normalizedBudget;

    @Column(name = "normalized_duration")
    private BigDecimal normalizedDuration;

    @Column(name = "normalized_complexity")
    private BigDecimal normalizedComplexity;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
