package tn.esprit.spring.baladna.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.transport.entity.Transport;
import tn.esprit.spring.baladna.transport.entity.TransportStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransportRepository extends JpaRepository<Transport, Long> {

    List<Transport> findByStatus(TransportStatus status);

    List<Transport> findByDepartureDateAfter(LocalDateTime date);

    List<Transport> findByAvailableSeatsGreaterThan(Integer seats);

    List<Transport> findByTrajetId(Long trajetId);

    @Query("SELECT t FROM Transport t WHERE t.departureDate > :now AND t.availableSeats > 0 AND t.status <> 'CANCELLED'")
    List<Transport> findAvailableTransports(LocalDateTime now);

    @Query("SELECT t FROM Transport t " +
            "WHERE LOWER(t.trajet.departureStation.city) = LOWER(:departureCity) " +
            "AND LOWER(t.trajet.arrivalStation.city) = LOWER(:arrivalCity)")
    List<Transport> findTransportsByCities(String departureCity, String arrivalCity);
}