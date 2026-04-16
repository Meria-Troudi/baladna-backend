package tn.esprit.spring.baladna.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeoLocationDTO {
    private String name;
    private String city;
    private String displayName;
    private Double latitude;
    private Double longitude;
}
