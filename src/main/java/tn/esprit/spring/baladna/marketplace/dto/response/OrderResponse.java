package tn.esprit.spring.baladna.marketplace.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private Long idCommande;
    private Long idUser;
    private BigDecimal total;
    private String statut;
    private String transactionId;
    private String paymentMethod;
    private String nomClient;
    private String adresseLivraison;
    private String telephone;
    private LocalDateTime dateCreation;
    private LocalDateTime updatedAt;
    private List<OrderLineResponse> lignes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderLineResponse {
        private Integer idLigneCommande;   // Integer — matches LigneCommande.idLigneCommande
        private Integer idProduit;
        private Integer idArtisan;
        private Integer quantite;
        private BigDecimal prix;
        private BigDecimal sousTotal;
        private String nomProduit;
    }
}