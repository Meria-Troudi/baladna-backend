package tn.esprit.spring.baladna.event.service;

import tn.esprit.spring.baladna.event.dto.EventCategoryDTO;
import tn.esprit.spring.baladna.event.entity.EventCategory;
import java.util.List;

public interface IEventCategoryService {
    List<EventCategory> retrieveEventCategories();
    EventCategory addEventCategory(EventCategoryDTO category);
    EventCategory updateEventCategory(EventCategoryDTO category);
    EventCategory retrieveEventCategory(Long id);
    void removeEventCategory(Long id);
}
