package tn.esprit.spring.baladna.marketplace.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductResponse {
    private Integer idProduit;
    private String nomProduit;
    private String descriptionProduit;
    private String imageProduit;
    private BigDecimal prixProduit;
    private Integer stockProduit;
    private LocalDateTime dateCreation;
    private LocalDateTime updatedAt;
    private Integer idCategorie;
    private Integer idArtisan;
    private List<String> images; // URLs des images additionnelles
}
