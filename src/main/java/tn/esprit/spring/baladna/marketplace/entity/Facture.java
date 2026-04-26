package tn.esprit.spring.baladna.marketplace.entity;

import jakarta.persistence.*;
import lombok.Data;
import tn.esprit.spring.baladna.user.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "factures")
public class Facture {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String numeroFacture;

    @ManyToOne
    @JoinColumn(name = "commande_id")
    private Commande commande;

    @ManyToOne
    @JoinColumn(name = "touriste_id")   // maintenant relié à User (table users)
    private User touriste;

    private LocalDateTime dateEmission;
    private BigDecimal montantHT;
    private BigDecimal tva;
    private BigDecimal montantTTC;
    private String statut; // PAYEE, EN_ATTENTE, ANNULEE

    @Column(columnDefinition = "TEXT")
    private String pdfPath;

    @PrePersist
    protected void onCreate() {
        dateEmission = LocalDateTime.now();
    }
}