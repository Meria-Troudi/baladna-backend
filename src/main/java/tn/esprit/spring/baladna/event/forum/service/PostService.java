package tn.esprit.spring.baladna.event.forum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.baladna.event.forum.dto.FeedRequest;
import tn.esprit.spring.baladna.event.forum.dto.FeedResponse;
import tn.esprit.spring.baladna.event.forum.dto.PostDTO;
import tn.esprit.spring.baladna.event.forum.entity.MediaType;
import tn.esprit.spring.baladna.event.forum.entity.Post;
import tn.esprit.spring.baladna.event.forum.entity.PostStatus;
import tn.esprit.spring.baladna.event.forum.entity.PostTopic;
import tn.esprit.spring.baladna.event.forum.repository.CommentRepository;
import tn.esprit.spring.baladna.event.forum.repository.NotificationRepository;
import tn.esprit.spring.baladna.event.forum.repository.PostRepository;
import tn.esprit.spring.baladna.event.forum.repository.ReactionRepository;
import tn.esprit.spring.baladna.event.forum.repository.SavedPostRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepo;
    private final SavedPostRepository savedPostRepo;
    private final CommentRepository commentRepo;
    private final ReactionRepository reactionRepo;
    private final NotificationRepository notificationRepo;
    private final tn.esprit.spring.baladna.user.service.UserService userService;
    private final ModerationService moderationService;
    private final TopicAsyncService topicAsyncService;

    @org.springframework.beans.factory.annotation.Value("${moderation.auto-hide:true}")
    private boolean moderationAutoHide;

    @Transactional(readOnly = true)
    public Page<PostDTO> getAllPosts(Pageable pageable, Long currentUserId) {
        return postRepo.findByStatusOrderByCreatedAtDesc(PostStatus.VISIBLE, pageable)
                .map(post -> mapToDTO(post, currentUserId));
    }

    @Transactional
    public PostDTO getPostById(Long id, Long currentUserId) {
        Post post = postRepo.findByIdAndStatus(id, PostStatus.VISIBLE)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        postRepo.incrementViewsCount(id);
        return mapToDTO(post, currentUserId);
    }

    public PostDTO createPost(Long userId, tn.esprit.spring.baladna.event.forum.dto.CreatePostRequestDTO request) {

        if (request.getContent() == null || request.getContent().isBlank()) {
            throw new RuntimeException("Content is required");
        }

        Post post = new Post();
        post.setUserId(userId);
        post.setContent(request.getContent());

        if (request.getTopic() != null && !request.getTopic().isBlank()) {
            try {
                post.setUserTopic(PostTopic.valueOf(request.getTopic().trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                post.setUserTopic(PostTopic.OTHER);
            }
        }
        // Set only user topic at creation; let AI override later
        post.setFinalTopic(post.getUserTopic());

        post.setStatus(PostStatus.VISIBLE);

        if (request.getMediaUrl() != null) {
            post.setMediaUrl(request.getMediaUrl());
            post.setMediaType(request.getMediaType() != null ? MediaType.valueOf(request.getMediaType()) : null);
        }

        // AI moderation: classify locally via Ollama. Fails open -> SAFE on any error.
        ModerationService.Verdict verdict = moderationService.classify(request.getContent());
        post.setModerationLabel(verdict.label().name());
        post.setModerationReason(verdict.reason());
        if (moderationAutoHide && verdict.isBlocking()) {
            post.setStatus(PostStatus.HIDDEN);
        }

        post = postRepo.save(post);
        topicAsyncService.process(post.getId(), request.getContent());

        return mapToDTO(post, userId);
    }

    public void deletePost(Long postId, Long userId) {

        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        if (!post.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized");
        }

        post.setStatus(PostStatus.DELETED);
        postRepo.save(post);
    }

    public void incrementViews(Long postId) {
        postRepo.incrementViewsCount(postId);
    }

    @Transactional(readOnly = true)
    public List<PostDTO> getPostsByUserId(Long userId, Long currentUserId) {
        return postRepo.findByUserIdAndStatusOrderByCreatedAtDesc(userId, PostStatus.VISIBLE)
                .stream()
                .map(post -> mapToDTO(post, currentUserId))
                .toList();
    }

    public PostDTO mapToDTO(Post p, Long currentUserId) {
        PostDTO.UserDTO userDto = null;
        try {
            tn.esprit.spring.baladna.user.entity.User u = userService.getUserById(p.getUserId());
            if (u != null) {
                userDto = PostDTO.UserDTO.builder()
                        .id(u.getId())
                        .firstName(u.getFirstName())
                        .lastName(u.getLastName())
                        .profilePicture(u.getProfilePhoto())
                        .build();
            }
        } catch (Exception ignored) { }

        boolean isSaved = false;
        try {
            if (currentUserId != null) {
                isSaved = savedPostRepo.findByPostIdAndUserId(p.getId(), currentUserId).isPresent();
            }
        } catch (Exception ignored) { }

        return PostDTO.builder()
                .id(p.getId())
                .userId(p.getUserId())
                .user(userDto)
                .content(p.getContent())
                .mediaUrl(p.getMediaUrl())
                .mediaType(p.getMediaType() != null ? p.getMediaType().name() : null)
                .finalTopic(p.getFinalTopic() != null ? p.getFinalTopic().name() : null)
                .userTopic(p.getUserTopic() != null ? p.getUserTopic().name() : null)
                .aiTopic(p.getAiTopic() != null ? p.getAiTopic().name() : null)
                .topicConfidence(p.getTopicConfidence())
                .aiTopicReason(p.getAiTopicReason())
                .likesCount(p.getLikesCount())
                .commentsCount(p.getCommentsCount())
                .viewsCount(p.getViewsCount())
                .createdAt(p.getCreatedAt())
                .userReaction(null)
                .isSaved(isSaved)
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .moderationLabel(p.getModerationLabel())
                .moderationReason(p.getModerationReason())
                .build();
    }

    @Transactional(readOnly = true)
    public FeedResponse getFeed(FeedRequest request, Long currentUserId) {
        int size = request.getSize() > 0 ? request.getSize() : 10;

        java.time.LocalDateTime cursor = request.getCursor() != null && !request.getCursor().isBlank()
                ? java.time.LocalDateTime.parse(request.getCursor())
                : null;

        List<Post> postEntities;
        if (cursor == null) {
            postEntities = postRepo.findByStatusOrderByCreatedAtDesc(PostStatus.VISIBLE, org.springframework.data.domain.PageRequest.of(0, size)).getContent();
        } else {
            postEntities = postRepo.findByCreatedAtBeforeOrderByCreatedAtDesc(cursor, org.springframework.data.domain.PageRequest.of(0, size)).getContent();
        }

        List<PostDTO> posts = postEntities.stream().map(post -> mapToDTO(post, currentUserId)).toList();

        String nextCursor = posts.isEmpty()
                ? null
                : (posts.get(posts.size() - 1).getCreatedAt() != null
                ? posts.get(posts.size() - 1).getCreatedAt().toString()
                : null);

        return new FeedResponse(posts, nextCursor);
    }

    // ======================================
    //          ADMIN METHODS (kept simple)
    // ======================================
    public java.util.Map<String, Long> getAdminStats() {
        java.util.Map<String, Long> stats = new java.util.LinkedHashMap<>();
        stats.put("totalPosts", postRepo.count());
        stats.put("totalComments", commentRepo.count());
        stats.put("hiddenPosts", postRepo.countByStatus(PostStatus.HIDDEN));
        stats.put("activeUsers", postRepo.countDistinctUserId());
        stats.put("aiFlaggedToxic", postRepo.countByModerationLabel("TOXIC"));
        stats.put("aiFlaggedSpam", postRepo.countByModerationLabel("SPAM"));
        return stats;
    }

    @Transactional(readOnly = true)
    public java.util.List<PostDTO> getAllPostsAdmin() {
        return postRepo.findAll()
                .stream()
                .map(post -> mapToDTO(post, null))
                .toList();
    }

    public void updatePostStatus(Long id, String status) {
        Post post = postRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setStatus(PostStatus.valueOf(status));
        postRepo.save(post);
    }

    public void overrideFinalTopic(Long id, PostTopic topic) {
        Post post = postRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        post.setFinalTopic(topic != null ? topic : PostTopic.OTHER);
        postRepo.save(post);
    }

    /**
     * Admin hard-delete: permanently removes a post and everything that references it
     * (comments, reactions, saved-bookmarks, notifications). Not reversible.
     */
    @Transactional
    public void hardDeletePost(Long id) {
        if (!postRepo.existsById(id)) {
            throw new RuntimeException("Post not found");
        }
        notificationRepo.deleteAllByPostId(id);
        savedPostRepo.deleteAllByPostId(id);
        reactionRepo.deleteAllByPostId(id);
        commentRepo.deleteAllByPostId(id);
        postRepo.deleteById(id);
    }
}