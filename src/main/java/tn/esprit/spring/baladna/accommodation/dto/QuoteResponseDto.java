package tn.esprit.spring.baladna.accommodation.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class QuoteResponseDto {
    private int nights;
    private BigDecimal subtotalBeforeAdjustments;
    private BigDecimal lastRoomPremiumPercent;
    private BigDecimal fullPropertyDiscountPercent;
    private BigDecimal total;
    private List<String> appliedRules;
}
