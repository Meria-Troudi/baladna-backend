package tn.esprit.spring.baladna.transport.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrajetDTO {

    private Long id;

    @NotNull(message = "L'ID de la station de départ est obligatoire")
    @Positive
    private Long departureStationId;

    private String departureStationName;

    @NotNull(message = "L'ID de la station d'arrivée est obligatoire")
    @Positive
    private Long arrivalStationId;

    private String arrivalStationName;

    @NotNull(message = "La distance est obligatoire")
    @DecimalMin("1.0")
    @DecimalMax("1000.0")
    private Double distanceKm;

    @NotNull(message = "La durée estimée est obligatoire")
    @Positive
    private Integer estimatedDurationMinutes;

    @NotNull(message = "Le prix par km est obligatoire")
    @DecimalMin("0.01")
    @DecimalMax("10.0")
    private Double pricePerKm;

    private Double basePrice;
}