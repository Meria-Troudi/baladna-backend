package tn.esprit.spring.baladna.itinerary.dto;

import lombok.Builder;
import lombok.Data;
import tn.esprit.spring.baladna.itinerary.entity.enums.CollaboratorRole;
import tn.esprit.spring.baladna.itinerary.entity.enums.CollaboratorStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CollaboratorResponse {

    private UUID id;
    private Long userId;
    private CollaboratorRole role;
    private CollaboratorStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime joinedAt;
}