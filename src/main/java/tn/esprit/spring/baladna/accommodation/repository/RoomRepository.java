package tn.esprit.spring.baladna.accommodation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.baladna.accommodation.entity.Room;

import java.util.List;
import java.util.UUID;

public interface RoomRepository extends JpaRepository<Room, UUID> {

    List<Room> findByAccommodation_IdOrderByPricePerNightAsc(UUID accommodationId);
}
