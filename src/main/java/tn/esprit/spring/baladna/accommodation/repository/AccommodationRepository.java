package tn.esprit.spring.baladna.accommodation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.spring.baladna.accommodation.entity.Accommodation;
import tn.esprit.spring.baladna.accommodation.entity.enums.AccommodationStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccommodationRepository extends JpaRepository<Accommodation, UUID> {

    @Query("SELECT DISTINCT a FROM Accommodation a LEFT JOIN FETCH a.rooms WHERE a.hostId = :hostId ORDER BY a.createdAt DESC")
    List<Accommodation> findByHostIdWithRooms(@Param("hostId") Long hostId);

    @Query("SELECT DISTINCT a FROM Accommodation a LEFT JOIN FETCH a.rooms WHERE a.status = :status ORDER BY a.title")
    List<Accommodation> findByStatusWithRooms(@Param("status") AccommodationStatus status);

    @Query("SELECT DISTINCT a FROM Accommodation a LEFT JOIN FETCH a.rooms WHERE a.status IN :statuses ORDER BY a.title")
    List<Accommodation> findByStatusInWithRooms(@Param("statuses") Collection<AccommodationStatus> statuses);

    @Query("SELECT DISTINCT a FROM Accommodation a LEFT JOIN FETCH a.rooms WHERE a.id = :id")
    Optional<Accommodation> findByIdWithRooms(@Param("id") UUID id);

    @Query("SELECT DISTINCT a FROM Accommodation a LEFT JOIN FETCH a.rooms WHERE a.id = :id AND a.hostId = :hostId")
    Optional<Accommodation> findByIdAndHostIdWithRooms(@Param("id") UUID id, @Param("hostId") Long hostId);
}
