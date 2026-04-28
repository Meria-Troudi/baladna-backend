package tn.esprit.spring.baladna.event.forum.service;


import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.esprit.spring.baladna.event.forum.entity.PostTopic;
import tn.esprit.spring.baladna.event.forum.repository.PostRepository;
import tn.esprit.spring.baladna.event.forum.service.TopicInferenceService;


@Service
@RequiredArgsConstructor
public class TopicAsyncService {
    private static final Logger log = LoggerFactory.getLogger(TopicAsyncService.class);

    private static final double AI_CONFIDENCE_THRESHOLD = 0.70;

    private final TopicInferenceService topicInferenceService;
    private final PostRepository postRepository;

    @Async
    @Transactional
    public void process(Long postId, String content) {
        TopicInferenceService.Result result = topicInferenceService.classify(content);

        try {
            postRepository.findById(postId).ifPresent(post -> {
                post.setAiTopic(result.topic());
                post.setTopicConfidence(result.confidence());
                post.setAiTopicReason(result.reason());

                // Only override finalTopic if AI confidence is high
                if (result.confidence() >= AI_CONFIDENCE_THRESHOLD) {
                    post.setFinalTopic(result.topic());
                }

                postRepository.save(post);
            });
        } catch (Exception e) {
            log.warn("Failed to update AI topic for post {}", postId, e);
        }
    }
}
