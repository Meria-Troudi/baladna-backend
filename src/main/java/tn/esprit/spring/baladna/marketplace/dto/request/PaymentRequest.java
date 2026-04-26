package tn.esprit.spring.baladna.marketplace.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
// PaymentRequest.java - Corrigé
@Data
@NoArgsConstructor

public class PaymentRequest {
    private String cardNumber;
    private String cardHolder;
    private String expiryDate;
    private String cvv;
    private BigDecimal amount;
    private String paymentMethod;

    public PaymentRequest(String cardNumber, String cardHolder, String expiryDate, String cvv, BigDecimal total, String paymentMethod) {
        this.cardNumber = cardNumber;
        this.cardHolder = cardHolder;
        this.expiryDate = expiryDate;
        this.cvv = cvv;
        this.amount = total;
        this.paymentMethod = paymentMethod;
    }
}