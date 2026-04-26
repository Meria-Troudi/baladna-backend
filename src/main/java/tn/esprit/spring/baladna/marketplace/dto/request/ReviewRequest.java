package tn.esprit.spring.baladna.marketplace.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotNull(message = "L'identifiant utilisateur est obligatoire")
    private Integer idUser;

    @NotNull(message = "L'identifiant produit est obligatoire")
    private Integer idProduit;

    private Integer idCommande;

    @NotNull(message = "La note est obligatoire")
    @Min(value = 1, message = "La note minimale est 1")
    @Max(value = 5, message = "La note maximale est 5")
    private Integer rating;

    @Size(max = 1000, message = "Le commentaire ne doit pas dépasser 1000 caractères")
    private String commentaire;
}
