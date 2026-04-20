package tn.esprit.spring.baladna.event.forum.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class NotificationDTO {
    private Long id;
    private String type;
    private Long postId;
    private Long commentId;
    private Long senderId;
    private boolean isRead;
    private LocalDateTime createdAt;
}