package tn.esprit.spring.baladna.event.forum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.event.forum.dto.PostDTO;
import tn.esprit.spring.baladna.event.forum.entity.PostTopic;
import tn.esprit.spring.baladna.event.forum.service.PostService;

import java.util.List;

@RestController
@RequestMapping("/api/forum/admin/posts")
@RequiredArgsConstructor
public class AdminController {

    private final PostService postService;

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(postService.getAdminStats());
    }

    @GetMapping
    public ResponseEntity<List<PostDTO>> getAllPosts() {
        return ResponseEntity.ok(postService.getAllPostsAdmin());
    }

    @RequestMapping(value = "/{id}/status", method = {RequestMethod.GET, RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        postService.updatePostStatus(id, status);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/topic")
    public ResponseEntity<Void> overrideTopic(@PathVariable Long id, @RequestParam PostTopic topic) {
        postService.overrideFinalTopic(id, topic);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Permanently deletes a post and all its dependencies (comments, reactions,
     * saved-bookmarks, notifications). Used from the admin dashboard's "Delete" action.
     */
     @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> hardDelete(@PathVariable Long id) {
        postService.hardDeletePost(id);
        return ResponseEntity.noContent().build();
    }
}