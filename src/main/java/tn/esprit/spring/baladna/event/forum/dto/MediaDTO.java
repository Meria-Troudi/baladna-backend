package tn.esprit.spring.baladna.event.forum.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MediaDTO {
    private String url;
    private String type; // IMAGE / VIDEO
    private int position;
}