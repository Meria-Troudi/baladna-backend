package tn.esprit.spring.baladna.itinerary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.itinerary.entity.Settlement;
import tn.esprit.spring.baladna.itinerary.entity.enums.SettlementStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    List<Settlement> findByItineraryId(UUID itineraryId);

    // What a specific user owes to others in this itinerary
    List<Settlement> findByItineraryIdAndDebtorUserId(UUID itineraryId, Long debtorUserId);

    // What others owe to a specific user in this itinerary
    List<Settlement> findByItineraryIdAndCreditorUserId(UUID itineraryId, Long creditorUserId);

    // All pending settlements for an itinerary
    List<Settlement> findByItineraryIdAndStatus(UUID itineraryId, SettlementStatus status);

    // Delete all settlements for an itinerary (before recomputing)
    void deleteByItineraryId(UUID itineraryId);
    void deleteByItineraryIdAndStatus(UUID itineraryId, SettlementStatus status);
    Optional<Settlement> findByItineraryIdAndDebtorUserIdAndCreditorUserId(
            UUID itineraryId, Long debtorUserId, Long creditorUserId);
    // NEW: Find settlements ordered by settledAt (PAID first), then ID
    List<Settlement> findByItineraryIdOrderBySettledAtDescIdDesc(UUID itineraryId);
}