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

    Optional<Station> findByIdAndHostEmail(Long id, String email);

    List<Station> findByHostEmailOrderByCityAscNameAsc(String email);

    boolean existsByNameAndCity(String name, String city);

    boolean existsByNameAndCityAndHostEmail(String name, String city, String email);

    boolean existsByNameAndCityAndIdNot(String name, String city, Long id);

    boolean existsByNameAndCityAndIdNotAndHostEmail(String name, String city, Long id, String email);

    @Query("""
            SELECT s
            FROM Station s
            WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(s.city) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY s.city ASC, s.name ASC
            """)
    List<Station> searchByNameOrCity(String query);
}
