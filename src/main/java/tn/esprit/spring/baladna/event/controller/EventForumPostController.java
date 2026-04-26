package tn.esprit.spring.baladna.event.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.event.dto.EventForumPostDTO;
import tn.esprit.spring.baladna.event.entity.EventForumPost;
import tn.esprit.spring.baladna.event.service.IEventForumPostService;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/events/event-forum-post")
public class EventForumPostController {

    private final IEventForumPostService forumPostService;

    @GetMapping("/list")
    public List<EventForumPost> retrieveEventForumPosts() {
        return forumPostService.retrieveEventForumPosts();
    }

    @GetMapping("/get/{id}")
    public EventForumPost retrieveEventForumPost(@PathVariable Long id) {
        return forumPostService.retrieveEventForumPost(id);
    }

    @PostMapping("/add")
    public EventForumPost addEventForumPost(@RequestBody EventForumPostDTO post) {
        return forumPostService.addEventForumPost(post);
    }

    @PutMapping("/update")
    public EventForumPost updateEventForumPost(@RequestBody EventForumPostDTO post) {
        return forumPostService.updateEventForumPost(post);
    }

    @DeleteMapping("/delete/{id}")
    public void removeEventForumPost(@PathVariable Long id) {
        forumPostService.removeEventForumPost(id);
    }
}
