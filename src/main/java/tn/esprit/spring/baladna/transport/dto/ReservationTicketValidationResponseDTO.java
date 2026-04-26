package tn.esprit.spring.baladna.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.spring.baladna.transport.entity.ReservationStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationTicketValidationResponseDTO {

    private boolean valid;
    private String message;
    private Long reservationId;
    private Long transportId;
    private String ticketCode;
    private String passengerName;
    private String passengerEmail;
    private String transportRoute;
    private String boardingPoint;
    private Integer reservedSeats;
    private Double totalPrice;
    private LocalDateTime reservationDate;
    private ReservationStatus status;
}
