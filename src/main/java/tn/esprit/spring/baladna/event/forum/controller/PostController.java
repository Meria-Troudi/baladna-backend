package tn.esprit.spring.baladna.event.forum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.baladna.event.forum.dto.CreatePostRequestDTO;
import tn.esprit.spring.baladna.event.forum.dto.FeedRequest;
import tn.esprit.spring.baladna.event.forum.dto.FeedResponse;
import tn.esprit.spring.baladna.event.forum.dto.PostDTO;
import tn.esprit.spring.baladna.event.forum.service.PostService;
import tn.esprit.spring.baladna.event.forum.service.TopicInferenceService;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/posts", "/api/forum/posts"})
@RequiredArgsConstructor
public class PostController {

    private final UserRepository userRepository;
    private final PostService postService;
    private final TopicInferenceService topicInferenceService;

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

    @GetMapping
    public ResponseEntity<List<PostDTO>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "latest") String sort,
            Authentication authentication
    ) {

        Sort sortOrder = sort.equals("popular")
                ? Sort.by(Sort.Direction.DESC, "likesCount", "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");

        Long currentUserId = tryResolveUserId(authentication);

        return ResponseEntity.ok(
                postService.getAllPosts(PageRequest.of(page, size, sortOrder), currentUserId).getContent()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> getPost(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(postService.getPostById(id, tryResolveUserId(authentication)));
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<PostDTO> getPostPreview(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(postService.getPostById(id, tryResolveUserId(authentication)));
    }

    @PostMapping
    public ResponseEntity<PostDTO> createPost(@RequestBody CreatePostRequestDTO request, Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(postService.createPost(userId, request));
    }

    @PostMapping("/ai/topic-preview")
    public ResponseEntity<TopicInferenceService.Result> previewTopic(@RequestBody Map<String, String> body) {
        String content = body != null ? body.get("content") : null;
        return ResponseEntity.ok(topicInferenceService.classify(content));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id, Authentication authentication) {
        Long userId = resolveUserId(authentication);
        postService.deletePost(id, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMyPosts(Authentication authentication) {
        Long currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(postService.getPostsByUserId(currentUserId, currentUserId));
    }
}