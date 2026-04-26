package tn.esprit.spring.baladna.marketplace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.marketplace.dto.request.CategorieRequest;
import tn.esprit.spring.baladna.marketplace.entity.Categorie;
import tn.esprit.spring.baladna.marketplace.exception.ResourceNotFoundException;
import tn.esprit.spring.baladna.marketplace.repository.CategorieRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategorieService {

    private final CategorieRepository categorieRepository;

    public List<Categorie> getAllCategories() {
        return categorieRepository.findAll();
    }

    public Categorie getCategorieById(Integer id) {
        return categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie non trouvée avec l'id: " + id));
    }

    public Categorie createCategorie(CategorieRequest request) {
        Categorie categorie = Categorie.builder()
                .nomCategorie(request.getNomCategorie())
                .build();
        return categorieRepository.save(categorie);
    }

    public Categorie updateCategorie(Integer id, CategorieRequest request) {
        Categorie categorie = getCategorieById(id);
        categorie.setNomCategorie(request.getNomCategorie());
        return categorieRepository.save(categorie);
    }

    public void deleteCategorie(Integer id) {
        if (!categorieRepository.existsById(id)) {
            throw new ResourceNotFoundException("Catégorie non trouvée avec l'id: " + id);
        }
        categorieRepository.deleteById(id);
    }
}
