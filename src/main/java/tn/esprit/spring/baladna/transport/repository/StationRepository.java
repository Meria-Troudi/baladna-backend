package tn.esprit.spring.baladna.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.transport.entity.Station;

import java.util.List;
import java.util.Optional;

@Repository
public interface StationRepository extends JpaRepository<Station, Long> {

    List<Station> findByCity(String city);

    List<Station> findByCityContainingIgnoreCase(String city);

    List<Station> findByDowntownTrue();

    List<Station> findBySurchargeGreaterThan(Double surcharge);

    Optional<Station> findByName(String name);

    boolean existsByNameAndCity(String name, String city);

    @Query("SELECT s FROM Station s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Station> searchByName(String name);
}