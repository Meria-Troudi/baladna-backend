package tn.esprit.spring.baladna.event.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.event.dto.EventCategoryDTO;
import tn.esprit.spring.baladna.event.entity.EventCategory;
import tn.esprit.spring.baladna.event.service.IEventCategoryService;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/events/event-category")
public class EventCategoryController {

    private final IEventCategoryService categoryService;

    @GetMapping("/list")
    public List<EventCategory> retrieveEventCategories() {
        return categoryService.retrieveEventCategories();
    }

    @GetMapping("/get/{id}")
    public EventCategory retrieveEventCategory(@PathVariable Long id) {
        return categoryService.retrieveEventCategory(id);
    }

    @PostMapping("/add")
    public EventCategory addEventCategory(@RequestBody EventCategoryDTO category) {
        return categoryService.addEventCategory(category);
    }

    @PutMapping("/update")
    public EventCategory updateEventCategory(@RequestBody EventCategoryDTO category) {
        return categoryService.updateEventCategory(category);
    }

    @DeleteMapping("/delete/{id}")
    public void removeEventCategory(@PathVariable Long id) {
        categoryService.removeEventCategory(id);
    }
}
