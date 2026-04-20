package tn.esprit.spring.baladna.itinerary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.itinerary.entity.Itinerary;
import tn.esprit.spring.baladna.itinerary.entity.enums.ItineraryStatus;
import tn.esprit.spring.baladna.itinerary.entity.enums.Visibility;

import java.util.List;
import java.util.UUID;

@Repository
public interface ItineraryRepository extends JpaRepository<Itinerary, UUID> {

    // All itineraries owned by a user
    List<Itinerary> findByOwnerId(Long ownerId);

    // All PUBLIC itineraries (for browse/discovery)
    List<Itinerary> findByVisibility(Visibility visibility);

    // All PUBLIC itineraries with a specific status
    List<Itinerary> findByVisibilityAndStatus(Visibility visibility, ItineraryStatus status);

    // Itineraries where a user is an ACTIVE collaborator (not owner)
    @Query("""
            SELECT i FROM Itinerary i
            JOIN i.collaborators c
            WHERE c.userId = :userId
            AND c.status = 'ACTIVE'
            """)
    List<Itinerary> findByActiveCollaboratorUserId(@Param("userId") Long userId);

    // All itineraries visible to a user (owns OR is active collaborator)
    @Query("""
            SELECT DISTINCT i FROM Itinerary i
            LEFT JOIN i.collaborators c
            WHERE i.ownerId = :userId
            OR (c.userId = :userId AND c.status = 'ACTIVE')
            """)
    List<Itinerary> findAllAccessibleByUser(@Param("userId") Long userId);
}