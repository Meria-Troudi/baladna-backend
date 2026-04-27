package tn.esprit.spring.baladna.event.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.event.entity.Event;
import tn.esprit.spring.baladna.event.repository.EventRepository;
import tn.esprit.spring.baladna.event.service.interfaces.EventService;

import java.util.List;

@AllArgsConstructor
@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;

    @Override
public List<Event> retrieveEvents() {
    List<Event> events = eventRepository.findAll();
    java.time.LocalDateTime now = java.time.LocalDateTime.now();
    for (Event event : events) {
        if (event.getStartAt() != null) {
            if (event.getStartAt().isAfter(now)) {
                if (event.getStatus() != tn.esprit.spring.baladna.event.entity.enums.EventStatus.UPCOMING) {
                    event.setStatus(tn.esprit.spring.baladna.event.entity.enums.EventStatus.UPCOMING);
                    eventRepository.save(event);
                }
            } else {
                if (event.getStatus() != tn.esprit.spring.baladna.event.entity.enums.EventStatus.FINISHED) {
                    event.setStatus(tn.esprit.spring.baladna.event.entity.enums.EventStatus.FINISHED);
                    eventRepository.save(event);
                }
            }
        }
    }
    return events;
}

    @Override
    public Event addEvent(Event event) {
        return eventRepository.save(event);
    }

    @Override
    public Event addEvent(tn.esprit.spring.baladna.event.dto.EventDTO dto, Long hostId) {
        Event event = new Event();
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        // Handle category conversion with fallback
        if (dto.getCategory() != null) {
            try {
                event.setCategory(tn.esprit.spring.baladna.event.entity.enums.EventCategory.valueOf(dto.getCategory().toUpperCase()));
            } catch (IllegalArgumentException e) {
                event.setCategory(tn.esprit.spring.baladna.event.entity.enums.EventCategory.OTHER);
            }
        } else {
            event.setCategory(tn.esprit.spring.baladna.event.entity.enums.EventCategory.OTHER);
        }
        event.setLocation(dto.getLocation());
        event.setStartAt(dto.getStartAt());
        event.setEndAt(dto.getEndAt());
        // Handle status conversion with fallback
        if (dto.getStatus() != null) {
            try {
                event.setStatus(tn.esprit.spring.baladna.event.entity.enums.EventStatus.valueOf(dto.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                event.setStatus(tn.esprit.spring.baladna.event.entity.enums.EventStatus.UPCOMING);
            }
        } else {
            event.setStatus(tn.esprit.spring.baladna.event.entity.enums.EventStatus.UPCOMING);
        }
        event.setLatitude(dto.getLatitude());
        event.setLongitude(dto.getLongitude());
        event.setCapacity(dto.getCapacity());
        event.setPrice(dto.getPrice() != null ? dto.getPrice().doubleValue() : 0.0);
        event.setCreatedByUserId(hostId);
        return eventRepository.save(event);
    }

    @Override
    public Event updateEventFromDTO(Long id, tn.esprit.spring.baladna.event.dto.EventDTO dto, Long hostId) {
        Event event = eventRepository.findById(id).orElseThrow();
        if (event.getCreatedByUserId() != null && !event.getCreatedByUserId().equals(hostId)) {
            throw new RuntimeException("Unauthorized: Only event owner can update");
        }
        // Update fields from DTO
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        // Handle category conversion with fallback
        if (dto.getCategory() != null) {
            try {
                event.setCategory(tn.esprit.spring.baladna.event.entity.enums.EventCategory.valueOf(dto.getCategory().toUpperCase()));
            } catch (IllegalArgumentException e) {
                event.setCategory(tn.esprit.spring.baladna.event.entity.enums.EventCategory.OTHER);
            }
        } else {
            event.setCategory(tn.esprit.spring.baladna.event.entity.enums.EventCategory.OTHER);
        }
        event.setLocation(dto.getLocation());
        event.setStartAt(dto.getStartAt());
event.setEndAt(dto.getEndAt());
// Auto-update status based on start date
if (dto.getStartAt() != null) {
    if (dto.getStartAt().isAfter(java.time.LocalDateTime.now())) {
        event.setStatus(tn.esprit.spring.baladna.event.entity.enums.EventStatus.UPCOMING);
    } else {
        event.setStatus(tn.esprit.spring.baladna.event.entity.enums.EventStatus.FINISHED);
    }
} else if (dto.getStatus() != null) {
    try {
        event.setStatus(tn.esprit.spring.baladna.event.entity.enums.EventStatus.valueOf(dto.getStatus().toUpperCase()));
    } catch (IllegalArgumentException e) {
        // Keep existing status if invalid
    }
}
        event.setLatitude(dto.getLatitude());
        event.setLongitude(dto.getLongitude());
        event.setCapacity(dto.getCapacity());
        event.setPrice(dto.getPrice() != null ? dto.getPrice().doubleValue() : 0.0);
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

    @Override
    public void removeEvent(Long id, Long hostId) {
        // Optionally check hostId matches event owner before deleting
        Event event = eventRepository.findById(id).orElseThrow();
        if (event.getCreatedByUserId() != null && !event.getCreatedByUserId().equals(hostId)) {
            throw new RuntimeException("Unauthorized: Only event owner can delete");
        }
        eventRepository.deleteById(id);
    }

    @Override
    public List<Event> getEventsByHost(Long hostId) {
        return eventRepository.findByCreatedByUserId(hostId);
    }

    @Override
    public List<Event> retrieveEventsByHost(Long hostId) {
        return eventRepository.findByCreatedByUserId(hostId);
    }
    @Override
    public List<Event> getEventsWithMedia() {
        // Fallback: return all events with non-empty media list
        return eventRepository.findAll().stream()
            .filter(e -> e.getMedia() != null && !e.getMedia().isEmpty())
            .toList();
    }

    @Override
    public List<Event> getEventsByStatus(tn.esprit.spring.baladna.event.entity.enums.EventStatus status) {
        return eventRepository.findAll().stream()
            .filter(e -> e.getStatus() == status)
            .toList();
    }

    @Override
    public List<Event> getUpcomingEvents() {
        return eventRepository.findAll().stream()
            .filter(e -> e.getStatus() == tn.esprit.spring.baladna.event.entity.enums.EventStatus.UPCOMING)
            .toList();
    }
}
