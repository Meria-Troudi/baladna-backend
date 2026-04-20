package tn.esprit.spring.baladna.itinerary.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoogleCalendarAuthRequest {
    private String authorizationCode;
}
