package tn.esprit.spring.baladna.event.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.spring.baladna.event.dto.EventDTO;
import tn.esprit.spring.baladna.event.entity.Event;
import tn.esprit.spring.baladna.event.entity.enums.EventCategory;
import tn.esprit.spring.baladna.event.entity.enums.EventStatus;
import tn.esprit.spring.baladna.event.service.interfaces.EventService;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@RestController
@RequestMapping("/api/events/event")
public class EventController {

    private final EventService eventService;
    private final UserRepository userRepository;

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

    @GetMapping("/list")
    public List<Event> retrieveEvents() {
        return eventService.retrieveEvents();
    }

    @GetMapping("/categories/enum")
    public List<String> getCategories() {
        return Arrays.stream(EventCategory.values())
                .map(EventCategory::name)
                .collect(Collectors.toList());
    }

    @GetMapping("/host/me")
    public List<Event> retrieveMyHostedEvents(Authentication authentication) {
        return eventService.retrieveEventsByHost(resolveUserId(authentication));
    }

    @GetMapping("/host/{hostId}")
    public List<Event> retrieveEventsByHost(@PathVariable Long hostId) {
        return eventService.retrieveEventsByHost(hostId);
    }

    @GetMapping("/get/{id}")
    public Event retrieveEvent(@PathVariable Long id) {
        return eventService.retrieveEvent(id);
    }

    @PostMapping("/add")
    public Event addEvent(@RequestBody EventDTO dto, Authentication authentication) {
        return eventService.addEvent(dto, resolveUserId(authentication));
    }

    @PutMapping("/update/{id}")
    public Event updateEvent(@PathVariable Long id, @RequestBody EventDTO dto, Authentication authentication) {
        return eventService.updateEventFromDTO(id, dto, resolveUserId(authentication));
    }

    @DeleteMapping("/delete/{id}")
    public void removeEvent(@PathVariable Long id, Authentication authentication) {
        eventService.removeEvent(id, resolveUserId(authentication));
    }

    @GetMapping("/upcoming")
    public List<Event> getUpcomingEvents() {
        return eventService.getUpcomingEvents();
    }

    @GetMapping("/status/{status}")
    public List<Event> getEventsByStatus(@PathVariable EventStatus status) {
        return eventService.getEventsByStatus(status);
    }

    @GetMapping("/with-media")
    public List<Event> getEventsWithMedia() {
        return eventService.getEventsWithMedia();
    }
}