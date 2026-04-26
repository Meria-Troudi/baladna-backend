package tn.esprit.spring.baladna.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import tn.esprit.spring.baladna.marketplace.entity.PanierItem;

import java.util.List;
import java.util.Optional;

public interface PanierItemRepository extends JpaRepository<PanierItem, Integer> {
    List<PanierItem> findByIdPanier(Integer idPanier);
    Optional<PanierItem> findByIdPanierAndIdProduit(Integer idPanier, Integer idProduit);

    @Modifying
    void deleteByIdPanierAndIdProduit(Integer idPanier, Integer idProduit);

    @Modifying
    void deleteByIdPanier(Integer idPanier);
}
