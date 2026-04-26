package tn.esprit.spring.baladna.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.transport.entity.Transport;
import tn.esprit.spring.baladna.transport.entity.TransportStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransportRepository extends JpaRepository<Transport, Long> {

    List<Transport> findByStatus(TransportStatus status);

    List<Transport> findByDepartureDateAfter(LocalDateTime date);

    List<Transport> findByAvailableSeatsGreaterThan(Integer seats);

    List<Transport> findByTrajetId(Long trajetId);

    List<Transport> findByTrajetIdAndHostEmail(Long trajetId, String email);

    Optional<Transport> findByIdAndHostEmail(Long id, String email);

    @Query(
            value = "SELECT * FROM transports WHERE id = :id FOR UPDATE",
            nativeQuery = true
    )
    Optional<Transport> findByIdForUpdate(@Param("id") Long id);

    List<Transport> findByHostEmailOrderByDepartureDateDesc(String email);

    @Query("SELECT t FROM Transport t WHERE t.departureDate > :now AND t.availableSeats > 0 AND t.status <> 'CANCELLED'")
    List<Transport> findAvailableTransports(LocalDateTime now);

    @Query("SELECT t FROM Transport t WHERE t.host.email = :email AND t.departureDate > :now AND t.availableSeats > 0 AND t.status <> 'CANCELLED'")
    List<Transport> findAvailableTransportsByHost(String email, LocalDateTime now);

    @Query("SELECT t FROM Transport t " +
            "WHERE LOWER(t.trajet.departureStation.city) = LOWER(:departureCity) " +
            "AND LOWER(t.trajet.arrivalStation.city) = LOWER(:arrivalCity)")
    List<Transport> findTransportsByCities(String departureCity, String arrivalCity);

    @Query("SELECT t FROM Transport t " +
            "WHERE t.host.email = :email " +
            "AND LOWER(t.trajet.departureStation.city) = LOWER(:departureCity) " +
            "AND LOWER(t.trajet.arrivalStation.city) = LOWER(:arrivalCity)")
    List<Transport> findTransportsByCitiesAndHost(String departureCity, String arrivalCity, String email);
}