package tn.esprit.spring.baladna.transport.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "trajets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trajet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La station de départ est obligatoire")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "departure_station_id", nullable = false)
    private Station departureStation;

    @NotNull(message = "La station d'arrivée est obligatoire")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "arrival_station_id", nullable = false)
    private Station arrivalStation;

    @NotNull(message = "La distance est obligatoire")
    @DecimalMin(value = "1.0", message = "La distance doit être >= 1")
    @DecimalMax(value = "1000.0", message = "La distance doit être <= 1000")
    private Double distanceKm;

    @NotNull(message = "La durée estimée est obligatoire")
    @Positive(message = "La durée doit être positive")
    @Min(value = 1, message = "La durée doit être au moins 1 minute")
    private Integer estimatedDurationMinutes;

    @NotNull(message = "Le prix par km est obligatoire")
    @DecimalMin(value = "0.01", message = "Le prix par km doit être >= 0.01")
    @DecimalMax(value = "10.0", message = "Le prix par km doit être <= 10")
    private Double pricePerKm;

    @OneToMany(mappedBy = "trajet", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<Transport> transports = new ArrayList<>();

    public double getBasePrice() {
        if (distanceKm == null || pricePerKm == null) return 0.0;
        return distanceKm * pricePerKm;
    }

    @AssertTrue(message = "La station de départ doit être différente de la station d'arrivée")
    public boolean isDifferentStations() {
        if (departureStation == null || arrivalStation == null) return true;
        if (departureStation.getId() == null || arrivalStation.getId() == null) return true;
        return !departureStation.getId().equals(arrivalStation.getId());
    }
}