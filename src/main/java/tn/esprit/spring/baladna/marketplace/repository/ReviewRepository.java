package tn.esprit.spring.baladna.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.baladna.marketplace.entity.Review;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Integer> {
    List<Review> findByIdProduit(Integer idProduit);
    boolean existsByIdUserAndIdProduit(Integer idUser, Integer idProduit);
}
