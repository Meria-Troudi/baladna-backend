package tn.esprit.spring.baladna.event.service;

import tn.esprit.spring.baladna.event.dto.EventMediaDTO;
import tn.esprit.spring.baladna.event.entity.EventMedia;
import java.util.List;

public interface IEventMediaService {
    List<EventMedia> retrieveEventMedias();
    EventMedia addEventMedia(EventMediaDTO media);
    EventMedia updateEventMedia(EventMediaDTO media);
    EventMedia retrieveEventMedia(Long id);
    void removeEventMedia(Long id);
}
