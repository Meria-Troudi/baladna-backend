package tn.esprit.spring.baladna.marketplace.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.baladna.marketplace.entity.Favoris;

import java.util.List;
import java.util.Optional;

public interface FavorisRepository extends JpaRepository<Favoris, Integer> {
    List<Favoris> findByIdUser(Integer idUser);
    Optional<Favoris> findByIdUserAndIdProduit(Integer idUser, Integer idProduit);
    boolean existsByIdUserAndIdProduit(Integer idUser, Integer idProduit);
    void deleteByIdUserAndIdProduit(Integer idUser, Integer idProduit);
}
