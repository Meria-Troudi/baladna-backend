package tn.esprit.spring.baladna.user.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String firstName;
    private String lastName;
    private String preferredLanguage;
    private String profilePhoto;
}
