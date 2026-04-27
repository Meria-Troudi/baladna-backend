package tn.esprit.spring.baladna.marketplace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.baladna.marketplace.dto.request.ProductRequest;
import tn.esprit.spring.baladna.marketplace.dto.response.ProductResponse;
import tn.esprit.spring.baladna.marketplace.entity.Product;
import tn.esprit.spring.baladna.marketplace.exception.ResourceNotFoundException;
import tn.esprit.spring.baladna.marketplace.mapper.ProductMapper;
import tn.esprit.spring.baladna.marketplace.repository.ProductImageRepository;
import tn.esprit.spring.baladna.marketplace.repository.ProductRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductMapper productMapper;

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(product -> {
                    ProductResponse response = productMapper.toResponse(product);
                    List<String> images = productImageRepository
                            .findByIdProduitOrderByDisplayOrderAsc(product.getIdProduit())
                            .stream()
                            .map(img -> img.getImageUrl())
                            .collect(Collectors.toList());
                    response.setImages(images);
                    return response;
                })
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé avec l'id: " + id));

        ProductResponse response = productMapper.toResponse(product);
        List<String> images = productImageRepository
                .findByIdProduitOrderByDisplayOrderAsc(id)
                .stream()
                .map(img -> img.getImageUrl())
                .collect(Collectors.toList());
        response.setImages(images);
        return response;
    }

    // ✅ NOUVELLE MÉTHODE : Récupérer les produits d'un artisan spécifique
    public List<ProductResponse> getProductsByArtisan(Integer idArtisan) {
        return productRepository.findByIdArtisan(idArtisan)
                .stream()
                .map(product -> {
                    ProductResponse response = productMapper.toResponse(product);
                    List<String> images = productImageRepository
                            .findByIdProduitOrderByDisplayOrderAsc(product.getIdProduit())
                            .stream()
                            .map(img -> img.getImageUrl())
                            .collect(Collectors.toList());
                    response.setImages(images);
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Product product = productMapper.toEntity(request);
        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Integer id, ProductRequest request, Integer currentArtisanId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé avec l'id: " + id));

        // ✅ Vérifier que l'artisan est bien le propriétaire
        if (!product.getIdArtisan().equals(currentArtisanId)) {
            throw new RuntimeException("Accès refusé : vous ne pouvez pas modifier ce produit");
        }

        productMapper.updateEntityFromRequest(product, request);
        Product updated = productRepository.save(product);
        return productMapper.toResponse(updated);
    }

    @Transactional
    public void deleteProduct(Integer id, Integer currentArtisanId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé avec l'id: " + id));

        // ✅ Vérifier que l'artisan est bien le propriétaire
        if (!product.getIdArtisan().equals(currentArtisanId)) {
            throw new RuntimeException("Accès refusé : vous ne pouvez pas supprimer ce produit");
        }

        productImageRepository.deleteByIdProduit(id);
        productRepository.deleteById(id);
    }

    public List<ProductResponse> getProductsByCategorie(Integer idCategorie) {
        return productRepository.findByIdCategorie(idCategorie)
                .stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }
}