package tn.esprit.spring.baladna.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.marketplace.entity.Commande;

import java.util.List;

@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {
    List<Commande> findByUserIdOrderByCreatedAtDesc(Long userId);
}



/*
@Repository
public interface CommandeRepository extends JpaRepository<Commande, Long> {
    List<Commande> findByUserIdOrderByCreatedAtDesc(Long userId);
}
 */