package tn.esprit.spring.baladna.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.transport.entity.Station;
import tn.esprit.spring.baladna.transport.entity.Trajet;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrajetRepository extends JpaRepository<Trajet, Long> {

    List<Trajet> findByDepartureStation(Station station);

    List<Trajet> findByArrivalStation(Station station);

    @Query("SELECT t FROM Trajet t WHERE t.departureStation = :station OR t.arrivalStation = :station")
    List<Trajet> findLinkedToStation(Station station);

    List<Trajet> findByDepartureStationAndArrivalStation(Station departure, Station arrival);

    List<Trajet> findByDistanceKmLessThan(Double distance);

    Optional<Trajet> findByIdAndHostEmail(Long id, String email);

    List<Trajet> findByHostEmailOrderByIdDesc(String email);

    @Query("SELECT t FROM Trajet t WHERE t.host.email = :email AND t.departureStation = :station")
    List<Trajet> findByHostEmailAndDepartureStation(String email, Station station);

    @Query("SELECT t FROM Trajet t WHERE t.host.email = :email AND t.arrivalStation = :station")
    List<Trajet> findByHostEmailAndArrivalStation(String email, Station station);

    @Query("SELECT t FROM Trajet t WHERE t.host.email = :email AND t.departureStation = :departure AND t.arrivalStation = :arrival")
    List<Trajet> findByHostEmailAndDepartureStationAndArrivalStation(String email, Station departure, Station arrival);

    @Query("SELECT t FROM Trajet t WHERE LOWER(t.departureStation.city) = LOWER(:city)")
    List<Trajet> findTrajetsByDepartureCity(String city);
}
