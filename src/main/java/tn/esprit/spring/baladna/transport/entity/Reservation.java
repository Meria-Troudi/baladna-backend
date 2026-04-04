package tn.esprit.spring.baladna.transport.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import tn.esprit.spring.baladna.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Le nombre de places est obligatoire")
    @Positive(message = "Le nombre de places doit être positif")
    @Min(value = 1, message = "Il faut réserver au moins 1 place")
    @Max(value = 20, message = "Maximum 20 places par réservation")
    @Column(nullable = false)
    private Integer reservedSeats;

    @NotNull(message = "Le prix total est obligatoire")
    @Positive(message = "Le prix total doit être positif")
    @Column(nullable = false)
    private Double totalPrice;

    @NotNull(message = "La date de réservation est obligatoire")
    @PastOrPresent(message = "La date ne peut pas être dans le futur")
    @Column(nullable = false)
    private LocalDateTime reservationDate;

    @NotBlank(message = "Le point d'embarquement est obligatoire")
    @Size(min = 2, max = 150, message = "Le point d'embarquement doit contenir entre 2 et 150 caractères")
    @Column(nullable = false)
    private String boardingPoint;

    @NotNull(message = "Le statut est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @NotNull(message = "Le transport est obligatoire")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transport_id", nullable = false)
    @JsonIgnoreProperties({"reservations"})
    private Transport transport;

    @NotNull(message = "L'utilisateur est obligatoire")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public double getPricePerSeat() {
        if (totalPrice == null || reservedSeats == null || reservedSeats == 0) {
            return 0.0;
        }
        return totalPrice / reservedSeats;
    }

    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }

    @AssertTrue(message = "Pas assez de places disponibles dans le transport")
    public boolean hasEnoughSeats() {
        if (transport == null || reservedSeats == null) return true;
        return transport.getAvailableSeats() != null
                && transport.getAvailableSeats() >= reservedSeats;
    }
}