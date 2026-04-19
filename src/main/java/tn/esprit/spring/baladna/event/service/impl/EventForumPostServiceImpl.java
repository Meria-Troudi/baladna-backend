package tn.esprit.spring.baladna.event.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.event.dto.EventForumPostDTO;
import tn.esprit.spring.baladna.event.entity.Event;
import tn.esprit.spring.baladna.event.entity.EventForumPost;
import tn.esprit.spring.baladna.event.repository.EventRepository;
import tn.esprit.spring.baladna.event.repository.EventForumPostRepository;
import tn.esprit.spring.baladna.event.service.IEventForumPostService;

import java.util.List;

@AllArgsConstructor
@Service
public class EventForumPostServiceImpl implements IEventForumPostService {

    private final EventForumPostRepository forumPostRepository;
    private final EventRepository eventRepository;

    @Override
    public List<EventForumPost> retrieveEventForumPosts() {
        return forumPostRepository.findAll();
    }

    @Override
    public EventForumPost addEventForumPost(EventForumPostDTO post) {
        Event event = eventRepository.findById(post.getEventId()).orElse(null);
        EventForumPost parent = post.getParentId() == null ? null
                : forumPostRepository.findById(post.getParentId()).orElse(null);
        EventForumPost entity = EventForumPost.builder()
                .event(event)
                .userId(post.getUserId())
                .parent(parent)
                .content(post.getContent())
                .attachments(post.getAttachments() == null ? null : post.getAttachments().toString())
                .build();
        return forumPostRepository.save(entity);
    }

    @Override
    public EventForumPost updateEventForumPost(EventForumPostDTO post) {
        EventForumPost entity = forumPostRepository.findById(post.getId()).orElse(null);
        if (entity == null) {
            return null;
        }
        if (post.getEventId() != null) {
            entity.setEvent(eventRepository.findById(post.getEventId()).orElse(null));
        }
        if (post.getParentId() != null) {
            entity.setParent(forumPostRepository.findById(post.getParentId()).orElse(null));
        }
        entity.setUserId(post.getUserId());
        entity.setContent(post.getContent());
        entity.setAttachments(post.getAttachments() == null ? null : post.getAttachments().toString());
        return forumPostRepository.save(entity);
    }

    @Override
    public EventForumPost retrieveEventForumPost(Long id) {
        return forumPostRepository.findById(id).orElse(null);
    }

    @Override
    public void removeEventForumPost(Long id) {
        forumPostRepository.deleteById(id);
    }
}
