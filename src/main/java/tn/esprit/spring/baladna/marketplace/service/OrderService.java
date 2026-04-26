package tn.esprit.spring.baladna.marketplace.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.baladna.marketplace.dto.request.CheckoutRequest;
import tn.esprit.spring.baladna.marketplace.dto.response.OrderResponse;
import tn.esprit.spring.baladna.marketplace.entity.Commande;
import tn.esprit.spring.baladna.marketplace.entity.LigneCommande;
import tn.esprit.spring.baladna.marketplace.entity.Panier;
import tn.esprit.spring.baladna.marketplace.entity.PanierItem;
import tn.esprit.spring.baladna.marketplace.entity.Product;
import tn.esprit.spring.baladna.marketplace.entity.StatutCommande;
import tn.esprit.spring.baladna.marketplace.exception.BusinessException;
import tn.esprit.spring.baladna.marketplace.exception.ResourceNotFoundException;
import tn.esprit.spring.baladna.marketplace.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final PanierRepository panierRepository;
    private final PanierItemRepository panierItemRepository;
    private final CommandeRepository commandeRepository;
    private final LigneCommandeRepository ligneCommandeRepository;
    private final ProductRepository productRepository;

    private String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null) return null;
        if (paymentMethod.equalsIgnoreCase("CASH_ON_DELIVERY") ||
                paymentMethod.equalsIgnoreCase("CASH") ||
                paymentMethod.contains("livraison")) {
            return "CASH_ON_DELIVERY";
        }
        if (paymentMethod.equalsIgnoreCase("CARD") ||
                paymentMethod.contains("carte")) {
            return "CARD";
        }
        return paymentMethod;
    }

    @Transactional
    public OrderResponse checkout(CheckoutRequest request) {
        log.info("Starting checkout for user: {}, raw paymentMethod: {}",
                request.getUserId(), request.getPaymentMethod());

        String normalizedPaymentMethod = normalizePaymentMethod(request.getPaymentMethod());
        log.info("Normalized payment method: {}", normalizedPaymentMethod);

        Panier panier = panierRepository.findByIdUserAndStatus(request.getUserId().intValue(), "ACTIVE")
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active cart found for user: " + request.getUserId()));

        List<PanierItem> items = panierItemRepository.findByIdPanier(panier.getIdPanier());
        if (items.isEmpty()) {
            throw new BusinessException("Cart is empty, cannot checkout");
        }

        BigDecimal total = items.stream()
                .map(item -> item.getPrix().multiply(BigDecimal.valueOf(item.getQuantite())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String transactionId;
        StatutCommande orderStatut;

        if ("CASH_ON_DELIVERY".equals(normalizedPaymentMethod)) {
            transactionId = "COD-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 12);
            orderStatut = StatutCommande.PENDING;
            log.info("Cash on delivery - transactionId: {}", transactionId);
        } else if ("CARD".equals(normalizedPaymentMethod)) {
            transactionId = "STRIPE-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 12);
            orderStatut = StatutCommande.CONFIRMED;
            log.info("Stripe card payment - transactionId: {}", transactionId);
        } else {
            throw new BusinessException("Unsupported payment method: " + request.getPaymentMethod());
        }

        Commande commande = new Commande();
        commande.setUserId(request.getUserId());
        commande.setTotal(total.doubleValue());
        commande.setStatut(orderStatut);
        commande.setTransactionId(transactionId);
        commande.setPaymentMethod(request.getPaymentMethod());
        commande.setAdresseLivraison(request.getAdresseLivraison());
        commande.setTelephone(request.getTelephone());
        commande.setNomClient(request.getNomClient());
        commande.setCreatedAt(LocalDateTime.now());
        commande.setUpdatedAt(LocalDateTime.now());

        Commande savedCommande = commandeRepository.save(commande);
        log.info("Order created - ID: {}, statut: {}, transactionId: {}",
                savedCommande.getId(), savedCommande.getStatut(), savedCommande.getTransactionId());

        // Update stock and remove out-of-stock products
        for (PanierItem item : items) {
            Product product = productRepository.findById(item.getIdProduit())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + item.getIdProduit()));

            int currentStock = product.getStockProduit();
            int quantityOrdered = item.getQuantite();

            if (currentStock < quantityOrdered) {
                throw new BusinessException("Insufficient stock: " + product.getNomProduit());
            }

            int newStock = currentStock - quantityOrdered;
            product.setStockProduit(newStock);
            productRepository.save(product);
            log.info("Stock updated - Product ID: {}, new stock: {}", product.getIdProduit(), newStock);

            // 🗑️ Supprimer automatiquement si stock = 0
            if (product.getStockProduit() <= 0) {
                productRepository.delete(product);
                log.info("🗑️ Product '{}' (ID: {}) removed - out of stock", product.getNomProduit(), product.getIdProduit());
            }
        }

        List<LigneCommande> lignes = items.stream().map(item -> {
            Integer idArtisan = productRepository.findById(item.getIdProduit())
                    .map(product -> product.getIdArtisan() != null ? product.getIdArtisan() : 0)
                    .orElse(0);

            LigneCommande ligne = new LigneCommande();
            ligne.setIdCommande(savedCommande.getId().intValue());
            ligne.setIdProduit(item.getIdProduit());
            ligne.setIdArtisan(idArtisan);
            ligne.setQuantite(item.getQuantite());
            ligne.setPrix(item.getPrix());
            return ligneCommandeRepository.save(ligne);
        }).collect(Collectors.toList());

        panier.setStatus("ORDERED");
        panierRepository.save(panier);
        panierItemRepository.deleteByIdPanier(panier.getIdPanier());

        return buildOrderResponse(savedCommande, lignes);
    }

    public List<OrderResponse> getOrdersByUser(Long idUser) {
        List<Commande> commandes = commandeRepository.findByUserIdOrderByCreatedAtDesc(idUser);
        return commandes.stream()
                .map(commande -> {
                    List<LigneCommande> lignes = ligneCommandeRepository.findByIdCommande(commande.getId());
                    return buildOrderResponse(commande, lignes);
                })
                .collect(Collectors.toList());
    }

    public OrderResponse getOrderById(Long idCommande) {
        Commande commande = commandeRepository.findById(idCommande)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + idCommande));
        List<LigneCommande> lignes = ligneCommandeRepository.findByIdCommande(idCommande);
        return buildOrderResponse(commande, lignes);
    }

    public List<OrderResponse> getOrdersByArtisan(Integer idArtisan) {
        List<LigneCommande> artisanLines = ligneCommandeRepository.findByIdArtisanOrderByIdCommandeDesc(idArtisan);
        Map<Integer, List<LigneCommande>> groupedByOrder = artisanLines.stream()
                .collect(Collectors.groupingBy(LigneCommande::getIdCommande, LinkedHashMap::new, Collectors.toList()));
        return groupedByOrder.entrySet().stream().map(entry -> {
            Commande commande = commandeRepository.findById(entry.getKey().longValue())
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + entry.getKey()));
            OrderResponse response = buildOrderResponse(commande, entry.getValue());
            BigDecimal artisanTotal = entry.getValue().stream()
                    .map(line -> line.getPrix().multiply(BigDecimal.valueOf(line.getQuantite())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            response.setTotal(artisanTotal);
            return response;
        }).collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateStatut(Long idCommande, StatutCommande newStatut) {
        Commande commande = commandeRepository.findById(idCommande)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + idCommande));
        validateTransition(commande.getStatut(), newStatut);
        commande.setStatut(newStatut);
        commande.setUpdatedAt(LocalDateTime.now());
        Commande updated = commandeRepository.save(commande);
        List<LigneCommande> lignes = ligneCommandeRepository.findByIdCommande(idCommande);
        return buildOrderResponse(updated, lignes);
    }

    private void validateTransition(StatutCommande current, StatutCommande next) {
        Map<StatutCommande, StatutCommande> transitions = Map.of(
                StatutCommande.CONFIRMED, StatutCommande.PREPARING,
                StatutCommande.PREPARING, StatutCommande.SHIPPED,
                StatutCommande.SHIPPED, StatutCommande.IN_TRANSIT,
                StatutCommande.IN_TRANSIT, StatutCommande.DELIVERED
        );
        StatutCommande expected = transitions.get(current);
        if (expected == null || !expected.equals(next)) {
            throw new BusinessException("Invalid transition: " + current + " → " + next);
        }
    }

    private OrderResponse buildOrderResponse(Commande commande, List<LigneCommande> lignes) {
        List<OrderResponse.OrderLineResponse> lineResponses = lignes.stream().map(ligne -> {
            OrderResponse.OrderLineResponse lineResp = new OrderResponse.OrderLineResponse();
            lineResp.setIdLigneCommande(ligne.getIdLigneCommande());
            lineResp.setIdProduit(ligne.getIdProduit());
            lineResp.setIdArtisan(ligne.getIdArtisan());
            lineResp.setQuantite(ligne.getQuantite());
            lineResp.setPrix(ligne.getPrix());
            lineResp.setSousTotal(ligne.getPrix().multiply(BigDecimal.valueOf(ligne.getQuantite())));
            productRepository.findById(ligne.getIdProduit())
                    .ifPresent(p -> lineResp.setNomProduit(p.getNomProduit()));
            return lineResp;
        }).collect(Collectors.toList());

        OrderResponse response = new OrderResponse();
        response.setIdCommande(commande.getId());
        response.setIdUser(commande.getUserId());
        response.setTotal(BigDecimal.valueOf(commande.getTotal()));
        response.setStatut(commande.getStatut().toString());
        response.setTransactionId(commande.getTransactionId());
        response.setPaymentMethod(commande.getPaymentMethod());
        response.setNomClient(commande.getNomClient());
        response.setAdresseLivraison(commande.getAdresseLivraison());
        response.setTelephone(commande.getTelephone());
        response.setDateCreation(commande.getCreatedAt());
        response.setUpdatedAt(commande.getUpdatedAt());
        response.setLignes(lineResponses);
        return response;
    }
}