package tn.esprit.spring.baladna.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.event.entity.EventForumNotification;

@Repository
public interface EventForumNotificationRepository extends JpaRepository<EventForumNotification, Long> {
}
