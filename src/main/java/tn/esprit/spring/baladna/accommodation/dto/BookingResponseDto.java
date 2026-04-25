package tn.esprit.spring.baladna.accommodation.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class BookingResponseDto {
    private UUID bookingGroupId;
    private String invoiceNumber;
    private String confirmationCode;
    private BigDecimal totalAmount;
    private List<UUID> reservationIds;
    private boolean paymentPending;
    private String message;
}
