package tn.esprit.spring.baladna.transport.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import tn.esprit.spring.baladna.user.entity.User;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "stations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le nom de la station est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "La ville est obligatoire")
    @Size(min = 2, max = 100, message = "La ville doit contenir entre 2 et 100 caractères")
    @Column(nullable = false, length = 100)
    private String city;

    @NotNull(message = "La surcharge est obligatoire")
    @DecimalMin(value = "0.0", message = "La surcharge doit être >= 0")
    @DecimalMax(value = "100.0", message = "La surcharge doit être <= 100")
    @Column(nullable = false)
    private Double surcharge;

    @NotNull(message = "Le champ downtown est obligatoire")
    @Column(nullable = false)
    private Boolean downtown;

    @NotNull(message = "La latitude est obligatoire")
    @DecimalMin(value = "-90.0", message = "La latitude doit etre >= -90")
    @DecimalMax(value = "90.0", message = "La latitude doit etre <= 90")
    @Column(nullable = false)
    private Double latitude;

    @NotNull(message = "La longitude est obligatoire")
    @DecimalMin(value = "-180.0", message = "La longitude doit etre >= -180")
    @DecimalMax(value = "180.0", message = "La longitude doit etre <= 180")
    @Column(nullable = false)
    private Double longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    @JsonIgnore
    private User host;

    @OneToMany(mappedBy = "departureStation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<Trajet> departureTrajets = new ArrayList<>();

    @OneToMany(mappedBy = "arrivalStation", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<Trajet> arrivalTrajets = new ArrayList<>();

    public double getPriceWithSurcharge(double basePrice) {
        return basePrice + (surcharge != null ? surcharge : 0.0);
    }
}
