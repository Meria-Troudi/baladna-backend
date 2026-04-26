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
        name = "transport_ai_trip_dataset",
        indexes = {
                @Index(name = "idx_ai_trip_transport", columnList = "transport_id"),
                @Index(name = "idx_ai_trip_departure_date", columnList = "departure_date"),
                @Index(name = "idx_ai_trip_route", columnList = "departure_city,arrival_city"),
                @Index(name = "idx_ai_trip_host", columnList = "host_id"),
                @Index(name = "idx_ai_trip_host_email", columnList = "host_email"),
                @Index(name = "idx_ai_trip_data_origin", columnList = "data_origin")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportAiTripDataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FIXED: ManyToOne nullable — bootstrap rows have no real transport
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_id", nullable = true)
    private Transport transport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trajet_id")
    private Trajet trajet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private User host;

    // Stored separately — queries by email work without joining user table
    @Column(name = "host_email", length = 200)
    private String hostEmail;

    @Builder.Default
    @Column(name = "data_origin", nullable = false, length = 30)
    private String dataOrigin = "REAL";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "departure_station_id")
    private Station departureStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "arrival_station_id")
    private Station arrivalStation;

    @Column(name = "departure_city", length = 120)
    private String departureCity;

    @Column(name = "arrival_city", length = 120)
    private String arrivalCity;

    @Column(name = "departure_point", length = 150)
    private String departurePoint;

    @Column(name = "departure_date", nullable = false)
    private LocalDateTime departureDate;

    @Column(name = "departure_day_of_week", length = 20)
    private String departureDayOfWeek;

    @Column(name = "departure_hour")
    private Integer departureHour;

    @Builder.Default
    @Column(name = "is_weekend", nullable = false)
    private Boolean isWeekend = false;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "transport_status", length = 30)
    private String transportStatus;

    @Column(name = "weather", length = 30)
    private String weather;

    @Column(name = "weather_temperature")
    private Double weatherTemperature;

    @Column(name = "weather_wind_speed")
    private Double weatherWindSpeed;

    @Column(name = "weather_precipitation")
    private Double weatherPrecipitation;

    @Builder.Default
    @Column(name = "traffic_jam", nullable = false)
    private Boolean trafficJam = false;

    @Column(name = "total_capacity")
    private Integer totalCapacity;

    @Column(name = "available_seats")
    private Integer availableSeats;

    @Column(name = "booked_seats")
    private Integer bookedSeats;

    @Column(name = "occupancy_rate")
    private Double occupancyRate;

    @Column(name = "base_price")
    private Double basePrice;

    @Builder.Default
    @Column(name = "rule_based_delay_minutes")
    private Integer ruleBasedDelayMinutes = 0;

    @Column(name = "actual_departure_date")
    private LocalDateTime actualDepartureDate;

    @Column(name = "actual_arrival_date")
    private LocalDateTime actualArrivalDate;

    @Column(name = "actual_delay_minutes")
    private Integer actualDelayMinutes;

    @Builder.Default
    @Column(name = "was_cancelled", nullable = false)
    private Boolean wasCancelled = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (isWeekend == null) isWeekend = false;
        if (trafficJam == null) trafficJam = false;
        if (wasCancelled == null) wasCancelled = false;
        if (ruleBasedDelayMinutes == null) ruleBasedDelayMinutes = 0;
        if (dataOrigin == null || dataOrigin.isBlank()) dataOrigin = "REAL";
        if ((hostEmail == null || hostEmail.isBlank()) && host != null && host.getEmail() != null) {
            hostEmail = host.getEmail();
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (dataOrigin == null || dataOrigin.isBlank()) dataOrigin = "REAL";
        if ((hostEmail == null || hostEmail.isBlank()) && host != null && host.getEmail() != null) {
            hostEmail = host.getEmail();
        }
    }
}