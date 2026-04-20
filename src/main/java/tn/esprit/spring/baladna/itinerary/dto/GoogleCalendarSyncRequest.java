package tn.esprit.spring.baladna.itinerary.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class GoogleCalendarSyncRequest {
    private UUID itineraryId;
}
