package tn.esprit.spring.baladna.marketplace.mapper;

import org.springframework.stereotype.Component;
import tn.esprit.spring.baladna.marketplace.dto.request.ProductRequest;
import tn.esprit.spring.baladna.marketplace.dto.response.ProductResponse;
import tn.esprit.spring.baladna.marketplace.entity.Product;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request) {
        return Product.builder()
                .nomProduit(request.getNomProduit())
                .descriptionProduit(request.getDescriptionProduit())
                .imageProduit(request.getImageProduit())
                .prixProduit(request.getPrixProduit())
                .stockProduit(request.getStockProduit())
                .idCategorie(request.getIdCategorie())
                .idArtisan(request.getIdArtisan())
                .build();
    }

    public ProductResponse toResponse(Product product) {
        ProductResponse response = new ProductResponse();
        response.setIdProduit(product.getIdProduit());
        response.setNomProduit(product.getNomProduit());
        response.setDescriptionProduit(product.getDescriptionProduit());
        response.setImageProduit(product.getImageProduit());
        response.setPrixProduit(product.getPrixProduit());
        response.setStockProduit(product.getStockProduit());
        response.setDateCreation(product.getDateCreation());
        response.setUpdatedAt(product.getUpdatedAt());
        response.setIdCategorie(product.getIdCategorie());
        response.setIdArtisan(product.getIdArtisan());
        return response;
    }

    public void updateEntityFromRequest(Product product, ProductRequest request) {
        product.setNomProduit(request.getNomProduit());
        product.setDescriptionProduit(request.getDescriptionProduit());
        product.setImageProduit(request.getImageProduit());
        product.setPrixProduit(request.getPrixProduit());
        product.setStockProduit(request.getStockProduit());
        product.setIdCategorie(request.getIdCategorie());
        product.setIdArtisan(request.getIdArtisan());
    }
}
