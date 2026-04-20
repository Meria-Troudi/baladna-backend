package tn.esprit.spring.baladna.event.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import tn.esprit.spring.baladna.event.forum.entity.ReactionType;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public class ReactionResponseDTO {
    private ReactionType userReaction;
    private Map<ReactionType, Long> counts;
}