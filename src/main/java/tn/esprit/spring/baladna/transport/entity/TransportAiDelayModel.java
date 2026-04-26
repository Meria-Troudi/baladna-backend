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
        name = "transport_ai_delay_models",
        indexes = {
                @Index(name = "idx_ai_delay_model_host", columnList = "host_id"),
                @Index(name = "idx_ai_delay_model_active", columnList = "active")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportAiDelayModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Builder.Default
    @Column(name = "sample_count", nullable = false)
    private Integer sampleCount = 0;

    @Builder.Default
    @Column(name = "intercept_weight", nullable = false)
    private Double interceptWeight = 0.0;

    @Builder.Default
    @Column(name = "rain_weight", nullable = false)
    private Double rainWeight = 0.0;

    @Builder.Default
    @Column(name = "sandstorm_weight", nullable = false)
    private Double sandstormWeight = 0.0;

    @Builder.Default
    @Column(name = "storm_weight", nullable = false)
    private Double stormWeight = 0.0;

    @Builder.Default
    @Column(name = "traffic_jam_weight", nullable = false)
    private Double trafficJamWeight = 0.0;

    @Builder.Default
    @Column(name = "rush_hour_weight", nullable = false)
    private Double rushHourWeight = 0.0;

    @Builder.Default
    @Column(name = "peak_weekday_weight", nullable = false)
    private Double peakWeekdayWeight = 0.0;

    @Builder.Default
    @Column(name = "long_route_weight", nullable = false)
    private Double longRouteWeight = 0.0;

    @Builder.Default
    @Column(name = "high_occupancy_weight", nullable = false)
    private Double highOccupancyWeight = 0.0;

    @Builder.Default
    @Column(name = "strong_wind_weight", nullable = false)
    private Double strongWindWeight = 0.0;

    @Builder.Default
    @Column(name = "heavy_precipitation_weight", nullable = false)
    private Double heavyPrecipitationWeight = 0.0;

    @Column(name = "mean_absolute_error")
    private Double meanAbsoluteError;

    @Column(name = "root_mean_squared_error")
    private Double rootMeanSquaredError;

    @Column(name = "trained_at", nullable = false)
    private LocalDateTime trainedAt;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "training_data_mode", length = 30)
    private String trainingDataMode;

    @Column(name = "real_sample_count")
    private Integer realSampleCount;

    @Column(name = "bootstrap_sample_count")
    private Integer bootstrapSampleCount;

    @Column(name = "dataset_fingerprint", length = 160)
    private String datasetFingerprint;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (trainedAt == null) {
            trainedAt = now;
        }
        if (active == null) {
            active = true;
        }
        if (sampleCount == null) {
            sampleCount = 0;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
