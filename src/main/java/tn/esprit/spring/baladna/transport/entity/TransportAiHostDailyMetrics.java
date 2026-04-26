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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.spring.baladna.user.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "transport_ai_host_daily_metrics",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ai_host_daily_metric", columnNames = {"host_id", "metric_date"})
        },
        indexes = {
                @Index(name = "idx_ai_host_metric_host", columnList = "host_id"),
                @Index(name = "idx_ai_host_metric_date", columnList = "metric_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportAiHostDailyMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(name = "metric_date", nullable = false)
    private LocalDate metricDate;

    @Builder.Default
    @Column(name = "total_transports")
    private Integer totalTransports = 0;

    @Builder.Default
    @Column(name = "upcoming_transports")
    private Integer upcomingTransports = 0;

    @Builder.Default
    @Column(name = "completed_transports")
    private Integer completedTransports = 0;

    @Builder.Default
    @Column(name = "cancelled_transports")
    private Integer cancelledTransports = 0;

    @Builder.Default
    @Column(name = "total_bookings")
    private Integer totalBookings = 0;

    @Builder.Default
    @Column(name = "confirmed_bookings")
    private Integer confirmedBookings = 0;

    @Builder.Default
    @Column(name = "cancelled_bookings")
    private Integer cancelledBookings = 0;

    @Builder.Default
    @Column(name = "total_revenue")
    private Double totalRevenue = 0.0;

    @Builder.Default
    @Column(name = "average_occupancy_rate")
    private Double averageOccupancyRate = 0.0;

    @Builder.Default
    @Column(name = "average_delay_minutes")
    private Double averageDelayMinutes = 0.0;

    @Builder.Default
    @Column(name = "at_risk_transports_count")
    private Integer atRiskTransportsCount = 0;

    @Builder.Default
    @Column(name = "low_occupancy_transports_count")
    private Integer lowOccupancyTransportsCount = 0;

    @Column(name = "top_route_label", length = 180)
    private String topRouteLabel;

    @Builder.Default
    @Column(name = "top_route_booking_count")
    private Integer topRouteBookingCount = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (metricDate == null) {
            metricDate = LocalDate.now();
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
