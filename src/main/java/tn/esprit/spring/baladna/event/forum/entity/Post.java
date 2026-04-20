package tn.esprit.spring.baladna.event.forum.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "forum_posts")
@Getter
@Setter
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String mediaUrl;
    
    @Enumerated(EnumType.STRING)
    private MediaType mediaType;

    @Enumerated(EnumType.STRING)
    private PostTopic finalTopic;

    @Enumerated(EnumType.STRING)
    private PostTopic userTopic;

    @Enumerated(EnumType.STRING)
    private PostTopic aiTopic;

    private Double topicConfidence;

    private String aiTopicReason;

    @Enumerated(EnumType.STRING)
    private PostStatus status = PostStatus.VISIBLE;

    private int likesCount;
    private int commentsCount;
    private int viewsCount;

    /**
     * Legacy DB column exists in some schemas (NOT NULL, no default).
     * Keep it to remain compatible with existing DB until migration.
     */
    @Column(nullable = false)
    private boolean pinned = false;

    /** AI moderation verdict: SAFE / TOXIC / SPAM (nullable = not moderated yet). */
    @Column(length = 16)
    private String moderationLabel;

    /** Short human-readable reason produced by the moderation model. */
    @Column(length = 255)
    private String moderationReason;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        likesCount = 0;
        commentsCount = 0;
        viewsCount = 0;
        pinned = false;
    }
}