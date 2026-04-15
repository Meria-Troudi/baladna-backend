package tn.esprit.spring.baladna.accommodation.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class HostReservationDto {
    private UUID reservationId;
    private UUID bookingGroupId;
    private UUID accommodationId;
    private String accommodationTitle;
    private String guestDisplayName;
    private String guestEmail;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private Integer guests;
    private String status;
    private String paymentStatus;
    private String invoiceNumber;
    private String confirmationCode;
    private BigDecimal priceTotal;
    private String roomSummary;
}
