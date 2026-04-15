package tn.esprit.spring.baladna.accommodation.dto;

import lombok.Builder;
import lombok.Data;
import tn.esprit.spring.baladna.accommodation.entity.enums.AccommodationStatus;
import tn.esprit.spring.baladna.accommodation.entity.enums.AccommodationType;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class AccommodationResponseDto {
    private String id;
    private String title;
    private String description;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Integer maxGuests;
    private String amenities;
    private String rules;
    private AccommodationType type;
    private AccommodationStatus status;
    private Long hostId;
    private String coverImageUrl;
    private List<RoomResponseDto> rooms;
    private BigDecimal fromPricePerNight;
}
