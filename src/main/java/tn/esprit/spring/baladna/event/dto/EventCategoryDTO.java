package tn.esprit.spring.baladna.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventCategoryDTO {
    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private String icon;
    private Boolean isActive;
}
