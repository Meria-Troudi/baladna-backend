package tn.esprit.spring.baladna.rh.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.baladna.rh.entity.Interview;
import tn.esprit.spring.baladna.rh.entity.InterviewStatus;
import java.util.List;
public interface InterviewRepository extends JpaRepository<Interview, Long>  {
    List<Interview> findByStatus(InterviewStatus status);
    List<Interview> findByDepartment(String department);
    List<Interview> findByStatusAndDepartment(InterviewStatus status, String department);
}
