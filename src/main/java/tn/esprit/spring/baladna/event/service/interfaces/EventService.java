package tn.esprit.spring.baladna.event.service.interfaces;

import tn.esprit.spring.baladna.event.dto.EventDTO;
import tn.esprit.spring.baladna.event.entity.Event;

import java.util.List;

public interface EventService {
    // Clean, unified EventService interface for controller usage
    //List<Event> getAll();
   // Event getById(Long id);
    Event addEvent(EventDTO dto, Long hostId);
    Event updateEventFromDTO(Long id, EventDTO dto, Long hostId);
   void removeEvent(Long id, Long hostId);
    List<Event> getUpcomingEvents();
    List<Event> getEventsByStatus(tn.esprit.spring.baladna.event.entity.enums.EventStatus status);
    List<Event> getEventsWithMedia();

   // void removeEvent(Long id);

   // List<Event> getEventsByHost(Long hostId);
    List<Event> retrieveEvents();


   // Event addEvent(Event event);

   // Event updateEvent(Event event);

    Event addEvent(Event event);

    Event updateEvent(Event event);

    Event retrieveEvent(Long id);


    void removeEvent(Long id);

    List<Event> getEventsByHost(Long hostId);

    List<Event> retrieveEventsByHost(Long hostId);
}