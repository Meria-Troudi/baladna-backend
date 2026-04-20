package tn.esprit.spring.baladna.event.forum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.baladna.event.forum.repository.NotificationRepository;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

@RestController
@RequestMapping("/api/forum/notifications")
@RequiredArgsConstructor
public class ForumNotificationController {

    private final NotificationRepository notificationRepo;
    private final UserRepository userRepository;

    private Long resolveUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return user.getId();
    }

    @GetMapping
    public ResponseEntity<?> getUserNotifications(Authentication authentication) {
        Long currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(
                notificationRepo.findByUserIdOrderByCreatedAtDesc(currentUserId)
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(Authentication authentication) {
        Long currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(
                notificationRepo.countByUserIdAndIsReadFalse(currentUserId)
        );
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication authentication) {
        Long currentUserId = resolveUserId(authentication);
        // Check if notification exists for this user
        boolean exists = notificationRepo.findByUserIdOrderByCreatedAtDesc(currentUserId)
            .stream().anyMatch(n -> n.getId().equals(id));
        if (!exists) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found");
        }
        notificationRepo.markAsRead(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        Long currentUserId = resolveUserId(authentication);
        notificationRepo.markAllAsRead(currentUserId);
        return ResponseEntity.noContent().build();
    }
}