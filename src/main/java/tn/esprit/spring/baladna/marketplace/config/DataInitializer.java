package tn.esprit.spring.baladna.marketplace.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tn.esprit.spring.baladna.marketplace.entity.Categorie;
import tn.esprit.spring.baladna.marketplace.entity.Product;
import tn.esprit.spring.baladna.marketplace.repository.CategorieRepository;
import tn.esprit.spring.baladna.marketplace.repository.ProductRepository;

import java.math.BigDecimal;

/**
 * Charge des données de test au démarrage si la base est vide.
 * Supprime cette classe en production.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final CategorieRepository categorieRepository;
    private final ProductRepository productRepository;

    @Override
    public void run(String... args) {
        if (categorieRepository.count() == 0) {
            log.info("=== Chargement des données de test ===");

            // Catégories principales pour votre marketplace chic

            Categorie c1 = categorieRepository.save(Categorie.builder()
                    .nomCategorie("🖼️ Œuvres d'art")
                    .build());

            Categorie c2 = categorieRepository.save(Categorie.builder()
                    .nomCategorie("🏺 Poterie d'exception")
                    .build());

            Categorie c3 = categorieRepository.save(Categorie.builder()
                    .nomCategorie("🌿 Soins précieux")
                    .build());

            Categorie c4 = categorieRepository.save(Categorie.builder()
                    .nomCategorie("🍯 Trésors gourmands")
                    .build());

            Categorie c5 = categorieRepository.save(Categorie.builder()
                    .nomCategorie("🍰 Pâtisserie fine")
                    .build());

            Categorie c6 = categorieRepository.save(Categorie.builder()
                    .nomCategorie("🍽️ Cuisine prête à déguster")
                    .build());

            productRepository.save(Product.builder()
                    .nomProduit("Brik à l'oeuf")
                    .descriptionProduit("Brik tunisienne traditionnelle, croustillante et dorée")
                    .prixProduit(new BigDecimal("2.50"))
                    .stockProduit(50)
                    .idCategorie(c1.getIdCategorie())
                    .idArtisan(1)
                    .build());

            productRepository.save(Product.builder()
                    .nomProduit("Makroudh")
                    .descriptionProduit("Gâteau tunisien aux dattes et semoule, frit et mielé")
                    .prixProduit(new BigDecimal("1.80"))
                    .stockProduit(100)
                    .idCategorie(c2.getIdCategorie())
                    .idArtisan(1)
                    .build());

            productRepository.save(Product.builder()
                    .nomProduit("Harissa maison")
                    .descriptionProduit("Harissa artisanale préparée avec des piments frais")
                    .prixProduit(new BigDecimal("4.00"))
                    .stockProduit(30)
                    .idCategorie(c3.getIdCategorie())
                    .idArtisan(2)
                    .build());

            productRepository.save(Product.builder()
                    .nomProduit("Ojja merguez")
                    .descriptionProduit("Ojja traditionnelle avec merguez maison et tomates fraîches")
                    .prixProduit(new BigDecimal("7.50"))
                    .stockProduit(20)
                    .idCategorie(c1.getIdCategorie())
                    .idArtisan(2)
                    .build());

            log.info("=== Données de test chargées avec succès ===");
        }
    }
}
