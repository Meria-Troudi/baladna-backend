package tn.esprit.spring.baladna.transport.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequestDTO {

    @NotNull(message = "Le transport est obligatoire")
    @Positive(message = "L'ID du transport doit être positif")
    private Long transportId;

    @NotBlank(message = "Le point d'embarquement est obligatoire")
    @Size(min = 2, max = 150, message = "Le point d'embarquement doit contenir entre 2 et 150 caractères")
    private String boardingPoint;

    @NotNull(message = "Le nombre de places est obligatoire")
    @Positive(message = "Le nombre de places doit être positif")
    @Min(value = 1, message = "Il faut réserver au moins 1 place")
    @Max(value = 20, message = "Maximum 20 places")
    private Integer seatsCount;
}