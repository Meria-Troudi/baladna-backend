package tn.esprit.spring.baladna.event.forum.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCommentRequestDTO {
    private String content;
    private Long parentId;
}