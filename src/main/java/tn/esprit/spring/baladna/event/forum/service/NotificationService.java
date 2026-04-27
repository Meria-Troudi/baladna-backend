package tn.esprit.spring.baladna.event.forum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.baladna.event.forum.entity.ForumNotification;
import tn.esprit.spring.baladna.event.forum.entity.NotificationType;
import tn.esprit.spring.baladna.event.forum.repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository repo;

    public void notifyLike(Long postOwnerId, Long actorId, Long postId) {
        if (postOwnerId.equals(actorId)) return;
        save(postOwnerId, actorId, postId, null, NotificationType.LIKE);
    }

    public void notifyComment(Long postOwnerId, Long actorId, Long postId, Long commentId) {
        if (postOwnerId.equals(actorId)) return;
        save(postOwnerId, actorId, postId, commentId, NotificationType.COMMENT);
    }

    public void notifyReply(Long parentOwnerId, Long actorId, Long postId, Long commentId) {
        if (parentOwnerId.equals(actorId)) return;
        save(parentOwnerId, actorId, postId, commentId, NotificationType.REPLY);
    }

    private void save(Long userId, Long actorId, Long postId, Long commentId, NotificationType type) {
        ForumNotification n = new ForumNotification();
        n.setUserId(userId);
        n.setActorId(actorId);
        n.setPostId(postId);
        n.setCommentId(commentId);
        n.setType(type);
        n.setRead(false);
        n.setCreatedAt(LocalDateTime.now());
        repo.save(n);
    }

    @Transactional(readOnly = true)
    public List<ForumNotification> getNotifications(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return repo.countByUserIdAndIsReadFalse(userId);
    }

    public void markAsRead(Long id, Long userId) {
        repo.markAsRead(id, userId);
    }

    public void markAllAsRead(Long userId) {
        repo.markAllAsRead(userId);
    }
}