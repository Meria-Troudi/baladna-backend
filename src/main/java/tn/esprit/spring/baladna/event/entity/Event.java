package tn.esprit.spring.baladna.event.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import tn.esprit.spring.baladna.event.entity.enums.EventStatus;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private EventCategory category;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    @Column(length = 200)
    private String location;

    private Double latitude;
    private Double longitude;

    private Integer capacity;
    @Builder.Default
    private Integer bookedSeats = 0;

    @Builder.Default
    private Double price = 0.0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EventStatus status = EventStatus.UPCOMING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "event")
    @JsonManagedReference("event-reservation")
    @Builder.Default
    private List<EventReservation> reservations = new ArrayList<>();
}