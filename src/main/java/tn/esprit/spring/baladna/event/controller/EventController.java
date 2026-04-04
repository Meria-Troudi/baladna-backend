package tn.esprit.spring.baladna.event.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.event.dto.EventDTO;
import tn.esprit.spring.baladna.event.entity.Event;
import tn.esprit.spring.baladna.event.service.EventService;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/events/event")
public class EventController {

    private final EventService eventService;

    @GetMapping("/list")
    public List<Event> retrieveEvents() {
        return eventService.retrieveEvents();
    }

    @GetMapping("/get/{id}")
    public Event retrieveEvent(@PathVariable Long id) {
        return eventService.retrieveEvent(id);
    }

    @PostMapping("/add")
    public Event addEvent(@RequestBody EventDTO dto) {
        return eventService.addEvent(dto);
    }

    @PutMapping("/update")
    public Event updateEvent(@RequestBody Event event) {
        return eventService.updateEvent(event);
    }

    @DeleteMapping("/delete/{id}")
    public void removeEvent(@PathVariable Long id) {
        eventService.removeEvent(id);
    }
}
