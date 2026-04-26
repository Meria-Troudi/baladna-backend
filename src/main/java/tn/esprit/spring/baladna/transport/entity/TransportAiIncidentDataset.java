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
        name = "transport_ai_incident_dataset",
        indexes = {
                @Index(name = "idx_ai_incident_transport", columnList = "transport_id"),
                @Index(name = "idx_ai_incident_host", columnList = "host_id"),
                @Index(name = "idx_ai_incident_type", columnList = "incident_type"),
                @Index(name = "idx_ai_incident_severity", columnList = "incident_severity")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportAiIncidentDataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_id", nullable = false)
    private Transport transport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trajet_id")
    private Trajet trajet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private User host;

    @Column(name = "incident_type", nullable = false, length = 40)
    private String incidentType;

    @Column(name = "incident_severity", nullable = false, length = 20)
    private String incidentSeverity;

    @Column(name = "incident_source", length = 40)
    private String incidentSource;

    @Column(name = "incident_title", length = 180)
    private String incidentTitle;

    @Column(name = "incident_description", columnDefinition = "TEXT")
    private String incidentDescription;

    @Column(name = "weather", length = 30)
    private String weather;

    @Builder.Default
    @Column(name = "traffic_jam", nullable = false)
    private Boolean trafficJam = false;

    @Builder.Default
    @Column(name = "predicted_delay_minutes")
    private Integer predictedDelayMinutes = 0;

    @Builder.Default
    @Column(name = "actual_delay_minutes")
    private Integer actualDelayMinutes = 0;

    @Column(name = "action_taken", length = 50)
    private String actionTaken;

    @Column(name = "action_result", length = 50)
    private String actionResult;

    @Builder.Default
    @Column(name = "affected_passengers_count")
    private Integer affectedPassengersCount = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (trafficJam == null) {
            trafficJam = false;
        }
        if (predictedDelayMinutes == null) {
            predictedDelayMinutes = 0;
        }
        if (actualDelayMinutes == null) {
            actualDelayMinutes = 0;
        }
        if (affectedPassengersCount == null) {
            affectedPassengersCount = 0;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
