package tn.esprit.spring.baladna.accommodation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.UUID;

/**
 * One row per room per night — filled when a reservation is paid and confirmed.
 */
@Entity
@Table(name = "room_availability",
        uniqueConstraints = @UniqueConstraint(name = "uk_room_night", columnNames = {"room_id", "night"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomAvailability {

    @Id
    @Column(name = "availability_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID id;

    /**
     * Legacy column still present in some MySQL schemas (NOT NULL, no default). Kept equal to {@link #id}.
     */
    @Column(name = "slot_id", columnDefinition = "BINARY(16)", nullable = false, updatable = false)
    @JdbcTypeCode(SqlTypes.BINARY)
    private UUID slotId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private LocalDate night;

    /**
     * Legacy column name in some MySQL schemas; same value as {@link #night}.
     */
    @Column(name = "availability_date", nullable = false, updatable = false)
    private LocalDate availabilityDate;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private AccommodationReservation reservation;

    @PrePersist
    private void syncLegacyColumns() {
        if (slotId == null) {
            slotId = id;
        }
        if (availabilityDate == null && night != null) {
            availabilityDate = night;
        }
    }
}
