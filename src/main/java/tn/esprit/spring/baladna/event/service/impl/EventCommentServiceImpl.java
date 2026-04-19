package tn.esprit.spring.baladna.event.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.event.dto.EventCommentDTO;
import tn.esprit.spring.baladna.event.entity.Event;
import tn.esprit.spring.baladna.event.entity.EventComment;
import tn.esprit.spring.baladna.event.repository.EventRepository;
import tn.esprit.spring.baladna.event.repository.EventCommentRepository;
import tn.esprit.spring.baladna.event.service.IEventCommentService;

import java.util.List;

@AllArgsConstructor
@Service
public class EventCommentServiceImpl implements IEventCommentService {

    private final EventCommentRepository commentRepository;
    private final EventRepository eventRepository;

    @Override
    public List<EventComment> retrieveEventComments() {
        return commentRepository.findAll();
    }

    @Override
    public EventComment addEventComment(EventCommentDTO comment) {
        Event event = eventRepository.findById(comment.getEventId()).orElse(null);
        EventComment entity = EventComment.builder()
                .event(event)
                .userId(comment.getUserId())
                .content(comment.getContent())
                .build();
        return commentRepository.save(entity);
    }

    @Override
    public EventComment updateEventComment(EventCommentDTO comment) {
        EventComment entity = commentRepository.findById(comment.getId()).orElse(null);
        if (entity == null) {
            return null;
        }
        if (comment.getEventId() != null) {
            entity.setEvent(eventRepository.findById(comment.getEventId()).orElse(null));
        }
        entity.setUserId(comment.getUserId());
        entity.setContent(comment.getContent());
        return commentRepository.save(entity);
    }

    @Override
    public EventComment retrieveEventComment(Long id) {
        return commentRepository.findById(id).orElse(null);
    }

    @Override
    public void removeEventComment(Long id) {
        commentRepository.deleteById(id);
    }
}
