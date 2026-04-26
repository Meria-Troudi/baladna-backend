package tn.esprit.spring.baladna.marketplace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "favoris")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favoris {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idFavoris;

    @Column(nullable = false)
    private Integer idUser;

    @Column(nullable = false)
    private Integer idProduit;

    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }
}
