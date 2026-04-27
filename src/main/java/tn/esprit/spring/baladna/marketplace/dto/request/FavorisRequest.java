package tn.esprit.spring.baladna.marketplace.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FavorisRequest {

    @NotNull(message = "L'identifiant utilisateur est obligatoire")
    private Integer idUser;

    @NotNull(message = "L'identifiant produit est obligatoire")
    private Integer idProduit;
}
