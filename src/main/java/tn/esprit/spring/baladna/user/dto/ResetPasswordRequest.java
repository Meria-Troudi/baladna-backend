package tn.esprit.spring.baladna.user.dto;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String token;
    private String newPassword;
}
