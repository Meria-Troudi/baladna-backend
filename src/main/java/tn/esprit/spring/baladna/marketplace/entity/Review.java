package tn.esprit.spring.baladna.marketplace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "review")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idReview;

    @Column(nullable = false)
    private Integer idUser;

    @Column(nullable = false)
    private Integer idProduit;

    private Integer idCommande;

    // Rating between 1 and 5
    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }
}
