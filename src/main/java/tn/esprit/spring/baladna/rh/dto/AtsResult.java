package tn.esprit.spring.baladna.rh.dto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AtsResult {
    private Integer score;
    private String feedback;
    private boolean accepted;
}
