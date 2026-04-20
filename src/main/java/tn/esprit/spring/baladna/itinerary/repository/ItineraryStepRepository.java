package tn.esprit.spring.baladna.itinerary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.itinerary.entity.ItineraryStep;

import java.util.List;
import java.util.UUID;

@Repository
public interface ItineraryStepRepository extends JpaRepository<ItineraryStep, UUID> {

    // All steps for an itinerary ordered by position
    List<ItineraryStep> findByItineraryIdOrderByPositionAsc(UUID itineraryId);

    // Max position in an itinerary (to auto-append new steps at end)
    @Query("SELECT COALESCE(MAX(s.position), 0) FROM ItineraryStep s WHERE s.itinerary.id = :itineraryId")
    Integer findMaxPositionByItineraryId(@Param("itineraryId") UUID itineraryId);
}