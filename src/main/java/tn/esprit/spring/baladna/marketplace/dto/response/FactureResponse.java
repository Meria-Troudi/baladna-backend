package tn.esprit.spring.baladna.marketplace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FactureResponse {
    private Long id;
    private String numeroFacture;
    private Long commandeId;
    private Long touristeId;
    private String nomTouriste;
    private String emailTouriste;
    private LocalDateTime dateEmission;
    private BigDecimal montantHT;
    private BigDecimal tva;
    private BigDecimal montantTTC;
    private String statut;
    private String pdfBase64;  // PDF encodé en base64 pour téléchargement direct
    private String message;    // Message de confirmation
}