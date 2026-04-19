package tn.esprit.spring.baladna.event.entity;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tn.esprit.spring.baladna.event.entity.enums.PostStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventForumPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(name = "user_id")
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    @JsonBackReference("forum-post-replies")
    private EventForumPost parent;  // Optional threaded reply

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "JSON")
    private String attachments;

    @Builder.Default
    private Integer likes = 0;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PostStatus status = PostStatus.VISIBLE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "parent")
    @JsonManagedReference("forum-post-replies")
    @Builder.Default
    private List<EventForumPost> replies = new ArrayList<>();
}