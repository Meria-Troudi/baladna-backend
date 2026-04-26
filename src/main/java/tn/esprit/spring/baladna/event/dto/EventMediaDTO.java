package tn.esprit.spring.baladna.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventMediaDTO {
    private Long id;
    private Long eventId;
    private String url;
    private String type;
    private Boolean isCover;
    private Integer orderIndex;
}
