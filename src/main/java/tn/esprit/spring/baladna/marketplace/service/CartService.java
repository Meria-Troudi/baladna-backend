package tn.esprit.spring.baladna.marketplace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.baladna.marketplace.dto.request.CartItemRequest;
import tn.esprit.spring.baladna.marketplace.dto.response.CartResponse;
import tn.esprit.spring.baladna.marketplace.entity.Panier;
import tn.esprit.spring.baladna.marketplace.entity.PanierItem;
import tn.esprit.spring.baladna.marketplace.entity.Product;
import tn.esprit.spring.baladna.marketplace.exception.BusinessException;
import tn.esprit.spring.baladna.marketplace.exception.ResourceNotFoundException;
import tn.esprit.spring.baladna.marketplace.repository.PanierItemRepository;
import tn.esprit.spring.baladna.marketplace.repository.PanierRepository;
import tn.esprit.spring.baladna.marketplace.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final PanierRepository panierRepository;
    private final PanierItemRepository panierItemRepository;
    private final ProductRepository productRepository;

    /**
     * Récupère ou crée un panier actif pour un utilisateur
     */
    public Panier getOrCreateActiveCart(Integer idUser) {
        return panierRepository.findByIdUserAndStatus(idUser, "ACTIVE")
                .orElseGet(() -> panierRepository.save(
                        Panier.builder()
                                .idUser(idUser)
                                .status("ACTIVE")
                                .build()
                ));
    }

    /**
     * Retourne le panier complet avec les items et le total
     */
    public CartResponse getCartByUser(Integer idUser) {
        Panier panier = getOrCreateActiveCart(idUser);
        return buildCartResponse(panier);
    }

    /**
     * Ajoute un produit au panier (ou augmente la quantité si déjà présent)
     */
    @Transactional
    public CartResponse addItem(CartItemRequest request) {
        Product product = productRepository.findById(request.getIdProduit())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Produit non trouvé avec l'id: " + request.getIdProduit()));

        if (product.getStockProduit() < request.getQuantite()) {
            throw new BusinessException("Stock insuffisant pour le produit: " + product.getNomProduit());
        }

        Panier panier = getOrCreateActiveCart(request.getIdUser());

        // Si l'item existe déjà → on met à jour la quantité
        panierItemRepository.findByIdPanierAndIdProduit(panier.getIdPanier(), request.getIdProduit())
                .ifPresentOrElse(
                        existingItem -> {
                            int newQty = existingItem.getQuantite() + request.getQuantite();
                            if (product.getStockProduit() < newQty) {
                                throw new BusinessException("Stock insuffisant. Disponible: " + product.getStockProduit());
                            }
                            existingItem.setQuantite(newQty);
                            panierItemRepository.save(existingItem);
                        },
                        () -> panierItemRepository.save(
                                PanierItem.builder()
                                        .idPanier(panier.getIdPanier())
                                        .idProduit(request.getIdProduit())
                                        .quantite(request.getQuantite())
                                        .prix(product.getPrixProduit())
                                        .build()
                        )
                );

        return buildCartResponse(panier);
    }

    /**
     * Met à jour la quantité d'un item existant
     */
    @Transactional
    public CartResponse updateItem(CartItemRequest request) {
        Panier panier = panierRepository.findByIdUserAndStatus(request.getIdUser(), "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException("Aucun panier actif trouvé"));

        PanierItem item = panierItemRepository
                .findByIdPanierAndIdProduit(panier.getIdPanier(), request.getIdProduit())
                .orElseThrow(() -> new ResourceNotFoundException("Article non trouvé dans le panier"));

        Product product = productRepository.findById(request.getIdProduit())
                .orElseThrow(() -> new ResourceNotFoundException("Produit non trouvé"));

        if (product.getStockProduit() < request.getQuantite()) {
            throw new BusinessException("Stock insuffisant. Disponible: " + product.getStockProduit());
        }

        item.setQuantite(request.getQuantite());
        panierItemRepository.save(item);

        return buildCartResponse(panier);
    }

    /**
     * Supprime un article du panier
     */
    @Transactional
    public CartResponse removeItem(CartItemRequest request) {
        Panier panier = panierRepository.findByIdUserAndStatus(request.getIdUser(), "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException("Aucun panier actif trouvé"));

        panierItemRepository.deleteByIdPanierAndIdProduit(panier.getIdPanier(), request.getIdProduit());

        return buildCartResponse(panier);
    }

    /**
     * Construit la réponse du panier avec les détails de chaque item
     */
    private CartResponse buildCartResponse(Panier panier) {
        List<PanierItem> items = panierItemRepository.findByIdPanier(panier.getIdPanier());

        List<CartResponse.CartItemResponse> itemResponses = items.stream().map(item -> {
            CartResponse.CartItemResponse itemResp = new CartResponse.CartItemResponse();
            itemResp.setIdCartItem(item.getIdCartItem());
            itemResp.setIdProduit(item.getIdProduit());
            itemResp.setQuantite(item.getQuantite());
            itemResp.setPrix(item.getPrix());
            itemResp.setSousTotal(item.getPrix().multiply(BigDecimal.valueOf(item.getQuantite())));

            // Enrichir avec le nom et l'image du produit
            productRepository.findById(item.getIdProduit()).ifPresent(p -> {
                itemResp.setNomProduit(p.getNomProduit());
                itemResp.setImageProduit(p.getImageProduit());
            });

            return itemResp;
        }).collect(Collectors.toList());

        BigDecimal total = itemResponses.stream()
                .map(CartResponse.CartItemResponse::getSousTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        CartResponse response = new CartResponse();
        response.setIdPanier(panier.getIdPanier());
        response.setIdUser(panier.getIdUser());
        response.setStatus(panier.getStatus());
        response.setDateCreation(panier.getDateCreation());
        response.setLastUpdated(panier.getLastUpdated());
        response.setItems(itemResponses);
        response.setTotal(total);

        return response;
    }
}
