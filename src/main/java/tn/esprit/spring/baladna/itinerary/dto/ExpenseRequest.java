package tn.esprit.spring.baladna.itinerary.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import tn.esprit.spring.baladna.itinerary.entity.enums.ExpenseCategory;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ExpenseRequest {

    @NotNull(message = "Category is required")
    private ExpenseCategory category;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String description;

    @NotNull(message = "Expense date is required")
    private LocalDateTime expenseDate;
}