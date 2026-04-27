package tn.esprit.spring.baladna.event.forum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.baladna.event.forum.dto.CommentDTO;
import tn.esprit.spring.baladna.event.forum.dto.CreateCommentRequestDTO;
import tn.esprit.spring.baladna.event.forum.dto.ReactionRequestDTO;
import tn.esprit.spring.baladna.event.forum.dto.ReactionResponseDTO;
import tn.esprit.spring.baladna.event.forum.service.InteractionService;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.util.List;

@RestController
@RequestMapping("/api/forum/interactions")
@RequiredArgsConstructor
public class ForumInteractionController {

    private final InteractionService interactionService;
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

    private Long tryResolveUserId(Authentication authentication) {
        try {
            return resolveUserId(authentication);
        } catch (Exception ignored) {
            return null;
        }
    }

    // =========================
    // REACT (REACTIONS)
    // =========================
    @PostMapping("/posts/{id}/react")
    public ResponseEntity<ReactionResponseDTO> react(
            @PathVariable Long id,
            @RequestBody ReactionRequestDTO request,
            Authentication authentication
    ) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(
                interactionService.react(id, userId, request.getType())
        );
    }

    @GetMapping("/posts/{id}/reactions")
    public ResponseEntity<ReactionResponseDTO> getReactions(
            @PathVariable Long id,
            Authentication authentication
    ) {
        Long userId = tryResolveUserId(authentication);
        return ResponseEntity.ok(
                interactionService.getReactions(id, userId)
        );
    }

    // =========================
    // COMMENTS
    // =========================
    @PostMapping("/posts/{id}/comments")
    public ResponseEntity<CommentDTO> addComment(
            @PathVariable Long id,
            @RequestBody CreateCommentRequestDTO request,
            Authentication authentication
    ) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(
                interactionService.addComment(
                        id,
                        userId,
                        request.getContent(),
                        request.getParentId()
                )
        );
    }

    @GetMapping("/posts/{id}/comments")
    public ResponseEntity<List<CommentDTO>> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(
                interactionService.getComments(id)
        );
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id, Authentication authentication) {
        Long userId = resolveUserId(authentication);
        interactionService.deleteComment(id, userId);
        return ResponseEntity.noContent().build();
    }
}