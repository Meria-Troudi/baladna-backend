package tn.esprit.spring.baladna.rh.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;
@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Interview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String location;
    private String department;
    private String contractType; // CDI, CDD, Stage, Freelance

    @Enumerated(EnumType.STRING)
    private InterviewStatus status; // OPEN, CLOSED, CANCELLED

    private LocalDateTime scheduledAt;
    private Integer maxCandidates;
    private String requiredSkills;
    private Integer experienceYears;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<Application> applications;
}
