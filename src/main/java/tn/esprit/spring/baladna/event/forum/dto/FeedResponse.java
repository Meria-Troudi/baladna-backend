package tn.esprit.spring.baladna.event.forum.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class FeedResponse {

    private List<PostDTO> posts;
    private String nextCursor;
}