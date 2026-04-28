package tn.esprit.spring.baladna.event.forum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.event.forum.entity.SavedPost;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedPostRepository extends JpaRepository<SavedPost, Long> {

    Optional<SavedPost> findByPostIdAndUserId(Long postId, Long userId);

    List<SavedPost> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<SavedPost> findTop3ByUserIdOrderByCreatedAtDesc(Long userId);
    @Modifying
    @Query("DELETE FROM SavedPost s WHERE s.postId = :postId")
    int deleteAllByPostId(@Param("postId") Long postId);
}