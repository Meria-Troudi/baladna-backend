package tn.esprit.spring.baladna.event.service;

import tn.esprit.spring.baladna.event.dto.EventForumNotificationDTO;
import tn.esprit.spring.baladna.event.entity.EventForumNotification;
import java.util.List;

public interface IEventForumNotificationService {
    List<EventForumNotification> retrieveEventForumNotifications();
    EventForumNotification addEventForumNotification(EventForumNotificationDTO notification);
    EventForumNotification updateEventForumNotification(EventForumNotificationDTO notification);
    EventForumNotification retrieveEventForumNotification(Long id);
    void removeEventForumNotification(Long id);
}
