package tn.esprit.spring.baladna.event.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder

@NoArgsConstructor
@AllArgsConstructor
public class EventDTO {
    private Long id;
    private String title;
    private String description;
    private String location;
    private String category;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String status;
    private Integer capacity;
    private Integer remainingSeats;
    private BigDecimal price;
    private Double latitude;
    private Double longitude;
}
