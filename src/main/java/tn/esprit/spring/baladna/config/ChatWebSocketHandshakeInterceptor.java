package tn.esprit.spring.baladna.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import tn.esprit.spring.baladna.itinerary.repository.ItineraryCollaboratorRepository;
import tn.esprit.spring.baladna.itinerary.repository.ItineraryRepository;
import tn.esprit.spring.baladna.user.service.JwtService;

import java.util.Map;
import java.util.UUID;

/**
 * Interceptor for WebSocket handshake to extract, validate JWT, and check permissions
 * 
 * This interceptor:
 * 1. Extracts JWT from query parameter or Authorization header
 * 2. Validates JWT using JwtService
 * 3. Extracts userId from JWT
 * 4. Extracts itineraryId from the URL path
 * 5. Checks if user is a collaborator in the itinerary
 * 6. Stores userId and itineraryId in session attributes
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final ItineraryRepository itineraryRepository;
    private final ItineraryCollaboratorRepository collaboratorRepository;

    /**
     * Before handshake - validate JWT and check permissions
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            String uri = request.getURI().toString();
            log.info("WebSocket connection attempt: {}", uri);

            // Extract JWT token
            String token = extractJwtToken(request);
            if (token == null) {
                log.warn("WebSocket connection rejected: JWT token not provided");
                return false;
            }

            // Validate JWT and extract userId
            Long userId;
            try {
                userId = jwtService.extractUserId(token);
                if (userId == null) {
                    log.warn("WebSocket connection rejected: Unable to extract userId from JWT");
                    return false;
                }
            } catch (Exception e) {
                log.warn("WebSocket connection rejected: Invalid JWT token - {}", e.getMessage());
                return false;
            }

            // Extract itineraryId from query parameter
            String itineraryIdStr = extractItineraryIdFromUri(uri);
            if (itineraryIdStr == null) {
                log.warn("WebSocket connection rejected: itineraryId not provided");
                return false;
            }

            // Validate that itinerary exists
            UUID itineraryId;
            try {
                itineraryId = UUID.fromString(itineraryIdStr);
            } catch (IllegalArgumentException e) {
                log.warn("WebSocket connection rejected: Invalid itineraryId format: {}", itineraryIdStr);
                return false;
            }

            // Check if itinerary exists
            if (!itineraryRepository.existsById(itineraryId)) {
                log.warn("WebSocket connection rejected: Itinerary not found: {}", itineraryId);
                return false;
            }

            // Check if user is a collaborator in this itinerary
            boolean isCollaborator = collaboratorRepository.findByItineraryIdAndUserId(itineraryId, userId).isPresent();
            boolean isOwner = itineraryRepository.findById(itineraryId)
                    .map(itin -> itin.getOwnerId().equals(userId))
                    .orElse(false);

            if (!isCollaborator && !isOwner) {
                log.warn("WebSocket connection rejected: User {} is not a collaborator in itinerary {}", 
                        userId, itineraryId);
                return false;
            }

            // Store userId and itineraryId in session attributes
            attributes.put("userId", userId);
            attributes.put("itineraryId", itineraryIdStr);

            log.info("WebSocket handshake successful for userId: {}, itineraryId: {}", userId, itineraryId);
            return true;

        } catch (Exception e) {
            log.error("Unexpected error during WebSocket handshake: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * After handshake - logging only
     */
    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("WebSocket handshake error: {}", exception.getMessage());
        } else {
            log.info("WebSocket handshake completed successfully");
        }
    }

    /**
     * Extract JWT token from query parameter or Authorization header
     * 
     * Priority:
     * 1. Query parameter: ?token=<token>
     * 2. Authorization header: Authorization: Bearer <token>
     */
    private String extractJwtToken(ServerHttpRequest request) {
        // Try to extract from query parameter first
        String uri = request.getURI().toString();
        if (uri.contains("token=")) {
            try {
                String query = uri.substring(uri.indexOf("?") + 1);
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("token=")) {
                        String token = param.substring(6);
                        if (!token.isEmpty()) {
                            return token;
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("Failed to extract token from query parameter: {}", e.getMessage());
            }
        }

        // Try to extract from Authorization header
        java.util.List<String> authHeaders = request.getHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }

        return null;
    }

    /**
     * Extract itineraryId from URI query parameters
     * 
     * Expected format: ?itineraryId=123e4567-e89b-12d3-a456-426614174000
     */
    private String extractItineraryIdFromUri(String uri) {
        try {
            if (!uri.contains("itineraryId=")) {
                log.debug("itineraryId parameter not found in URI");
                return null;
            }

            String query = uri.substring(uri.indexOf("?") + 1);
            String[] params = query.split("&");

            for (String param : params) {
                if (param.startsWith("itineraryId=")) {
                    String itineraryIdStr = param.substring(12);
                    if (!itineraryIdStr.isEmpty()) {
                        return itineraryIdStr;
                    }
                }
            }

            return null;

        } catch (Exception e) {
            log.debug("Error extracting itineraryId from URI: {}", e.getMessage());
            return null;
        }
    }
}
