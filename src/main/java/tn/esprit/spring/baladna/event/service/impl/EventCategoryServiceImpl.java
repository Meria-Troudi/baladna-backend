package tn.esprit.spring.baladna.event.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.event.dto.EventCategoryDTO;
import tn.esprit.spring.baladna.event.entity.EventCategory;
import tn.esprit.spring.baladna.event.repository.EventCategoryRepository;
import tn.esprit.spring.baladna.event.service.IEventCategoryService;

import java.util.List;

@AllArgsConstructor
@Service
public class EventCategoryServiceImpl implements IEventCategoryService {

    private final EventCategoryRepository categoryRepository;

    @Override
    public List<EventCategory> retrieveEventCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public EventCategory addEventCategory(EventCategoryDTO category) {
        EventCategory entity = EventCategory.builder()
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .icon(category.getIcon())
                .isActive(category.getIsActive())
                .build();
        return categoryRepository.save(entity);
    }

    @Override
    public EventCategory updateEventCategory(EventCategoryDTO category) {
        EventCategory entity = categoryRepository.findById(category.getId()).orElse(null);
        if (entity == null) {
            return null;
        }
        entity.setName(category.getName());
        entity.setDescription(category.getDescription());
        entity.setImageUrl(category.getImageUrl());
        entity.setIcon(category.getIcon());
        entity.setIsActive(category.getIsActive());
        return categoryRepository.save(entity);
    }

    @Override
    public EventCategory retrieveEventCategory(Long id) {
        return categoryRepository.findById(id).orElse(null);
    }

    @Override
    public void removeEventCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}
