package tn.esprit.spring.baladna.rh.entity;
import jakarta.persistence.*;
import lombok.*;
import tn.esprit.spring.baladna.user.entity.User;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonBackReference;
@Entity
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ lié à User (peut être null si non connecté)
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "interview_id")
    @JsonBackReference
    private Interview interview;

    // Infos candidat
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String cvPath;
    @Column(columnDefinition = "TEXT")// chemin du CV uploadé
    private String coverLetter;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status; // PENDING, ACCEPTED, REJECTED, INTERVIEW_SCHEDULED

    // ✅ Score ATS (0-100)
    private Integer atsScore;
    @Column(columnDefinition = "TEXT")
    private String atsFeedback;

    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
    @Column(length = 8)
    private String cin;
}
