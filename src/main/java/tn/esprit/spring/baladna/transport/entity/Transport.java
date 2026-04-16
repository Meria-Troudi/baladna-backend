package tn.esprit.spring.baladna.transport.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import tn.esprit.spring.baladna.user.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "transports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le point de d�part est obligatoire")
    @Size(min = 2, max = 150, message = "Le point de d�part doit contenir entre 2 et 150 caract�res")
    @Column(nullable = false)
    private String departurePoint;

    @NotNull(message = "La date de d�part est obligatoire")
    @Future(message = "La date doit �tre dans le futur")
    @Column(nullable = false)
    private LocalDateTime departureDate;

    @NotNull(message = "La capacit� totale est obligatoire")
    @Positive(message = "La capacit� doit �tre positive")
    @Min(value = 1, message = "La capacit� minimale est 1")
    @Max(value = 100, message = "La capacit� maximale est 100")
    @Column(nullable = false)
    private Integer totalCapacity;

    @Column(nullable = false)
    private Integer availableSeats;

    @NotNull(message = "Le statut est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransportStatus status;

    @NotNull(message = "Le prix de base est obligatoire")
    @Positive(message = "Le prix de base doit �tre positif")
    @Column(nullable = false)
    private Double basePrice;

    @Builder.Default
    @Column(nullable = false)
    private Boolean trafficJam = false;

    @NotNull(message = "La condition m�t�o est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WeatherCondition weather;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String weatherSource = "MANUAL";
    @Column
    private Double weatherTemperature;

    @Column
    private Double weatherWindSpeed;

    @Column
    private Double weatherPrecipitation;

    @NotNull(message = "Le trajet est obligatoire")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "trajet_id", nullable = false)
    private Trajet trajet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    @JsonIgnore
    private User host;

    @OneToMany(mappedBy = "transport", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<Reservation> reservations = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (availableSeats == null && totalCapacity != null) {
            availableSeats = totalCapacity;
        }
        if (trafficJam == null) {
            trafficJam = false;
        }
        if (status == null) {
            status = TransportStatus.SCHEDULED;
        }
        if (weatherSource == null || weatherSource.isBlank()) {
            weatherSource = "MANUAL";
        }
    }

    public boolean checkWeatherConditions() {
        return weather != WeatherCondition.STORM;
    }

    public int calculateDelay() {
        int delay = 0;

        if (weather != null) {
            switch (weather) {
                case RAIN -> delay += 25;
                case SANDSTORM -> delay += 30;
                case STORM -> delay += 40;
                default -> delay += 0;
            }
        }

        if (Boolean.TRUE.equals(trafficJam)) {
            delay += 20;
        }

        return delay;
    }

    public LocalDateTime getRealDepartureDate() {
        if (departureDate == null) return null;
        return departureDate.plusMinutes(calculateDelay());
    }

    public double calculatePrice(String boardingPoint, int lastSeatsCount) {
        double price = (trajet != null) ? trajet.getBasePrice() : (basePrice != null ? basePrice : 0.0);

        double departureSurcharge = 0.0;
        if (boardingPoint != null && trajet != null && trajet.getDepartureStation() != null) {
            departureSurcharge = trajet.getDepartureStation().getPriceWithSurcharge(0);
        }
        price += departureSurcharge;

        double lastSeatsMultiplier = 1.00;
        if (availableSeats != null && lastSeatsCount >= 3 && availableSeats <= 3) {
            lastSeatsMultiplier = 1.30;
        }
        price *= lastSeatsMultiplier;

        double trafficMultiplier = Boolean.TRUE.equals(trafficJam) ? 1.25 : 1.00;
        price *= trafficMultiplier;

        double weatherMultiplier = 1.00;
        if (weather != null) {
            switch (weather) {
                case RAIN -> weatherMultiplier = 1.10;
                case SANDSTORM -> weatherMultiplier = 1.15;
                case STORM -> weatherMultiplier = 1.20;
                default -> weatherMultiplier = 1.00;
            }
        }
        price *= weatherMultiplier;

        return Math.round(price * 100.0) / 100.0;
    }

    @AssertTrue(message = "Les places disponibles ne peuvent pas d�passer la capacit� totale")
    public boolean isValidAvailableSeats() {
        if (availableSeats == null || totalCapacity == null) return true;
        return availableSeats >= 0 && availableSeats <= totalCapacity;
    }

}
