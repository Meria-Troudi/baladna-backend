package tn.esprit.spring.baladna.event.dto;

import lombok.Builder;
import lombok.Data;
import tn.esprit.spring.baladna.event.entity.enums.ReservationStatus;
import tn.esprit.spring.baladna.event.entity.enums.PaymentStatus;
import java.math.BigDecimal;

@Data
@Builder
public class ReservationDTO {
    private Long id;
    private Long eventId;
    private Integer personsCount;
    private BigDecimal totalPrice;
    private ReservationStatus status;
    private PaymentStatus paymentStatus;
}