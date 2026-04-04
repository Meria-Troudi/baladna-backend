package tn.esprit.spring.baladna.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.event.entity.EventReservation;

@Repository
public interface EventReservationRepository extends JpaRepository<EventReservation, Long> {
}
