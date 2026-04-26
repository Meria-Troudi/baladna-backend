package tn.esprit.spring.baladna.marketplace.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewResponse {
    private Integer idReview;
    private Integer idUser;
    private Integer idProduit;
    private Integer idCommande;
    private Integer rating;
    private String commentaire;
    private LocalDateTime dateCreation;
    /** Display name from user profile when available */
    private String auteurNom;
}
