package tn.esprit.spring.baladna.itinerary.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.spring.baladna.itinerary.entity.enums.CollaboratorRole;
import tn.esprit.spring.baladna.itinerary.entity.enums.CollaboratorStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "itinerary_collaborator",
        uniqueConstraints = @UniqueConstraint(columnNames = {"itinerary_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryCollaborator {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

    // References User.id from the user module (Long)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private CollaboratorRole role = CollaboratorRole.VIEWER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CollaboratorStatus status = CollaboratorStatus.PENDING;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
    }
}
