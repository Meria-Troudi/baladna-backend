package tn.esprit.spring.baladna.event.forum.dto;

import lombok.Getter;
import lombok.Setter;
import tn.esprit.spring.baladna.event.forum.entity.ReactionType;

@Getter
@Setter
public class ReactionRequestDTO {
    private ReactionType type;
}