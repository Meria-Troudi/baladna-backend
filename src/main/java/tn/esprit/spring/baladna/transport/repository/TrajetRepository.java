package tn.esprit.spring.baladna.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.transport.entity.Station;
import tn.esprit.spring.baladna.transport.entity.Trajet;

import java.util.List;

@Repository
public interface TrajetRepository extends JpaRepository<Trajet, Long> {

    List<Trajet> findByDepartureStation(Station station);

    List<Trajet> findByArrivalStation(Station station);

    List<Trajet> findByDepartureStationAndArrivalStation(Station departure, Station arrival);

    List<Trajet> findByDistanceKmLessThan(Double distance);

    @Query("SELECT t FROM Trajet t WHERE LOWER(t.departureStation.city) = LOWER(:city)")
    List<Trajet> findTrajetsByDepartureCity(String city);
}