package tn.esprit.spring.baladna.accommodation.dto;

import lombok.Builder;
import lombok.Data;
import tn.esprit.spring.baladna.accommodation.entity.enums.RoomType;

import java.math.BigDecimal;

@Data
@Builder
public class RoomResponseDto {
    private String id;
    private RoomType type;
    private Integer capacity;
    private BigDecimal pricePerNight;
    private String amenities;
}
