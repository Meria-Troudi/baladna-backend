package tn.esprit.spring.baladna.itinerary.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.itinerary.dto.GoogleCalendarAuthRequest;
import tn.esprit.spring.baladna.itinerary.dto.GoogleCalendarAuthResponse;
import tn.esprit.spring.baladna.itinerary.dto.GoogleCalendarSyncRequest;
import tn.esprit.spring.baladna.itinerary.service.GoogleCalendarService;
import tn.esprit.spring.baladna.itinerary.util.SecurityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Google Calendar Integration Endpoints
 * All endpoints require valid JWT in Authorization header
 */
@Slf4j
@RestController
@RequestMapping("/api/itineraries/calendar")
@RequiredArgsConstructor
public class GoogleCalendarController {

    private final GoogleCalendarService googleCalendarService;
    private final SecurityUtils securityUtils;

    /**
     * Get Google Calendar authorization URL
     * This endpoint returns the Google OAuth URL that the frontend should open
     */
    @GetMapping("/auth-url")
    public ResponseEntity<String> getAuthorizationUrl() throws UnsupportedEncodingException {
        Long userId = securityUtils.getCurrentUserId();
        log.info("Auth URL requested for user: {}", userId);
        
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }
        
        String authUrl = googleCalendarService.getAuthorizationUrl(userId);
        return ResponseEntity.ok(authUrl);
    }

    /**
     * OAuth callback endpoint - handles authorization code from Google OAuth redirect
     * Google redirects to: /api/itineraries/calendar/oauth/callback?code=xxx&state=userId
     * This endpoint:
     * 1. Accepts GET requests (from Google's OAuth redirect)
     * 2. Does NOT require JWT in the Authorization header (Google's redirect won't have it)
     * 3. Uses the 'state' parameter to identify which user should link this calendar
     * 4. Extracts the authorization code from URL parameters
     * 5. Exchanges code for tokens via backend
     */
    @GetMapping("/oauth/callback")
    public ResponseEntity<?> handleOAuthCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error) throws IOException {
        
        log.info("OAuth callback received. State: {}, Has code: {}, Error: {}", state, code != null, error);
        
        // Check for OAuth errors from Google
        if (error != null) {
            log.error("OAuth error from Google: {}", error);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Authorization failed: " + error);
            return ResponseEntity.badRequest().body(response);
        }
        
        // Validate code is present
        if (code == null || code.trim().isEmpty()) {
            log.error("OAuth callback received without authorization code");
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Missing authorization code from Google");
            return ResponseEntity.badRequest().body(response);
        }
        
        // Validate state parameter contains user ID
        if (state == null || state.trim().isEmpty()) {
            log.error("OAuth callback received without state parameter (user ID)");
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid OAuth state: missing user identification");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            // Extract user ID from state parameter
            Long userId = Long.parseLong(state);
            log.info("OAuth callback for user: {} with authorization code", userId);
            
            // Exchange code for tokens and save credentials
            GoogleCalendarAuthResponse callbackResponse = googleCalendarService.handleOAuthCallback(userId, code);
            
            log.info("Successfully processed OAuth callback for user: {}", userId);
            
            // Return success response
            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", callbackResponse.getMessage());
            successResponse.put("userId", callbackResponse.getUserId());
            successResponse.put("isSynced", callbackResponse.getIsSynced());
            successResponse.put("calendarId", callbackResponse.getCalendarId());
            successResponse.put("tokenExpiry", callbackResponse.getTokenExpiry());
            
            return ResponseEntity.ok(successResponse);
            
        } catch (NumberFormatException e) {
            log.error("Invalid state parameter (user ID): {}", state);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid OAuth state format");
            return ResponseEntity.badRequest().body(response);
        } catch (Exception e) {
            log.error("Error processing OAuth callback: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to process OAuth callback: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Sync itinerary steps to Google Calendar
     */
    @PostMapping("/sync")
    public ResponseEntity<GoogleCalendarAuthResponse> syncItinerary(
            @RequestBody GoogleCalendarSyncRequest request) throws IOException {
        Long userId = securityUtils.getCurrentUserId();
        log.info("Sync request for user: {}, itinerary: {}", userId, request.getItineraryId());
        GoogleCalendarAuthResponse response = googleCalendarService.syncItineraryToGoogleCalendar(
                userId, 
                request.getItineraryId()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Check if user has Google Calendar connected
     */
    @GetMapping("/status")
    public ResponseEntity<Boolean> checkCalendarStatus() {
        Long userId = securityUtils.getCurrentUserId();
        log.info("Checking calendar status for user: {}", userId);
        boolean isConnected = googleCalendarService.isGoogleCalendarConnected(userId);
        return ResponseEntity.ok(isConnected);
    }

    /**
     * Disconnect Google Calendar
     */
    @DeleteMapping("/disconnect")
    public ResponseEntity<?> disconnectCalendar() {
        try {
            Long userId = securityUtils.getCurrentUserId();
            log.info("Disconnect request for user: {}", userId);
            
            if (userId == null) {
                log.error("User ID is null - security context issue");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse("Unauthorized: User not authenticated"));
            }
            
            boolean wasConnected = googleCalendarService.isGoogleCalendarConnected(userId);
            log.info("User {} was connected: {}", userId, wasConnected);
            
            googleCalendarService.disconnectGoogleCalendar(userId);
            log.info("Successfully disconnected Google Calendar for user: {}", userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Google Calendar disconnected successfully");
            response.put("userId", userId);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error disconnecting Google Calendar: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error: " + e.getMessage()));
        }
    }

    /**
     * Helper method to create error response
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
}
