package tn.esprit.spring.baladna.user.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.baladna.user.entity.PasswordResetToken;

import java.util.Optional;
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long>{
    Optional<PasswordResetToken> findByToken(String token);
    void deleteAllByUser_Id(Long userId);
}
