package tn.esprit.spring.baladna.marketplace.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FavorisResponse {
    private Integer idFavoris;
    private Integer idUser;
    private Integer idProduit;
    private String nomProduit;
    private BigDecimal prixProduit;
    private String imageProduit;
    private LocalDateTime dateCreation;
}
