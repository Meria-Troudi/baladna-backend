package tn.esprit.spring.baladna.accommodation.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ClientReviewResponseDto {
    private UUID reviewId;
    private int stars;
    private String comment;
    private LocalDateTime createdAt;
    private String accommodationTitle;
    private String guestDisplayName;
    private UUID reservationId;
}
