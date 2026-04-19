package tn.esprit.spring.baladna.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.baladna.user.entity.ActivityLog;
import tn.esprit.spring.baladna.user.entity.User;

import java.util.List;
import java.util.UUID;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, UUID> {
    List<ActivityLog> findByUserOrderByTimestampDesc(User user);
    @Transactional
    void deleteAllByUser(User user);
}
