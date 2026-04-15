package tn.esprit.spring.baladna.user.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;

    private String password;

}
