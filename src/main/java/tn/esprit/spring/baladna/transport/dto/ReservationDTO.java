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
public class ReservationDTO {

    private Long id;
    private Integer reservedSeats;
    private Double totalPrice;
    private Double pricePerSeat;
    private LocalDateTime reservationDate;
    private String boardingPoint;
    private ReservationStatus status;

    private Long transportId;
    private String transportDeparturePoint;
    private String transportRoute;

    private Long userId;
    private String userFullName;
    private String userEmail;
}