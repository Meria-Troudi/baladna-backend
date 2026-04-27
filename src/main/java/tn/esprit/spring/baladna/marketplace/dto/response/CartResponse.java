package tn.esprit.spring.baladna.marketplace.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CartResponse {
    private Integer idPanier;
    private Integer idUser;
    private String status;
    private LocalDateTime dateCreation;
    private LocalDateTime lastUpdated;
    private List<CartItemResponse> items;
    private BigDecimal total;

    @Data
    public static class CartItemResponse {
        private Integer idCartItem;
        private Integer idProduit;
        private String nomProduit;
        private String imageProduit;
        private Integer quantite;
        private BigDecimal prix;
        private BigDecimal sousTotal;
    }
}
