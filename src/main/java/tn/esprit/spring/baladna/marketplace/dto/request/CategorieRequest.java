package tn.esprit.spring.baladna.marketplace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategorieRequest {

    @NotBlank(message = "Le nom de la catégorie est obligatoire")
    @Size(max = 100)
    private String nomCategorie;
}
