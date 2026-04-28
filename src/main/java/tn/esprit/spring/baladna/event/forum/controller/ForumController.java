package tn.esprit.spring.baladna.event.forum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.baladna.event.forum.dto.FeedRequest;
import tn.esprit.spring.baladna.event.forum.dto.FeedResponse;
import tn.esprit.spring.baladna.event.forum.service.InteractionService;
import tn.esprit.spring.baladna.event.forum.service.PostService;
import tn.esprit.spring.baladna.event.forum.entity.Post;
import tn.esprit.spring.baladna.event.forum.entity.PostStatus;
import tn.esprit.spring.baladna.event.forum.repository.PostRepository;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class ForumController {

    private final PostService postService;
    private final InteractionService interactionService;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

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

    private Long tryResolveUserId(Authentication authentication) {
        try {
            return resolveUserId(authentication);
        } catch (Exception ignored) {
            return null;
        }
    }

    @GetMapping("/feed")
    public FeedResponse getFeed(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "LATEST") String mode,
            Authentication authentication
    ) {
        FeedRequest req = new FeedRequest();
        req.setCursor(cursor);
        req.setSize(size);
        req.setMode(mode);
        return postService.getFeed(req, tryResolveUserId(authentication));
    }

    // Saved posts (frontend expects /api/forum/flags/saved + /api/forum/flags/save/{postId})
    @GetMapping("/flags/saved")
    public ResponseEntity<?> getSavedPosts(Authentication authentication) {
        Long currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(interactionService.getSavedPosts(currentUserId));
    }

    @PostMapping("/flags/save/{postId}")
    public ResponseEntity<?> toggleSave(@PathVariable Long postId, Authentication authentication) {
        Long currentUserId = resolveUserId(authentication);
        boolean result = interactionService.toggleSave(postId, currentUserId);
        return ResponseEntity.ok(result);
    }

    // Pinned post: until explicit pin-flag storage exists, surface the most-liked visible post.
    @GetMapping("/flags/pinned")
    public ResponseEntity<?> getPinned(Authentication authentication) {
        Long currentUserId = tryResolveUserId(authentication);
        return postRepository.findTopByStatusOrderByLikesCountDescCreatedAtDesc(PostStatus.VISIBLE)
                .filter(p -> p.getLikesCount() > 0)
                .<ResponseEntity<?>>map(p -> ResponseEntity.ok(postService.mapToDTO(p, currentUserId)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/flags/pin/{postId}")
    public ResponseEntity<?> pinPost(@PathVariable Long postId, Authentication authentication) {
        resolveUserId(authentication);
        // Explicit pinning requires admin + storage; for now the pinned post is derived from likes.
        return ResponseEntity.ok().build();
    }
}