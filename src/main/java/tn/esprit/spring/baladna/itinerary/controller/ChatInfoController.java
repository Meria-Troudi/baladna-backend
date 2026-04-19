package tn.esprit.spring.baladna.itinerary.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.baladna.itinerary.dto.ChatInfoResponse;
import tn.esprit.spring.baladna.itinerary.util.SecurityUtils;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

/**
 * REST endpoints for chat service information.
 * Provides health check and current user info.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatInfoController {

    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;

    /**
     * Get chat service info and current user details
     *
     * GET /api/chat/info
     * GET /api/chat/info?userId=1
     *
     * Optional: Pass valid JWT in Authorization header for authenticated info:
     *   Authorization: Bearer <token>
     *
     * Falls back to query parameter userId if no JWT is provided
     *
     * @return chat service status and user info
     */
    @GetMapping("/info")
    public ResponseEntity<ChatInfoResponse> getChatInfo(
            @org.springframework.web.bind.annotation.RequestParam(value = "userId", required = false) Long userIdParam) {
        
        Long userId = null;
        String userName = "Unknown";
        
        // Try to get userId from JWT first
        try {
            userId = securityUtils.getCurrentUserId();
        } catch (Exception e) {
            // If JWT extraction fails, use the query parameter
            userId = userIdParam;
        }
        
        // If still no userId, return generic response
        if (userId == null) {
            ChatInfoResponse response = ChatInfoResponse.builder()
                    .status("active")
                    .userId(null)
                    .userName("Anonymous")
                    .available(true)
                    .message("Chat service is active. Please authenticate to access full features")
                    .build();
            return ResponseEntity.ok(response);
        }
        
        // Get current user details
        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
                userName = user.getFirstName();
                if (user.getLastName() != null && !user.getLastName().isEmpty()) {
                    userName += " " + user.getLastName();
                }
            } else if (user.getEmail() != null) {
                userName = user.getEmail();
            }
        }

        ChatInfoResponse response = ChatInfoResponse.builder()
                .status("active")
                .userId(userId)
                .userName(userName)
                .available(true)
                .message("Chat service is active and ready")
                .build();

        return ResponseEntity.ok(response);
    }
}
