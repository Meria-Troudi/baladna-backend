package tn.esprit.spring.baladna.event.forum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.baladna.event.forum.dto.CommentDTO;
import tn.esprit.spring.baladna.event.forum.dto.ReactionResponseDTO;
import tn.esprit.spring.baladna.event.forum.entity.Comment;
import tn.esprit.spring.baladna.event.forum.entity.Reaction;
import tn.esprit.spring.baladna.event.forum.entity.ReactionType;
import tn.esprit.spring.baladna.event.forum.repository.CommentRepository;
import tn.esprit.spring.baladna.event.forum.repository.PostRepository;
import tn.esprit.spring.baladna.event.forum.repository.ReactionRepository;
import tn.esprit.spring.baladna.event.forum.repository.SavedPostRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InteractionService {

    private final ReactionRepository reactionRepo;
    private final CommentRepository commentRepo;
    private final PostRepository postRepo;
    private final SavedPostRepository savedPostRepo;
    private final NotificationService notificationService;
    private final tn.esprit.spring.baladna.user.service.UserService userService;
    private final PostService postService;

    // REACTIONS
    public ReactionResponseDTO react(Long postId, Long userId, ReactionType type) {

        postRepo.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));

        Optional<Reaction> existing = reactionRepo.findByPostIdAndUserId(postId, userId);

        if (existing.isPresent()) {

            Reaction r = existing.get();

            // toggle off if same reaction
            if (r.getType() == type) {
                reactionRepo.delete(r);
                postRepo.decrementLikesCount(postId);
                return buildReactionResponse(postId, null);
            }

            // update reaction type (no change to total count)
            r.setType(type);
            reactionRepo.save(r);

        } else {
            Reaction r = new Reaction();
            r.setPostId(postId);
            r.setUserId(userId);
            r.setType(type);
            r.setCreatedAt(LocalDateTime.now());
            reactionRepo.save(r);
            postRepo.incrementLikesCount(postId);
        }

        Long postOwnerId = postRepo.findUserIdById(postId);
        notificationService.notifyLike(postOwnerId, userId, postId);

        return buildReactionResponse(postId, type);
    }

    public ReactionResponseDTO getReactions(Long postId, Long userId) {

        ReactionType userReaction = reactionRepo.findByPostIdAndUserId(postId, userId)
                .map(Reaction::getType)
                .orElse(null);

        return buildReactionResponse(postId, userReaction);
    }

    private ReactionResponseDTO buildReactionResponse(Long postId, ReactionType userReaction) {

        Map<ReactionType, Long> counts = new EnumMap<>(ReactionType.class);

        for (ReactionType t : ReactionType.values()) {
            counts.put(t, reactionRepo.countByPostIdAndType(postId, t));
        }

        return new ReactionResponseDTO(userReaction, counts);
    }

    // COMMENTS
    public CommentDTO addComment(Long postId, Long userId, String content, Long parentId) {

        if (content == null || content.trim().isEmpty()) {
            throw new RuntimeException("Comment cannot be empty");
        }
        if (content.length() > 1000) {
            throw new RuntimeException("Comment too long");
        }

        postRepo.findById(postId).orElseThrow(() -> new RuntimeException("Post not found"));

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setParentId(parentId);
        comment.setCreatedAt(LocalDateTime.now());

        comment = commentRepo.save(comment);

        postRepo.incrementCommentsCount(postId);

        Long postOwnerId = postRepo.findUserIdById(postId);
        notificationService.notifyComment(postOwnerId, userId, postId, comment.getId());

        if (parentId != null) {
            Long parentOwnerId = commentRepo.findUserIdById(parentId);
            if (parentOwnerId != null) {
                notificationService.notifyReply(parentOwnerId, userId, postId, comment.getId());
            }
        }

        return mapToDTO(comment);
    }

    public void deleteComment(Long commentId, Long userId) {

        Comment comment = commentRepo.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!Objects.equals(comment.getUserId(), userId)) {
            throw new RuntimeException("Not authorized to delete this comment");
        }

        commentRepo.delete(comment);
        postRepo.decrementCommentsCount(comment.getPostId());
    }

    public List<CommentDTO> getComments(Long postId) {
        List<Comment> comments = commentRepo.findByPostIdOrderByCreatedAtAsc(postId);
        if (comments.isEmpty()) return List.of();
        return comments.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    private CommentDTO mapToDTO(Comment c) {
        String authorName = null;
        String authorAvatar = null;
        try {
            tn.esprit.spring.baladna.user.entity.User u = userService.getUserById(c.getUserId());
            if (u != null) {
                authorName = ((u.getFirstName() != null ? u.getFirstName() : "") + " "
                        + (u.getLastName() != null ? u.getLastName() : "")).trim();
                authorAvatar = u.getProfilePhoto();
            }
        } catch (Exception ignored) { }

        return CommentDTO.builder()
                .id(c.getId())
                .userId(c.getUserId())
                .content(c.getContent())
                .parentId(c.getParentId())
                .createdAt(c.getCreatedAt())
                .authorName(authorName)
                .authorAvatar(authorAvatar)
                .replies(new ArrayList<>())
                .build();
    }

    // SAVED POSTS
    public boolean toggleSave(Long postId, Long userId) {
        Optional<tn.esprit.spring.baladna.event.forum.entity.SavedPost> existing =
                savedPostRepo.findByPostIdAndUserId(postId, userId);

        if (existing.isPresent()) {
            savedPostRepo.delete(existing.get());
            return false;
        }

        tn.esprit.spring.baladna.event.forum.entity.SavedPost s = new tn.esprit.spring.baladna.event.forum.entity.SavedPost();
        s.setPostId(postId);
        s.setUserId(userId);
        s.setCreatedAt(LocalDateTime.now());
        savedPostRepo.save(s);
        return true;
    }

    public List<tn.esprit.spring.baladna.event.forum.dto.PostDTO> getSavedPosts(Long userId) {
        return savedPostRepo.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(sp -> postRepo.findById(sp.getPostId()).orElse(null))
                .filter(Objects::nonNull)
                .map(p -> postService.mapToDTO(p, userId))
                .collect(Collectors.toList());
    }
}