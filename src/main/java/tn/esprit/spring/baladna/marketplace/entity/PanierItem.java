package tn.esprit.spring.baladna.marketplace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "panier_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PanierItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idCartItem;

    @Column(nullable = false)
    private Integer idPanier;

    @Column(nullable = false)
    private Integer idProduit;

    @Column(nullable = false)
    private Integer quantite;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prix;
}
