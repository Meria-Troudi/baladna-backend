package tn.esprit.spring.baladna.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventReservationDTO {
    private Long id;
    private Long eventId;
    private Long userId;
    private Integer personsCount;
    private Double totalPrice;
    private String status;
    private String paymentStatus;
}
