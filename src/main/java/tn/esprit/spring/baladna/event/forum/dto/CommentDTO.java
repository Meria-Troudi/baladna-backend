package tn.esprit.spring.baladna.event.forum.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
public class CommentDTO {
    private Long id;
    private Long userId;
    private String content;
    private Long parentId;
    private LocalDateTime createdAt;
    private String authorName;
    private String authorAvatar;

    @Builder.Default
    private List<CommentDTO> replies = new ArrayList<>();
}