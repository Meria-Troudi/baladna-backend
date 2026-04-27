package tn.esprit.spring.baladna.event.forum.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class PostDTO {
    public Long id;
    public Long userId;
    public UserDTO user; // populated by service with real user info
    public String content;
    public String mediaUrl;
    public String mediaType;
    public String finalTopic;
    public String userTopic;
    public String aiTopic;
    public Double topicConfidence;
    public String aiTopicReason;
    public int likesCount;
    public int commentsCount;
    public int viewsCount;
    public LocalDateTime createdAt;
    // userReaction as string to avoid coupling; backend can supply enum name
    public String userReaction;
    public boolean isSaved;
    // Post lifecycle status (VISIBLE / HIDDEN / DELETED) — useful for admin UI
    public String status;
    // AI moderation verdict surfaced to admin UI
    public String moderationLabel;
    public String moderationReason;

    // simple embedded user dto to avoid creating additional packages for now
    @Getter
    @Setter
    @Builder
    public static class UserDTO {
        public Long id;
        public String firstName;
        public String lastName;
        public String profilePicture;
    }
}