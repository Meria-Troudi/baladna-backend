package tn.esprit.spring.baladna.marketplace.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.baladna.marketplace.dto.request.CheckoutRequest;
import tn.esprit.spring.baladna.marketplace.dto.request.PaymentRequest;
import tn.esprit.spring.baladna.marketplace.dto.response.CheckoutResponse;
import tn.esprit.spring.baladna.marketplace.dto.response.PaymentResponse;
import tn.esprit.spring.baladna.marketplace.entity.Commande;
import tn.esprit.spring.baladna.marketplace.entity.StatutCommande;
import tn.esprit.spring.baladna.marketplace.repository.CommandeRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {

    private final PaymentService paymentService;
    private final CommandeRepository commandeRepository;
    @Transactional
    public CheckoutResponse processCheckout(CheckoutRequest req) {
        log.info("Processing checkout for user: {} with payment method: {}", req.getUserId(), req.getPaymentMethod());

        // CASH - Paiement à la livraison (pas de payment processing)
        if ("CASH".equals(req.getPaymentMethod())) {
            log.info("Cash on delivery - creating order directly");
            return createCashOrder(req);
        }

        // CARD - Traitement du paiement
        log.info("Card payment - processing payment");
        return processCardPayment(req);
    }

    private CheckoutResponse createCashOrder(CheckoutRequest req) {
        Commande commande = new Commande();
        commande.setUserId(req.getUserId());
        commande.setTotal(req.getTotal().doubleValue());
        commande.setPaymentMethod(req.getPaymentMethod());
        commande.setTransactionId("CASH-" + System.currentTimeMillis());
        commande.setStatut(StatutCommande.CONFIRMED);
        commande.setAdresseLivraison(req.getAdresseLivraison());
        commande.setTelephone(req.getTelephone());
        commande.setNomClient(req.getNomClient());
        commande.setCreatedAt(LocalDateTime.now());
        commande.setUpdatedAt(LocalDateTime.now());

        Commande saved = commandeRepository.save(commande);
        log.info("Cash order saved - ID: {}, Transaction: {}", saved.getId(), saved.getTransactionId());

        return new CheckoutResponse("SUCCESS", saved.getTransactionId(), saved, "Commande créée avec succès");
    }

    private CheckoutResponse processCardPayment(CheckoutRequest req) {
        BigDecimal totalAmount = req.getTotal() != null ? req.getTotal() : BigDecimal.ZERO;

        PaymentRequest paymentRequest = new PaymentRequest(
                req.getCardNumber(),
                req.getCardHolder(),
                req.getExpiryDate(),
                req.getCvv(),
                totalAmount,
                req.getPaymentMethod()
        );

        PaymentResponse paymentResponse = paymentService.processPayment(paymentRequest);

        if (!"SUCCESS".equals(paymentResponse.getStatus())) {
            return new CheckoutResponse("FAILED", null, null, "Paiement échoué : " + paymentResponse.getMessage());
        }

        Commande commande = new Commande();
        commande.setUserId(req.getUserId());
        commande.setTotal(req.getTotal().doubleValue());
        commande.setPaymentMethod(req.getPaymentMethod());
        commande.setTransactionId(paymentResponse.getTransactionId());
        commande.setStatut(StatutCommande.CONFIRMED);
        commande.setAdresseLivraison(req.getAdresseLivraison());
        commande.setTelephone(req.getTelephone());
        commande.setNomClient(req.getNomClient());
        commande.setCreatedAt(LocalDateTime.now());
        commande.setUpdatedAt(LocalDateTime.now());

        Commande saved = commandeRepository.save(commande);
        log.info("Card order saved - ID: {}, Transaction: {}", saved.getId(), saved.getTransactionId());

        return new CheckoutResponse("SUCCESS", paymentResponse.getTransactionId(), saved, "Commande créée avec succès");
    }
}