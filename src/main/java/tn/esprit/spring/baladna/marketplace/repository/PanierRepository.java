package tn.esprit.spring.baladna.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.baladna.marketplace.entity.Panier;

import java.util.Optional;

public interface PanierRepository extends JpaRepository<Panier, Integer> {
    Optional<Panier> findByIdUserAndStatus(Integer idUser, String status);
}
