package tn.esprit.spring.baladna.marketplace.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_image")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idImage;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    private Integer displayOrder;

    @Column(nullable = false)
    private Integer idProduit;
}
