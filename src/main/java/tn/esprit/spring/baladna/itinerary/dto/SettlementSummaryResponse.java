package tn.esprit.spring.baladna.itinerary.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SettlementSummaryResponse {

    private Long userId;
    private BigDecimal totalPaid;
    private BigDecimal equalShare;
    private BigDecimal netBalance;
    private LocalDateTime computedAt;

    // Positive netBalance → this user is owed money by others
    // Negative netBalance → this user owes money to others
    private List<SettlementResponse> owes;     // debts this user must pay
    private List<SettlementResponse> owedBy;   // debts others must pay this user
}