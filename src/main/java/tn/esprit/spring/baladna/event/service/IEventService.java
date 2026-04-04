package tn.esprit.spring.baladna.event.service;

import tn.esprit.spring.baladna.event.entity.Event;
import java.util.List;

public interface IEventService {
    List<Event> retrieveEvents();
    Event addEvent(Event event);
    Event updateEvent(Event event);
    Event retrieveEvent(Long id);
    void removeEvent(Long id);
}
