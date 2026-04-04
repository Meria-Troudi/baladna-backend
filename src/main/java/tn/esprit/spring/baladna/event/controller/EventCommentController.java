package tn.esprit.spring.baladna.event.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.event.dto.EventCommentDTO;
import tn.esprit.spring.baladna.event.entity.EventComment;
import tn.esprit.spring.baladna.event.service.IEventCommentService;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/events/event-comment")
public class EventCommentController {

    private final IEventCommentService commentService;

    @GetMapping("/list")
    public List<EventComment> retrieveEventComments() {
        return commentService.retrieveEventComments();
    }

    @GetMapping("/get/{id}")
    public EventComment retrieveEventComment(@PathVariable Long id) {
        return commentService.retrieveEventComment(id);
    }

    @PostMapping("/add")
    public EventComment addEventComment(@RequestBody EventCommentDTO comment) {
        return commentService.addEventComment(comment);
    }

    @PutMapping("/update")
    public EventComment updateEventComment(@RequestBody EventCommentDTO comment) {
        return commentService.updateEventComment(comment);
    }

    @DeleteMapping("/delete/{id}")
    public void removeEventComment(@PathVariable Long id) {
        commentService.removeEventComment(id);
    }
}
