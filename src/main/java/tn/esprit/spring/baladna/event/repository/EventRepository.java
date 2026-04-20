package tn.esprit.spring.baladna.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.event.entity.Event;
import tn.esprit.spring.baladna.event.entity.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByCreatedByUserId(Long createdByUserId);
    List<Event> findByStatus(EventStatus status);
    List<Event> findByStatusAndStartAtAfter(EventStatus status, LocalDateTime startAt);
    
    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.media WHERE e.status = :status AND e.startAt > :now ORDER BY e.startAt ASC")
    List<Event> findUpcomingEventsWithMedia(EventStatus status, LocalDateTime now);
    
    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.media WHERE e.id IN :eventIds")
    List<Event> findEventsWithMediaByIds(List<Long> eventIds);
}
