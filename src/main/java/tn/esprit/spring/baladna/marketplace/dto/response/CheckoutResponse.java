package tn.esprit.spring.baladna.marketplace.dto.response;

//import com.marketplace.model.Commande;

import tn.esprit.spring.baladna.marketplace.entity.Commande;

public class CheckoutResponse {

    private String paymentStatus;
    private String transactionId;
    private Commande commande;
    private String message;

    public CheckoutResponse() {}

    public CheckoutResponse(String paymentStatus, String transactionId, Commande commande, String message) {
        this.paymentStatus = paymentStatus;
        this.transactionId = transactionId;
        this.commande = commande;
        this.message = message;
    }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public Commande getCommande() { return commande; }
    public void setCommande(Commande commande) { this.commande = commande; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
