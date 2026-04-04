package tn.esprit.spring.baladna.user.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.baladna.user.entity.Session;
import tn.esprit.spring.baladna.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByToken(String token);
    List<Session> findByUserAndExpiresAtAfter(User user, LocalDateTime now);

    @Transactional
    void deleteAllByUser(User user);
}
