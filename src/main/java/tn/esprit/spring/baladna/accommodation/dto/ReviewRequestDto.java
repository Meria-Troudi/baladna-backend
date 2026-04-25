package tn.esprit.spring.baladna.accommodation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ReviewRequestDto {
    @NotNull
    private UUID reservationId;
    @NotNull
    @Min(1)
    @Max(5)
    private Integer stars;
    private String comment;
}
