package tn.esprit.spring.baladna.event.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.event.dto.EventMediaDTO;
import tn.esprit.spring.baladna.event.entity.Event;
import tn.esprit.spring.baladna.event.entity.EventMedia;
import tn.esprit.spring.baladna.event.entity.enums.MediaType;
import tn.esprit.spring.baladna.event.repository.EventRepository;
import tn.esprit.spring.baladna.event.repository.EventMediaRepository;
import tn.esprit.spring.baladna.event.service.IEventMediaService;

import java.util.List;

@AllArgsConstructor
@Service
public class EventMediaServiceImpl implements IEventMediaService {

    private final EventMediaRepository mediaRepository;
    private final EventRepository eventRepository;

    @Override
    public List<EventMedia> retrieveEventMedias() {
        return mediaRepository.findAll();
    }

    @Override
    public EventMedia addEventMedia(EventMediaDTO media) {
        Event event = eventRepository.findById(media.getEventId()).orElse(null);
        EventMedia entity = EventMedia.builder()
                .event(event)
                .url(media.getUrl())
                .type(media.getType() == null ? null : MediaType.valueOf(media.getType()))
                .isCover(media.getIsCover())
                .orderIndex(media.getOrderIndex())
                .build();
        return mediaRepository.save(entity);
    }

    @Override
    public EventMedia updateEventMedia(EventMediaDTO media) {
        EventMedia entity = mediaRepository.findById(media.getId()).orElse(null);
        if (entity == null) {
            return null;
        }
        if (media.getEventId() != null) {
            entity.setEvent(eventRepository.findById(media.getEventId()).orElse(null));
        }
        entity.setUrl(media.getUrl());
        entity.setType(media.getType() == null ? null : MediaType.valueOf(media.getType()));
        entity.setIsCover(media.getIsCover());
        entity.setOrderIndex(media.getOrderIndex());
        return mediaRepository.save(entity);
    }

    @Override
    public EventMedia retrieveEventMedia(Long id) {
        return mediaRepository.findById(id).orElse(null);
    }

    @Override
    public void removeEventMedia(Long id) {
        mediaRepository.deleteById(id);
    }
}
