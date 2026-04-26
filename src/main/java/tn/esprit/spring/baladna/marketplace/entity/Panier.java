package tn.esprit.spring.baladna.marketplace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Panier = Cart
 * status possible values: ACTIVE, ABANDONED, ORDERED
 */
@Entity
@Table(name = "panier")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Panier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idPanier;

    @Column(nullable = false)
    private Integer idUser;

    private LocalDateTime dateCreation;
    private LocalDateTime lastUpdated;

    // "ACTIVE", "ABANDONED", "ORDERED" - using String instead of Enum for flexibility
    @Column(nullable = false, length = 20)
    private String status;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
        if (this.status == null) this.status = "ACTIVE";
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }
}
