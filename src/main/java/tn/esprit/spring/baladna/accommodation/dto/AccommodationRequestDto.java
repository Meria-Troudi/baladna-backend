package tn.esprit.spring.baladna.accommodation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import tn.esprit.spring.baladna.accommodation.entity.enums.AccommodationStatus;
import tn.esprit.spring.baladna.accommodation.entity.enums.AccommodationType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class AccommodationRequestDto {

    @NotBlank
    @Size(max = 200)
    private String title;

    private String description;

    @NotBlank
    @Size(max = 500)
    private String address;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @NotNull
    @Min(1)
    private Integer maxGuests;

    private String amenities;
    private String rules;

    @NotNull
    private AccommodationType type;

    private AccommodationStatus status;

    @Valid
    @NotEmpty
    private List<RoomRequestDto> rooms = new ArrayList<>();
}
