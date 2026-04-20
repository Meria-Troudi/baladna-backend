package tn.esprit.spring.baladna.event.forum.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.event.forum.entity.Post;
import tn.esprit.spring.baladna.event.forum.entity.PostStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    Page<Post> findByStatusOrderByCreatedAtDesc(PostStatus status, Pageable pageable);
    Optional<Post> findByIdAndStatus(Long id, PostStatus status);
    List<Post> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, PostStatus status);
    // Counters (atomic ops)
    @Modifying
    @Query("UPDATE Post p SET p.likesCount = p.likesCount + 1 WHERE p.id = :postId")
    void incrementLikesCount(@Param("postId") Long postId);
    @Modifying
    @Query("UPDATE Post p SET p.likesCount = p.likesCount - 1 WHERE p.id = :postId AND p.likesCount > 0")
    void decrementLikesCount(@Param("postId") Long postId);
    @Modifying
    @Query("UPDATE Post p SET p.commentsCount = p.commentsCount + 1 WHERE p.id = :postId")
    void incrementCommentsCount(@Param("postId") Long postId);
    @Modifying
    @Query("UPDATE Post p SET p.commentsCount = p.commentsCount - 1 WHERE p.id = :postId AND p.commentsCount > 0")
    void decrementCommentsCount(@Param("postId") Long postId);
    @Modifying
    @Query("UPDATE Post p SET p.viewsCount = p.viewsCount + 1 WHERE p.id = :postId")
    void incrementViewsCount(@Param("postId") Long postId);
    @Query("SELECT p.userId FROM Post p WHERE p.id = :postId")
    Long findUserIdById(@Param("postId") Long postId);
    Page<Post> findByCreatedAtBeforeOrderByCreatedAtDesc(java.time.LocalDateTime cursor, Pageable pageable);
    long countByStatus(PostStatus status);

    @Query("SELECT COUNT(DISTINCT p.userId) FROM Post p")
    long countDistinctUserId();
    Optional<Post> findTopByStatusOrderByLikesCountDescCreatedAtDesc(PostStatus status);
    long countByModerationLabel(String moderationLabel);
}