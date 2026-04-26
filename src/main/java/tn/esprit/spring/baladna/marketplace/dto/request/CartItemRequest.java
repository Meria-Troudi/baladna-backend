package tn.esprit.spring.baladna.marketplace.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartItemRequest {

    @NotNull(message = "L'identifiant utilisateur est obligatoire")
    private Integer idUser;

    @NotNull(message = "L'identifiant produit est obligatoire")
    private Integer idProduit;

    @NotNull(message = "La quantité est obligatoire")
    @Min(value = 1, message = "La quantité doit être au moins 1")
    private Integer quantite;
}
