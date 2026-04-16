package tn.esprit.spring.baladna.itinerary.entity;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.spring.baladna.itinerary.entity.enums.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

    // User who owes money
    @Column(name = "debtor_user_id", nullable = false)
    private Long debtorUserId;

    // User who receives money
    @Column(name = "creditor_user_id", nullable = false)
    private Long creditorUserId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;
}