package tn.esprit.spring.baladna.rh.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.baladna.rh.entity.Application;
import tn.esprit.spring.baladna.rh.entity.ApplicationStatus;
import java.util.List;
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByInterviewId(Long interviewId);
    List<Application> findByEmail(String email);
    List<Application> findByInterviewIdOrderByAtsScoreDesc(Long interviewId);
    boolean existsByEmailAndInterviewId(String email, Long interviewId);

}
