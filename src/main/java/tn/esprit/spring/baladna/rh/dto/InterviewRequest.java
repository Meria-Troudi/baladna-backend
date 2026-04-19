package tn.esprit.spring.baladna.rh.dto;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InterviewRequest {
    private String title;
    private String description;
    private String location;
    private String department;
    private String contractType;
    private LocalDateTime scheduledAt;
    private Integer maxCandidates;
    private String requiredSkills;
    private Integer experienceYears;
}
