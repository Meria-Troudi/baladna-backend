package tn.esprit.spring.baladna.transport.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StationDTO {

    private Long id;

    @NotBlank(message = "Le nom de la station est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    private String name;

    @NotBlank(message = "La ville est obligatoire")
    @Size(min = 2, max = 100, message = "La ville doit contenir entre 2 et 100 caractères")
    private String city;

    @NotNull(message = "La surcharge est obligatoire")
    @DecimalMin(value = "0.0", message = "La surcharge doit être >= 0")
    @DecimalMax(value = "100.0", message = "La surcharge doit être <= 100")
    private Double surcharge;

    @NotNull(message = "Le champ downtown est obligatoire")
    private Boolean downtown;
}