package tn.esprit.spring.baladna.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventDTO {
    private String title;
    private String description;
    private Long categoryId;       // Send categoryId instead of nested object
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String location;
    private Double latitude;
    private Double longitude;
    private Integer capacity;
    private Double price;
    private Long createdByUserId;  // Keep as Long for now
}
