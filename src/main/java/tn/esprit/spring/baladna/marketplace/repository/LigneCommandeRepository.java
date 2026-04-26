package tn.esprit.spring.baladna.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.baladna.marketplace.entity.LigneCommande;

import java.util.List;

public interface LigneCommandeRepository extends JpaRepository<LigneCommande, Integer> {
    List<LigneCommande> findByIdCommande(Long idCommande);
    List<LigneCommande> findByIdArtisanOrderByIdCommandeDesc(Integer idArtisan);


}
