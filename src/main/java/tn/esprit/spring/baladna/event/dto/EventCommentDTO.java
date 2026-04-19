package tn.esprit.spring.baladna.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCommentDTO {
    private Long id;
    private Long eventId;
    private Long userId;
    private String content;
}
