package tn.esprit.spring.baladna.user.dto;

import lombok.Data;

@Data
public class FaceLoginRequest {
    private String email;
    private String image;
}