package tn.esprit.spring.baladna.event.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.event.dto.EventMediaDTO;
import tn.esprit.spring.baladna.event.entity.EventMedia;
import tn.esprit.spring.baladna.event.service.IEventMediaService;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/events/event-media")
public class EventMediaController {

    private final IEventMediaService mediaService;

    @GetMapping("/list")
    public List<EventMedia> retrieveEventMedias() {
        return mediaService.retrieveEventMedias();
    }

    @GetMapping("/get/{id}")
    public EventMedia retrieveEventMedia(@PathVariable Long id) {
        return mediaService.retrieveEventMedia(id);
    }

    @PostMapping("/add")
    public EventMedia addEventMedia(@RequestBody EventMediaDTO media) {
        return mediaService.addEventMedia(media);
    }

    @PutMapping("/update")
    public EventMedia updateEventMedia(@RequestBody EventMediaDTO media) {
        return mediaService.updateEventMedia(media);
    }

    @DeleteMapping("/delete/{id}")
    public void removeEventMedia(@PathVariable Long id) {
        mediaService.removeEventMedia(id);
    }
}
