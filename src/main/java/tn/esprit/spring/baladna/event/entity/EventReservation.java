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

    // Returns the QR code image in base64 format (for compatibility)
    public String getQrCode() {
        return qrCodeImageBase64;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id")
    private Event event;

@Column(name = "user_id")
private Long userId;

public Long getUserId() {
    return userId;
}

public void setUserId(Long userId) {
    this.userId = userId;
}

    private Integer personsCount;

    private Double totalPrice;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    // QR token for validation (signed string)
    @Column(name = "qr_token", columnDefinition = "TEXT")
    private String qrToken;

    // QR code image for frontend display (base64 PNG)
    @Column(name = "qr_code_image_base64", columnDefinition = "TEXT")
    private String qrCodeImageBase64;


    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    private LocalDateTime cancelledAt;
}
