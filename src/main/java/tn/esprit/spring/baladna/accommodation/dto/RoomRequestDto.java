package tn.esprit.spring.baladna.accommodation.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import tn.esprit.spring.baladna.accommodation.entity.enums.RoomType;

import java.math.BigDecimal;

@Data
public class RoomRequestDto {
    private String id;

    @NotNull
    private RoomType type;

    @NotNull
    @Min(1)
    private Integer capacity;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal pricePerNight;

    private String amenities;
}
