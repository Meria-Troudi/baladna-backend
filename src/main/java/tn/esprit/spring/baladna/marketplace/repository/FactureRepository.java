package tn.esprit.spring.baladna.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.baladna.marketplace.entity.Facture;

import java.util.Optional;

public interface FactureRepository extends JpaRepository<Facture, Long> {
    Optional<Facture> findByNumeroFacture(String numeroFacture);
}