package tn.esprit.spring.baladna.marketplace.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank(message = "Le nom du produit est obligatoire")
    @Size(max = 200, message = "Le nom ne doit pas dépasser 200 caractères")
    private String nomProduit;

    private String descriptionProduit;

    private String imageProduit;

    @NotNull(message = "Le prix est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "Le prix doit être positif")
    private BigDecimal prixProduit;

    @NotNull(message = "Le stock est obligatoire")
    @Min(value = 0, message = "Le stock ne peut pas être négatif")
    private Integer stockProduit;

    private Integer idCategorie;
    private Integer idArtisan;

    public void setNom(String productName) {
    }

    public void setPrix(Double price) {
    }

    public void setStock(Integer stock) {
    }

    public void setCategorie(String category) {
    }
}
