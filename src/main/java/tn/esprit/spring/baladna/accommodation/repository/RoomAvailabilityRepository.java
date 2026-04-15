package tn.esprit.spring.baladna.accommodation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.spring.baladna.accommodation.entity.RoomAvailability;

import java.util.UUID;

public interface RoomAvailabilityRepository extends JpaRepository<RoomAvailability, UUID> {

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RoomAvailability ra WHERE ra.reservation.accommodation.id = :accId")
    void deleteByAccommodationId(@Param("accId") UUID accId);
}
