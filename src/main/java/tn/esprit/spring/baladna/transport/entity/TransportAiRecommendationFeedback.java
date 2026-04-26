package tn.esprit.spring.baladna.transport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.spring.baladna.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "transport_ai_recommendation_feedback",
        indexes = {
                @Index(name = "idx_ai_feedback_user", columnList = "user_id"),
                @Index(name = "idx_ai_feedback_transport", columnList = "recommended_transport_id"),
                @Index(name = "idx_ai_feedback_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportAiRecommendationFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_transport_id", nullable = false)
    private Transport recommendedTransport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id")
    private Reservation booking;

    @Column(name = "departure_query", length = 120)
    private String departureQuery;

    @Column(name = "arrival_query", length = 120)
    private String arrivalQuery;

    @Column(name = "recommended_score")
    private Integer recommendedScore;

    @Column(name = "predicted_delay_minutes")
    private Integer predictedDelayMinutes;

    @Column(name = "price_at_recommendation")
    private Double priceAtRecommendation;

    @Column(name = "weather_at_recommendation", length = 30)
    private String weatherAtRecommendation;

    @Column(name = "available_seats_at_recommendation")
    private Integer availableSeatsAtRecommendation;

    @Builder.Default
    @Column(name = "clicked", nullable = false)
    private Boolean clicked = false;

    @Builder.Default
    @Column(name = "booked", nullable = false)
    private Boolean booked = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (clicked == null) {
            clicked = false;
        }
        if (booked == null) {
            booked = false;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
