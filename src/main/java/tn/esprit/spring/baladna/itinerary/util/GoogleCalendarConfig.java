package tn.esprit.spring.baladna.itinerary.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * Google Calendar Configuration helper using REST API calls
 */
@Slf4j
@Component
public class GoogleCalendarConfig {

    @Value("${google.calendar.client-id:YOUR_CLIENT_ID}")
    private String clientId;

    @Value("${google.calendar.client-secret:YOUR_CLIENT_SECRET}")
    private String clientSecret;

    @Value("${google.calendar.redirect-uri:http://localhost:8081/api/itineraries/calendar/oauth/callback}")
    private String redirectUri;

    private static final String GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Exchange authorization code for access token
     */
    public GoogleTokenResponse exchangeCodeForToken(String authorizationCode) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = String.format(
                "code=%s&client_id=%s&client_secret=%s&redirect_uri=%s&grant_type=authorization_code",
                authorizationCode, clientId, clientSecret, encodeUriComponent(redirectUri)
        );

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        try {
            String response = restTemplate.postForObject(GOOGLE_TOKEN_URL, request, String.class);
            return parseTokenResponse(response);
        } catch (Exception e) {
            log.error("Error exchanging authorization code: {}", e.getMessage());
            throw new IOException("Failed to exchange authorization code", e);
        }
    }

    /**
     * Parse Google's token response
     */
    private GoogleTokenResponse parseTokenResponse(String jsonResponse) throws IOException {
        JsonNode node = objectMapper.readTree(jsonResponse);
        
        return GoogleTokenResponse.builder()
                .accessToken(node.get("access_token").asText())
                .refreshToken(node.has("refresh_token") ? node.get("refresh_token").asText() : null)
                .expiresIn(node.has("expires_in") ? node.get("expires_in").asLong() : 3600)
                .tokenType(node.get("token_type").asText("Bearer"))
                .build();
    }

    /**
     * Get OAuth authorization URL with state parameter for user tracking
     * @param userId User ID to encode in state parameter
     */
    public String getAuthorizationUrl(Long userId) throws UnsupportedEncodingException {
        // Encode user ID in state parameter to track which user initiated the OAuth flow
        String state = encodeUriComponent(userId.toString());
        
        return String.format(
                "%s?client_id=%s&scope=%s&redirect_uri=%s&response_type=code&access_type=offline&state=%s",
                GOOGLE_AUTH_URL,
                clientId,
                encodeUriComponent(CALENDAR_SCOPE),
                encodeUriComponent(redirectUri),
                state
        );
    }
    
    /**
     * Get OAuth authorization URL without state (backwards compatibility)
     */
    public String getAuthorizationUrl() throws UnsupportedEncodingException {
        return String.format(
                "%s?client_id=%s&scope=%s&redirect_uri=%s&response_type=code&access_type=offline",
                GOOGLE_AUTH_URL,
                clientId,
                encodeUriComponent(CALENDAR_SCOPE),
                encodeUriComponent(redirectUri)
        );
    }

    /**
     * URL encode a string
     */
    private String encodeUriComponent(String component) throws UnsupportedEncodingException {
        return URLEncoder.encode(component, "UTF-8");
    }

    /**
     * Get Google Calendar API URL
     */
    public static String getCalendarApiUrl() {
        return "https://www.googleapis.com/calendar/v3";
    }
}
