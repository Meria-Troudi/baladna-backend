package tn.esprit.spring.baladna.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.baladna.marketplace.entity.Product;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {
    List<Product> findByIdCategorie(Integer idCategorie);
    List<Product> findByNomProduitContainingIgnoreCase(String keyword);

    // ✅ AJOUTER CETTE MÉTHODE
    List<Product> findByIdArtisan(Integer idArtisan);

}
