package tn.esprit.spring.baladna.event.entity;

import tn.esprit.spring.baladna.event.entity.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Enumerated(EnumType.STRING)
    private MediaType type;

    @Builder.Default
    private Boolean isCover = false;

    private Integer orderIndex;
}