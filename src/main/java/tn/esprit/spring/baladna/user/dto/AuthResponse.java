package tn.esprit.spring.baladna.user.dto;

import lombok.Builder;
import lombok.Data;
import tn.esprit.spring.baladna.user.entity.Role;

@Data
@Builder
public class AuthResponse {
    private String accessToken;

    private String refreshToken;

    private Role role;
}
