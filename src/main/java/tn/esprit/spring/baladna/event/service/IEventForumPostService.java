package tn.esprit.spring.baladna.event.service;

import tn.esprit.spring.baladna.event.dto.EventForumPostDTO;
import tn.esprit.spring.baladna.event.entity.EventForumPost;
import java.util.List;

public interface IEventForumPostService {
    List<EventForumPost> retrieveEventForumPosts();
    EventForumPost addEventForumPost(EventForumPostDTO post);
    EventForumPost updateEventForumPost(EventForumPostDTO post);
    EventForumPost retrieveEventForumPost(Long id);
    void removeEventForumPost(Long id);
}
