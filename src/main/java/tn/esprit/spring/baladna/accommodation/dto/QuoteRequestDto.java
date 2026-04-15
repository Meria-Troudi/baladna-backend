package tn.esprit.spring.baladna.accommodation.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class QuoteRequestDto {
    @NotNull
    private UUID accommodationId;

    @NotNull
    private LocalDate checkIn;

    @NotNull
    private LocalDate checkOut;

    @NotEmpty
    private List<UUID> roomIds;
}
