package tn.esprit.spring.baladna.accommodation.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayResponseDto {
    private String invoiceNumber;
    private String confirmationCode;
    /** PNG as Base64 (data URL usable in &lt;img src&gt;). */
    private String qrImageBase64;
    private boolean confirmationEmailSent;
    private String message;
}
