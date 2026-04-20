package tn.esprit.spring.baladna.itinerary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.itinerary.entity.SettlementSummary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettlementSummaryRepository extends JpaRepository<SettlementSummary, UUID> {

    List<SettlementSummary> findByItineraryId(UUID itineraryId);

    Optional<SettlementSummary> findByItineraryIdAndUserId(UUID itineraryId, Long userId);

    void deleteByItineraryId(UUID itineraryId);
}