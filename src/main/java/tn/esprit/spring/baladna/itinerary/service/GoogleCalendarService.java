package tn.esprit.spring.baladna.itinerary.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import tn.esprit.spring.baladna.itinerary.dto.GoogleCalendarAuthResponse;
import tn.esprit.spring.baladna.itinerary.entity.GoogleCalendarCredential;
import tn.esprit.spring.baladna.itinerary.entity.Itinerary;
import tn.esprit.spring.baladna.itinerary.entity.ItineraryStep;
import tn.esprit.spring.baladna.itinerary.repository.GoogleCalendarCredentialRepository;
import tn.esprit.spring.baladna.itinerary.repository.ItineraryRepository;
import tn.esprit.spring.baladna.itinerary.util.GoogleCalendarConfig;
import tn.esprit.spring.baladna.itinerary.util.GoogleTokenResponse;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing Google Calendar integration using REST API
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private final GoogleCalendarCredentialRepository credentialRepository;
    private final ItineraryRepository itineraryRepository;
    private final GoogleCalendarConfig calendarConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Handle OAuth callback and store credentials
     */
    @Transactional
    public GoogleCalendarAuthResponse handleOAuthCallback(Long userId, String authorizationCode) 
            throws IOException {
        try {
            // Exchange code for token
            GoogleTokenResponse tokenResponse = calendarConfig.exchangeCodeForToken(authorizationCode);
            
            Long expirationSeconds = tokenResponse.getExpiresIn() != null ? tokenResponse.getExpiresIn() : 3600L;
            LocalDateTime tokenExpiry = LocalDateTime.now().plusSeconds(expirationSeconds);
            
            // Save or update credentials
            GoogleCalendarCredential credential = credentialRepository.findByUserId(userId)
                    .orElse(new GoogleCalendarCredential());
            
            credential.setUserId(userId);
            credential.setAccessToken(tokenResponse.getAccessToken());
            credential.setRefreshToken(tokenResponse.getRefreshToken());
            credential.setTokenExpiry(tokenExpiry);
            credential.setCalendarId("primary");
            credential.setIsSynced(true);
            
            credentialRepository.save(credential);
            
            log.info("Google Calendar credentials saved for user: {}", userId);
            
            return GoogleCalendarAuthResponse.builder()
                    .userId(userId)
                    .isSynced(true)
                    .calendarId("primary")
                    .tokenExpiry(tokenExpiry)
                    .message("Google Calendar connected successfully!")
                    .build();
            
        } catch (IOException e) {
            log.error("Error handling OAuth callback for user {}: {}", userId, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during Google Calendar OAuth callback for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to initiate Google Calendar connection: " + e.getMessage(), e);
        }
    }

    /**
     * Sync itinerary steps to Google Calendar
     */
    @Transactional(readOnly = true)
    public GoogleCalendarAuthResponse syncItineraryToGoogleCalendar(Long userId, UUID itineraryId) 
            throws IOException {
        // Check if user has Google Calendar credentials
        GoogleCalendarCredential credential = credentialRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("User has not connected Google Calendar. Please authenticate first."));
        
        // Check token expiry
        if (credential.getTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Google Calendar token expired. Please reconnect.");
        }
        
        // Get itinerary and steps
        Itinerary itinerary = itineraryRepository.findById(itineraryId)
                .orElseThrow(() -> new RuntimeException("Itinerary not found"));
        
        int syncedCount = 0;
        try {
            for (ItineraryStep step : itinerary.getSteps()) {
                if (step.getPlannedDate() != null) {
                    Map<String, Object> event = createGoogleCalendarEvent(step);
                    insertEventToGoogleCalendar(credential.getAccessToken(), credential.getCalendarId(), event);
                    syncedCount++;
                    log.info("Synced step: {} to Google Calendar", step.getTitle());
                }
            }
            
            log.info("Successfully synced {} steps for itinerary: {}", syncedCount, itineraryId);
            
            return GoogleCalendarAuthResponse.builder()
                    .userId(userId)
                    .isSynced(true)
                    .calendarId(credential.getCalendarId())
                    .message("Successfully synced " + syncedCount + " steps to Google Calendar!")
                    .build();
            
        } catch (Exception e) {
            log.error("Error syncing itinerary to Google Calendar: {}", e.getMessage());
            throw new RuntimeException("Failed to sync itinerary to Google Calendar", e);
        }
    }

    /**
     * Insert event to Google Calendar via REST API
     */
    private void insertEventToGoogleCalendar(String accessToken, String calendarId, Map<String, Object> event) {
        String url = String.format("%s/calendars/%s/events", GoogleCalendarConfig.getCalendarApiUrl(), calendarId);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        
        HttpEntity<String> request = null;
        try {
            request = new HttpEntity<>(objectMapper.writeValueAsString(event), headers);
            restTemplate.postForObject(url, request, String.class);
        } catch (Exception e) {
            log.error("Error inserting event to Google Calendar: {}", e.getMessage());
            throw new RuntimeException("Failed to insert event to Google Calendar", e);
        }
    }

    /**
     * Convert ItineraryStep to Google Calendar Event
     */
    private Map<String, Object> createGoogleCalendarEvent(ItineraryStep step) {
        Map<String, Object> event = new HashMap<>();
        event.put("summary", step.getTitle());
        event.put("description", step.getNotes() != null ? step.getNotes() : "");
        
        // Set location if coordinates are available
        if (step.getLatitude() != null && step.getLongitude() != null) {
            event.put("location", step.getLatitude() + "," + step.getLongitude());
        }
        
        // Set time
        if (step.getPlannedDate() != null) {
            Map<String, Object> start = new HashMap<>();
            Map<String, Object> end = new HashMap<>();
            
            String startTime = formatDateTime(step.getPlannedDate());
            String endTime = formatDateTime(step.getPlannedDate().plusHours(1));
            
            start.put("dateTime", startTime);
            start.put("timeZone", "UTC");
            end.put("dateTime", endTime);
            end.put("timeZone", "UTC");
            
            event.put("start", start);
            event.put("end", end);
        }
        
        return event;
    }

    /**
     * Format LocalDateTime to ISO 8601 format for Google Calendar API
     * Example: 2024-04-20T14:30:00
     */
    private String formatDateTime(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return dateTime.format(formatter);
    }

    /**
     * Check if user has Google Calendar connected
     */
    public boolean isGoogleCalendarConnected(Long userId) {
        return credentialRepository.existsByUserId(userId);
    }

    /**
     * Disconnect Google Calendar
     */
    @Transactional
    public void disconnectGoogleCalendar(Long userId) {
        log.info("Attempting to disconnect Google Calendar for user: {}", userId);
        try {
            boolean exists = credentialRepository.existsByUserId(userId);
            log.info("Credential exists for user {}: {}", userId, exists);
            
            if (!exists) {
                log.warn("No Google Calendar credential found for user: {}", userId);
            }
            
            credentialRepository.deleteByUserId(userId);
            log.info("Successfully deleted Google Calendar credential for user: {}", userId);
        } catch (Exception e) {
            log.error("Error deleting Google Calendar credential for user {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to disconnect Google Calendar: " + e.getMessage(), e);
        }
    }

    /**
     * Get Google Calendar authorization URL with user ID in state parameter
     */
    public String getAuthorizationUrl(Long userId) throws UnsupportedEncodingException {
        return calendarConfig.getAuthorizationUrl(userId);
    }
}
