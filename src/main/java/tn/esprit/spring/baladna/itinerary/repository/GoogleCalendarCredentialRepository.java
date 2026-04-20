package tn.esprit.spring.baladna.itinerary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.itinerary.entity.GoogleCalendarCredential;

import java.util.Optional;

@Repository
public interface GoogleCalendarCredentialRepository extends JpaRepository<GoogleCalendarCredential, Long> {
    Optional<GoogleCalendarCredential> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
    void deleteByUserId(Long userId);
}
