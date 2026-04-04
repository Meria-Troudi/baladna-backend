package tn.esprit.spring.baladna.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.event.entity.EventMedia;

@Repository
public interface EventMediaRepository extends JpaRepository<EventMedia, Long> {
}
