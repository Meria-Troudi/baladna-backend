package tn.esprit.spring.baladna.event.forum.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeedRequest {

    private String cursor;
    private int size;
    private String mode; // "LATEST" or "POPULAR"
}