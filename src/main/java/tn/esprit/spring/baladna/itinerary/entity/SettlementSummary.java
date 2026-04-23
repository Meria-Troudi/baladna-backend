package tn.esprit.spring.baladna.itinerary.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlement_summary",
        uniqueConstraints = @UniqueConstraint(columnNames = {"itinerary_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_id", nullable = false)
    private Itinerary itinerary;

    // References User.id from the user module
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Total this user paid across all expenses
    @Column(name = "total_paid", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalPaid = BigDecimal.ZERO;

    // totalExpenses / number of active collaborators
    @Column(name = "equal_share", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal equalShare = BigDecimal.ZERO;

    // totalPaid - equalShare
    // positive → owed money by others
    // negative → owes money to others
    @Column(name = "net_balance", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal netBalance = BigDecimal.ZERO;

    @Column(name = "computed_at")
    private LocalDateTime computedAt;
}