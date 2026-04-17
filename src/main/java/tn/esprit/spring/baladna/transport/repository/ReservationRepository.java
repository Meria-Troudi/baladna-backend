package tn.esprit.spring.baladna.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.transport.entity.Reservation;
import tn.esprit.spring.baladna.transport.entity.ReservationStatus;
import tn.esprit.spring.baladna.user.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByUser(User user);

    List<Reservation> findByUserEmail(String email);

    List<Reservation> findByTransportId(Long transportId);

    List<Reservation> findByTransportIdAndTransportHostEmail(Long transportId, String email);

    List<Reservation> findByTransportHostEmail(String email);

    Optional<Reservation> findByIdAndTransportHostEmail(Long id, String email);

    List<Reservation> findByStatus(ReservationStatus status);

    List<Reservation> findByUserAndStatus(User user, ReservationStatus status);

    boolean existsByUserIdAndTransportId(Long userId, Long transportId);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.transport.id = :transportId AND r.status <> 'CANCELLED'")
    Integer countActiveReservationsByTransport(Long transportId);
}
