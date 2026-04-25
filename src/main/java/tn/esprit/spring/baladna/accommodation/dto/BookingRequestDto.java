package tn.esprit.spring.baladna.accommodation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class BookingRequestDto {
    @NotNull
    private UUID accommodationId;
    @NotNull
    private LocalDate checkIn;
    @NotNull
    private LocalDate checkOut;
    @NotEmpty
    private List<UUID> roomIds;
    @NotNull
    @Min(1)
    private Integer guests;
}
