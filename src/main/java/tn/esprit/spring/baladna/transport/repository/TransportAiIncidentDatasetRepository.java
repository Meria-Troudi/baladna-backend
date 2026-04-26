package tn.esprit.spring.baladna.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.transport.entity.TransportAiIncidentDataset;

import java.util.Optional;

@Repository
public interface TransportAiIncidentDatasetRepository extends JpaRepository<TransportAiIncidentDataset, Long> {

    long countByHostEmail(String email);

    Optional<TransportAiIncidentDataset> findTopByTransportIdAndIncidentTypeAndIncidentSeverityAndResolvedAtIsNullOrderByCreatedAtDesc(
            Long transportId,
            String incidentType,
            String incidentSeverity
    );

    void deleteByTransportId(Long transportId);
}
