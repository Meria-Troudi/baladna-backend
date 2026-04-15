package tn.esprit.spring.baladna.event.service;

import tn.esprit.spring.baladna.event.dto.EventCommentDTO;
import tn.esprit.spring.baladna.event.entity.EventComment;
import java.util.List;

public interface IEventCommentService {
    List<EventComment> retrieveEventComments();
    EventComment addEventComment(EventCommentDTO comment);
    EventComment updateEventComment(EventCommentDTO comment);
    EventComment retrieveEventComment(Long id);
    void removeEventComment(Long id);
}
