package tn.esprit.spring.baladna.marketplace.controller;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.marketplace.dto.request.PaymentRequest;
import tn.esprit.spring.baladna.marketplace.dto.response.PaymentResponse;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/payment")
@CrossOrigin(origins = "*")
@Slf4j
public class PaymentController {

    @Value("${stripe.secret.key}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    /**
     * POST /payment/process
     * Paiement simulé
     */
    @PostMapping("/process")
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        log.info("Processing payment for amount: {}", request.getAmount());

        if (request.getCardNumber() == null || request.getCardNumber().isBlank()) {
            return ResponseEntity.ok(new PaymentResponse("FAILED", null, "Invalid card number"));
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.ok(new PaymentResponse("FAILED", null, "Invalid amount"));
        }

        String transactionId = "TXN-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 12);
        log.info("Payment successful: transactionId={}", transactionId);

        return ResponseEntity.ok(new PaymentResponse("SUCCESS", transactionId, "Payment successful"));
    }

    /**
     * POST /payment/create-payment-intent
     * Crée un PaymentIntent Stripe en EUR
     */
    @PostMapping("/create-payment-intent")
    public ResponseEntity<Map<String, Object>> createPaymentIntent(@RequestBody Map<String, Object> request) {
        try {
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            long amountInCents = amount.multiply(new BigDecimal("100")).longValue();

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency("eur")
                    .addPaymentMethodType("card")
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            String transactionId = "STRIPE-" + paymentIntent.getId().substring(0, 12).toUpperCase();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "SUCCESS");
            response.put("transactionId", transactionId);
            response.put("clientSecret", paymentIntent.getClientSecret());
            response.put("message", "Payment intent created");

            log.info("Stripe PaymentIntent created: {}", paymentIntent.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Stripe error: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "FAILED");
            errorResponse.put("message", "Payment error: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}