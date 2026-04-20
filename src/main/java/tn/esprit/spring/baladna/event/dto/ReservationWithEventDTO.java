package tn.esprit.spring.baladna.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.spring.baladna.event.entity.Event;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationWithEventDTO {
    private Long id;
    private Event event;
    private Long touristUserId;
    private Integer personsCount;
    private Double totalPrice;
    private String status;
    private String paymentStatus;
    private String qrCode;
    private String createdAt;
    private String cancelledAt;
}
