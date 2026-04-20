package tn.esprit.spring.baladna.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.spring.baladna.user.entity.Role;
import tn.esprit.spring.baladna.user.entity.Status;
import tn.esprit.spring.baladna.user.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByStatusNot(Status status);
    List<User> findByRole(Role role);
    List<User> findByStatus(Status status);
    List<User> findByFirstNameContainingOrLastNameContaining(String firstName, String lastName);
    long countByRole(Role role);
    long countByStatus(Status status);
}
