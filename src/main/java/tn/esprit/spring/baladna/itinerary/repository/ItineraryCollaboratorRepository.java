package tn.esprit.spring.baladna.itinerary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.itinerary.entity.ItineraryCollaborator;
import tn.esprit.spring.baladna.itinerary.entity.enums.CollaboratorStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItineraryCollaboratorRepository extends JpaRepository<ItineraryCollaborator, UUID> {

    // All collaborators of an itinerary with a given status
    List<ItineraryCollaborator> findByItineraryIdAndStatus(UUID itineraryId, CollaboratorStatus status);

    // All collaborators of an itinerary (any status)
    List<ItineraryCollaborator> findByItineraryId(UUID itineraryId);

    // Check if a user already has any entry for this itinerary
    Optional<ItineraryCollaborator> findByItineraryIdAndUserId(UUID itineraryId, Long userId);

    // Count active collaborators for settlement share calculation
    long countByItineraryIdAndStatus(UUID itineraryId, CollaboratorStatus status);
}