package tn.esprit.spring.baladna.transport.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import tn.esprit.spring.baladna.transport.entity.TrafficCongestionLevel;
import tn.esprit.spring.baladna.transport.entity.TransportStatus;
import tn.esprit.spring.baladna.transport.entity.WeatherCondition;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransportDTO {

    private Long id;

    @NotBlank(message = "Le point de départ est obligatoire")
    @Size(min = 2, max = 150, message = "Le point de départ doit contenir entre 2 et 150 caractères")
    private String departurePoint;

    @NotNull(message = "La date de départ est obligatoire")
    private LocalDateTime departureDate;

    private LocalDateTime realDepartureDate;

    @NotNull(message = "La capacité totale est obligatoire")
    @Positive(message = "La capacité doit être positive")
    @Min(value = 1, message = "La capacité minimale est 1")
    @Max(value = 100, message = "La capacité maximale est 100")
    private Integer totalCapacity;

    private Integer availableSeats;

    private TransportStatus status;

    @NotNull(message = "Le prix de base est obligatoire")
    @Positive(message = "Le prix de base doit être positif")
    private Double basePrice;

    private Boolean trafficJam;
    private TrafficCongestionLevel trafficCongestionLevel;

    @NotNull(message = "La condition météo est obligatoire")
    private WeatherCondition weather;

    private String weatherSource;
    private Double weatherTemperature;
    private Double weatherWindSpeed;
    private Double weatherPrecipitation;

    @NotNull(message = "L'ID du trajet est obligatoire")
    @Positive(message = "L'ID du trajet doit être positif")
    private Long trajetId;

    private String trajetDescription;

    private Integer delayMinutes;

    @Min(value = 0, message = "Le retard reel ne peut pas etre negatif")
    private Integer actualDelayMinutes;
}
