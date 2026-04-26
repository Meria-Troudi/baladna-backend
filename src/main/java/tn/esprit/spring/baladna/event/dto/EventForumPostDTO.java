package tn.esprit.spring.baladna.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventForumPostDTO {
    private Long id;
    private Long eventId;
    private Long userId;
    private Long parentId;
    private String content;
    private List<String> attachments;
}
