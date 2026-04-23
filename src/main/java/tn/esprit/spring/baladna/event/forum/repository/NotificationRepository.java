package tn.esprit.spring.baladna.event.forum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.event.forum.entity.ForumNotification;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<ForumNotification, Long> {
    List<ForumNotification> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserIdAndIsReadFalse(Long userId);
    @Modifying
    @Query("UPDATE ForumNotification n SET n.isRead = true WHERE n.id = :id AND n.userId = :userId")
    void markAsRead(@Param("id") Long id, @Param("userId") Long userId);

    @Modifying
    @Query("UPDATE ForumNotification n SET n.isRead = true WHERE n.userId = :userId")
    void markAllAsRead(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM ForumNotification n WHERE n.postId = :postId")
    int deleteAllByPostId(@Param("postId") Long postId);
}