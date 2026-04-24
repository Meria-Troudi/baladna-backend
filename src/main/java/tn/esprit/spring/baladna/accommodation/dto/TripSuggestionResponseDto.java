package tn.esprit.spring.baladna.accommodation.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TripSuggestionResponseDto {
    /** Ranked stays (only from existing listings). */
    private List<AccommodationResponseDto> accommodations;
    /** Short explanation for the traveler. */
    private String note;
    /** "ollama" if local AI was used, "keyword" for fallback matching. */
    private String mode;
}
