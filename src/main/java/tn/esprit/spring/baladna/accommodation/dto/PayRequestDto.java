package tn.esprit.spring.baladna.accommodation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PayRequestDto {
    @NotNull
    private UUID bookingGroupId;

    /** Simulated card number (16+ digits). Spaces allowed. */
    @NotBlank
    private String cardNumber;

    private String cardHolder;
}
