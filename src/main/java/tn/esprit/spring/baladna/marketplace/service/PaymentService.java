package tn.esprit.spring.baladna.marketplace.service;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.marketplace.dto.request.PaymentRequest;
import tn.esprit.spring.baladna.marketplace.dto.response.PaymentResponse;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Processing Stripe payment for amount: {}", request.getAmount());

        try {
            // Validation du montant
            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return new PaymentResponse("FAILED", null, "Montant invalide");
            }

            // Créer un PaymentIntent Stripe
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(request.getAmount().multiply(new BigDecimal("100")).longValue()) // Centimes
                    .setCurrency("tnd") // ou "eur"
                    .setDescription("Commande Baladna")
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            String transactionId = "TXN-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 12);
            log.info("Stripe payment successful: paymentIntentId={}, transactionId={}",
                    paymentIntent.getId(), transactionId);

            return new PaymentResponse("SUCCESS", transactionId, "Paiement effectué avec succès");

        } catch (Exception e) {
            log.error("Stripe payment failed: {}", e.getMessage());
            return new PaymentResponse("FAILED", null, "Erreur paiement: " + e.getMessage());
        }
    }
}