package tn.esprit.spring.baladna.event.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import org.hibernate.annotations.CreationTimestamp;
import tn.esprit.spring.baladna.event.entity.enums.ReservationStatus;
import tn.esprit.spring.baladna.event.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id")
    @JsonBackReference("event-reservation")
    private Event event;

    @Column(name = "tourist_user_id")
    private Long touristUserId;

    private Integer personsCount;

    private Double totalPrice;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    @Column(columnDefinition = "TEXT")
    private String qrCode;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime cancelledAt;
}