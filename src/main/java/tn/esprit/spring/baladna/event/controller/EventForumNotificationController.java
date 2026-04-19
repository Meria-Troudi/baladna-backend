package tn.esprit.spring.baladna.event.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.event.dto.EventForumNotificationDTO;
import tn.esprit.spring.baladna.event.entity.EventForumNotification;
import tn.esprit.spring.baladna.event.service.IEventForumNotificationService;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/events/event-forum-notification")
public class EventForumNotificationController {

    private final IEventForumNotificationService notificationService;

    @GetMapping("/list")
    public List<EventForumNotification> retrieveEventForumNotifications() {
        return notificationService.retrieveEventForumNotifications();
    }

    @GetMapping("/get/{id}")
    public EventForumNotification retrieveEventForumNotification(@PathVariable Long id) {
        return notificationService.retrieveEventForumNotification(id);
    }

    @PostMapping("/add")
    public EventForumNotification addEventForumNotification(@RequestBody EventForumNotificationDTO notification) {
        return notificationService.addEventForumNotification(notification);
    }

    @PutMapping("/update")
    public EventForumNotification updateEventForumNotification(@RequestBody EventForumNotificationDTO notification) {
        return notificationService.updateEventForumNotification(notification);
    }

    @DeleteMapping("/delete/{id}")
    public void removeEventForumNotification(@PathVariable Long id) {
        notificationService.removeEventForumNotification(id);
    }
}
