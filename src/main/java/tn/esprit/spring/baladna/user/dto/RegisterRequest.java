package tn.esprit.spring.baladna.user.dto;

import lombok.Data;
import tn.esprit.spring.baladna.user.entity.Role;

@Data
public class RegisterRequest {
    private String firstName;

    private String lastName;

    private String email;

    private String password;

    private Role role;

    private String preferredLanguage;
}
