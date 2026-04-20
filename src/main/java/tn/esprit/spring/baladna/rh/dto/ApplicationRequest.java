package tn.esprit.spring.baladna.rh.dto;
import lombok.Data;

@Data
public class ApplicationRequest {
    private Long interviewId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String cin;
    private String coverLetter;

}
