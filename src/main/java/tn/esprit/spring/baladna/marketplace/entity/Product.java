package tn.esprit.spring.baladna.marketplace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idProduit;

    @Column(nullable = false, length = 200)
    private String nomProduit;

    @Column(columnDefinition = "TEXT")
    private String descriptionProduit;

    @Column(columnDefinition = "TEXT")
    private String imageProduit;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixProduit;

    @Column(nullable = false)
    private Integer stockProduit;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    private LocalDateTime updatedAt;

    // Foreign keys as plain integers (microservices-compatible)
    private Integer idCategorie;
    private Integer idArtisan;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
