package tn.esprit.spring.baladna.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventReviewDTO {
    private Long id;
    private Long eventId;
    private Long userId;
    private Long reservationId;
    private Integer rating;
    private String comment;
}
