package tn.esprit.spring.baladna.marketplace.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    private Long userId;
    private String paymentMethod;
    private BigDecimal total;  // ← AJOUTÉ

    // Carte bancaire
    private String cardNumber;
    private String cardHolder;
    private String expiryDate;
    private String cvv;

    // Livraison
    private String adresseLivraison;
    private String telephone;
    private String nomClient;
}