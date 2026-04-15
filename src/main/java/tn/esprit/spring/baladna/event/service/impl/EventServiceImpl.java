package tn.esprit.spring.baladna.event.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.event.entity.Event;
import tn.esprit.spring.baladna.event.repository.EventRepository;
import tn.esprit.spring.baladna.event.service.IEventService;

import java.util.List;

@AllArgsConstructor
@Service
public class EventServiceImpl implements IEventService {

    private final EventRepository eventRepository;

    @Override
    public List<Event> retrieveEvents() {
        return eventRepository.findAll();
    }

    @Override
    public Event addEvent(Event event) {
        return eventRepository.save(event);
    }

    @Override
    public Event updateEvent(Event event) {
        return eventRepository.save(event);
    }

    @Override
    public Event retrieveEvent(Long id) {
        return eventRepository.findById(id).orElse(null);
    }

    @Override
    public void removeEvent(Long id) {
        eventRepository.deleteById(id);
    }
}
