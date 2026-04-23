package tn.esprit.spring.baladna.itinerary.dto;

import lombok.Builder;
import lombok.Data;
import tn.esprit.spring.baladna.itinerary.entity.enums.SettlementStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SettlementResponse {

    private UUID id;
    private Long debtorUserId;
    private Long creditorUserId;
    private BigDecimal amount;
    private SettlementStatus status;
    private LocalDateTime settledAt;
}