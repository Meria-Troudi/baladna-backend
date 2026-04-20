package tn.esprit.spring.baladna.itinerary.dto;

import lombok.Builder;
import lombok.Data;
import tn.esprit.spring.baladna.itinerary.entity.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ExpenseResponse {

    private UUID id;
    private UUID itineraryId;
    private Long paidByUserId;
    private ExpenseCategory category;
    private BigDecimal amount;
    private String description;
    private LocalDateTime expenseDate;
    private LocalDateTime createdAt;
}