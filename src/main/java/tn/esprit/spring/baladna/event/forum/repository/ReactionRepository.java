package tn.esprit.spring.baladna.event.forum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.event.forum.entity.Reaction;
import tn.esprit.spring.baladna.event.forum.entity.ReactionType;

import java.util.Optional;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    Optional<Reaction> findByPostIdAndUserId(Long postId, Long userId);

    long countByPostIdAndType(Long postId, ReactionType type);

    @Modifying
    @Query("DELETE FROM Reaction r WHERE r.postId = :postId")
    int deleteAllByPostId(@Param("postId") Long postId);

}