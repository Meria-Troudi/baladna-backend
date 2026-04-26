package tn.esprit.spring.baladna.marketplace.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * UnsplashService — Recherche d'images automatiques pour les produits Baladna.
 *
 * Utilise l'API Unsplash (gratuite, 50 requêtes/heure).
 * Quand l'IA ajoute un produit, on cherche une image correspondante.
 */
@Service
@Slf4j
public class UnsplashService {

    @Value("${unsplash.access.key}")
    private String accessKey;

    private static final String UNSPLASH_API = "https://api.unsplash.com";
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient client = HttpClient.newHttpClient();

    /**
     * Recherche une image pour un produit et retourne l'URL.
     * Si aucune image trouvée, retourne une image par défaut d'Unsplash.
     */
    public String searchProductImage(String productName, String category) {
        try {
            String query = buildQuery(productName, category);
            String url = UNSPLASH_API + "/search/photos?query="
                    + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&per_page=3";

            log.info("🔍 Unsplash query URL: {}", url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Client-ID " + accessKey)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("🔍 Unsplash response code: {}", response.statusCode());

            if (response.statusCode() == 200) {
                JsonNode root = mapper.readTree(response.body());
                JsonNode results = root.get("results");

                log.info("🔍 Unsplash total results: {}", root.get("total").asInt());

                if (results != null && results.isArray() && results.size() > 0) {
                    String imageUrl = results.get(0).get("urls").get("regular").asText();
                    log.info("✅ Unsplash image found: {}", imageUrl);
                    return imageUrl;
                } else {
                    log.warn("⚠️ No results found for query: {}", query);
                }
            } else {
                log.warn("⚠️ Unsplash API error {} : {}", response.statusCode(), response.body());
            }

        } catch (Exception e) {
            log.error("❌ Unsplash error: {}", e.getMessage(), e);
        }

        return getDefaultImage(category);
    }
    /**
     * Construit une requête de recherche optimisée.
     */
    private String buildQuery(String productName, String category) {
        StringBuilder sb = new StringBuilder();

        // Ajouter le nom du produit (priorité)
        if (productName != null && !productName.isBlank()) {
            sb.append(productName);
        }

        // Ajouter la catégorie en anglais
        String englishCategory = translateCategory(category);
        if (englishCategory != null) {
            sb.append(" ").append(englishCategory);
        }

        // Si la requête est courte, ajouter un terme générique
        String query = sb.toString().trim();
        if (query.length() < 10) {
            query = query + " artisan";
        }

        log.info("🔍 Search query: {}", query);
        return query;
    }

    /**
     * Traduit la catégorie en anglais pour l'API Unsplash.
     */
    private String translateCategory(String category) {
        if (category == null) return "handicraft";
        return switch (category.toLowerCase()) {
            case "ceramics", "🏺 ceramics" -> "pottery ceramic";
            case "art", "🖼️ art" -> "artisan craft";
            case "jewelry", "💎 jewelry" -> "jewelry";
            case "wellness", "🌿 wellness" -> "natural product";
            case "pastry", "🍰 pastry" -> "pastry food";
            case "ready meals", "🍽️ ready meals" -> "food meal";
            case "fashion", "👗 fashion" -> "clothing fashion";
            case "gifts", "🎁 gifts" -> "handicraft gift";
            case "home decor", "🏠home decor" -> "home decoration";
            default -> category.toLowerCase();
        };
    }

    /**
     * Retourne une image par défaut selon la catégorie.
     */
    public String getDefaultImage(String category) {
        return switch (category != null ? category.toLowerCase() : "artisanat") {
            case "poterie"    -> "https://images.unsplash.com/photo-1565193566173-7a0ee3dbe261?w=600";
            case "bijouterie" -> "https://images.unsplash.com/photo-1602173574767-37ac01994b2a?w=600";
            case "tissage"    -> "https://images.unsplash.com/photo-1618221469555-7f3adf40b7b0?w=600";
            case "plantes"    -> "https://images.unsplash.com/photo-1466692476868-aef1dfb1e735?w=600";
            case "bois"       -> "https://images.unsplash.com/photo-1518531933037-91b2f5f229cc?w=600";
            case "alimentaire"-> "https://images.unsplash.com/photo-1509365465985-25d11c17e812?w=600";
            case "cuir"       -> "https://images.unsplash.com/photo-1601924638867-3a6de6b7a500?w=600";
            default           -> "https://images.unsplash.com/photo-1441986300917-64674bd600d8?w=600";
        };
    }

    // ==================================================================
//  UTILITAIRE : parsing robuste de prix (supporte "45,00" et "45.00")
// ==================================================================
    private double parseDouble(JsonNode node) {
        if (node == null) return 0.0;
        try {
            String text = node.asText().replace(",", ".").trim();
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return node.asDouble(0.0);
        }
    }





}