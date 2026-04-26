package tn.esprit.spring.baladna.marketplace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.baladna.marketplace.dto.request.FavorisRequest;
import tn.esprit.spring.baladna.marketplace.dto.response.FavorisResponse;
import tn.esprit.spring.baladna.marketplace.entity.Favoris;
import tn.esprit.spring.baladna.marketplace.entity.Product;
import tn.esprit.spring.baladna.marketplace.exception.BusinessException;
import tn.esprit.spring.baladna.marketplace.exception.ResourceNotFoundException;
import tn.esprit.spring.baladna.marketplace.repository.FavorisRepository;
import tn.esprit.spring.baladna.marketplace.repository.ProductRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavorisService {

    private final FavorisRepository favorisRepository;
    private final ProductRepository productRepository;

    public FavorisResponse addFavoris(FavorisRequest request) {
        Product product = productRepository.findById(request.getIdProduit())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produit non trouvé avec l'id: " + request.getIdProduit()));

        if (favorisRepository.existsByIdUserAndIdProduit(request.getIdUser(), request.getIdProduit())) {
            throw new BusinessException("Ce produit est déjà dans vos favoris");
        }

        Favoris favoris = Favoris.builder()
                .idUser(request.getIdUser())
                .idProduit(request.getIdProduit())
                .build();

        Favoris saved = favorisRepository.save(favoris);
        return toResponse(saved, product);
    }

    @Transactional
    public void removeFavoris(FavorisRequest request) {
        if (!favorisRepository.existsByIdUserAndIdProduit(request.getIdUser(), request.getIdProduit())) {
            throw new ResourceNotFoundException("Favori non trouvé");
        }
        favorisRepository.deleteByIdUserAndIdProduit(request.getIdUser(), request.getIdProduit());
    }

    public List<FavorisResponse> getFavorisByUser(Integer idUser) {
        return favorisRepository.findByIdUser(idUser)
                .stream()
                .map(favoris -> {
                    Product product = productRepository.findById(favoris.getIdProduit())
                            .orElse(null);
                    return toResponse(favoris, product);
                })
                .collect(Collectors.toList());
    }

    private FavorisResponse toResponse(Favoris favoris, Product product) {
        FavorisResponse response = new FavorisResponse();
        response.setIdFavoris(favoris.getIdFavoris());
        response.setIdUser(favoris.getIdUser());
        response.setIdProduit(favoris.getIdProduit());
        response.setDateCreation(favoris.getDateCreation());
        if (product != null) {
            response.setNomProduit(product.getNomProduit());
            response.setPrixProduit(product.getPrixProduit());
            response.setImageProduit(product.getImageProduit());
        }
        return response;
    }
}
