package tn.esprit.spring.baladna.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.transport.entity.TransportAiTripDataset;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransportAiTripDatasetRepository extends JpaRepository<TransportAiTripDataset, Long> {

    Optional<TransportAiTripDataset> findByTransportId(Long transportId);

    long countByHostEmail(String email);

    long countByHostEmailAndActualDelayMinutesIsNotNull(String email);

    long countByHostEmailAndDataOrigin(String email, String dataOrigin);

    long countByHostEmailAndDataOriginAndActualDelayMinutesIsNotNull(String email, String dataOrigin);

    List<TransportAiTripDataset> findByHostEmailOrderByDepartureDateDesc(String email);

    List<TransportAiTripDataset> findByHostEmailAndDataOriginAndActualDelayMinutesIsNotNullOrderByDepartureDateDesc(String email, String dataOrigin);

    @Modifying
    void deleteByTransportId(Long transportId);

    @Modifying
    void deleteByHostEmailAndDataOrigin(String email, String dataOrigin);

    long countByHostId(Long hostId);
}