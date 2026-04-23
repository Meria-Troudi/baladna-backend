package tn.esprit.spring.baladna.event.forum.dto;
 
import lombok.Getter;
import lombok.Setter;
 
@Getter
@Setter
public class CreatePostRequestDTO {
    private String content;
    private String topic;

    // SINGLE MEDIA ONLY
    private String mediaUrl;
    private String mediaType;
}