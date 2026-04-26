package tn.esprit.spring.baladna.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.transport.entity.TransportAiDelayModel;

import java.util.Optional;

@Repository
public interface TransportAiDelayModelRepository extends JpaRepository<TransportAiDelayModel, Long> {

    Optional<TransportAiDelayModel> findTopByHostEmailAndActiveTrueOrderByTrainedAtDesc(String email);

    long countByHostEmailAndActiveTrue(String email);

    @Modifying
    @Query("UPDATE TransportAiDelayModel model SET model.active = false WHERE model.host.email = :email AND model.active = true")
    void deactivateActiveModels(String email);
}
