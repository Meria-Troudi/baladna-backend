package tn.esprit.spring.baladna.itinerary.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class GoogleCalendarAuthResponse {
    private Long userId;
    private Boolean isSynced;
    private String calendarId;
    private LocalDateTime tokenExpiry;
    private String message;
}
