package tn.esprit.spring.baladna.accommodation.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class TouristReservationDto {
    private UUID reservationId;
    private UUID bookingGroupId;
    private UUID accommodationId;
    private String accommodationTitle;
    private String coverImageUrl;
    private LocalDate checkIn;
    private LocalDate checkOut;
    private String status;
    private String paymentStatus;
    private String invoiceNumber;
    private String confirmationCode;
    private BigDecimal priceTotal;
    private String roomSummary;
    private boolean canSubmitReview;
    private Integer existingReviewStars;
    private String existingReviewComment;
}
