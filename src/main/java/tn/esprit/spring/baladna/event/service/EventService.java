package tn.esprit.spring.baladna.event.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.event.dto.EventDTO;
import tn.esprit.spring.baladna.event.entity.Event;
import tn.esprit.spring.baladna.event.entity.EventCategory;
import tn.esprit.spring.baladna.event.entity.enums.EventStatus;
import tn.esprit.spring.baladna.event.repository.EventCategoryRepository;
import tn.esprit.spring.baladna.event.repository.EventRepository;

import java.util.List;

@Service
@AllArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final EventCategoryRepository categoryRepository;

    public List<Event> retrieveEvents() {
        return eventRepository.findAll();
    }

    public Event addEvent(EventDTO dto) {
        EventCategory category = categoryRepository.findById(dto.getCategoryId()).orElse(null);

        Event event = Event.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .category(category)
                .startAt(dto.getStartAt())
                .endAt(dto.getEndAt())
                .location(dto.getLocation())
                .latitude(dto.getLatitude())
                .longitude(dto.getLongitude())
                .capacity(dto.getCapacity())
                .price(dto.getPrice())
                .createdByUserId(dto.getCreatedByUserId())
                .bookedSeats(0)
                .status(EventStatus.UPCOMING)
                .build();

        return eventRepository.save(event);
    }

    public Event updateEvent(Event event) {
        return eventRepository.save(event);
    }

    public Event retrieveEvent(Long id) {
        return eventRepository.findById(id).orElse(null);
    }

    public void removeEvent(Long id) {
        eventRepository.deleteById(id);
    }
}
