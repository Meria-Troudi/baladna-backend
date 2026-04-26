package tn.esprit.spring.baladna.transport.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationTicketValidationRequestDTO {

    @NotBlank(message = "Le code ticket est obligatoire")
    private String ticketCode;
}
