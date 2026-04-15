package tn.esprit.spring.baladna.event.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.event.dto.EventForumNotificationDTO;
import tn.esprit.spring.baladna.event.entity.EventForumNotification;
import tn.esprit.spring.baladna.event.entity.EventForumPost;
import tn.esprit.spring.baladna.event.entity.enums.NotificationType;
import tn.esprit.spring.baladna.event.repository.EventForumNotificationRepository;
import tn.esprit.spring.baladna.event.repository.EventForumPostRepository;
import tn.esprit.spring.baladna.event.service.IEventForumNotificationService;

import java.util.List;

@AllArgsConstructor
@Service
public class EventForumNotificationServiceImpl implements IEventForumNotificationService {

    private final EventForumNotificationRepository notificationRepository;
    private final EventForumPostRepository postRepository;

    @Override
    public List<EventForumNotification> retrieveEventForumNotifications() {
        return notificationRepository.findAll();
    }

    @Override
    public EventForumNotification addEventForumNotification(EventForumNotificationDTO notification) {
        EventForumPost post = postRepository.findById(notification.getPostId()).orElse(null);
        EventForumNotification entity = EventForumNotification.builder()
                .post(post)
                .recipientUserId(notification.getRecipientUserId())
                .type(notification.getType() == null ? null : NotificationType.valueOf(notification.getType()))
                .build();
        return notificationRepository.save(entity);
    }

    @Override
    public EventForumNotification updateEventForumNotification(EventForumNotificationDTO notification) {
        EventForumNotification entity = notificationRepository.findById(notification.getId()).orElse(null);
        if (entity == null) {
            return null;
        }
        if (notification.getPostId() != null) {
            entity.setPost(postRepository.findById(notification.getPostId()).orElse(null));
        }
        entity.setRecipientUserId(notification.getRecipientUserId());
        entity.setType(notification.getType() == null ? null : NotificationType.valueOf(notification.getType()));
        return notificationRepository.save(entity);
    }

    @Override
    public EventForumNotification retrieveEventForumNotification(Long id) {
        return notificationRepository.findById(id).orElse(null);
    }

    @Override
    public void removeEventForumNotification(Long id) {
        notificationRepository.deleteById(id);
    }
}
