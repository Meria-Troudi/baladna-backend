package tn.esprit.spring.baladna.event.forum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.spring.baladna.event.forum.entity.Comment;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
    @Query("SELECT c.userId FROM Comment c WHERE c.id = :commentId")
    Long findUserIdById(@Param("commentId") Long commentId);
    @Modifying
    @Query("DELETE FROM Comment c WHERE c.postId = :postId")
    int deleteAllByPostId(@Param("postId") Long postId);
}