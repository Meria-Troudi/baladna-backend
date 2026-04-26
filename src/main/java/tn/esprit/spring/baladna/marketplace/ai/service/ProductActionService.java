package tn.esprit.spring.baladna.marketplace.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class ProductActionService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UnsplashService unsplashService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================================================================
    //  ENTRY POINT
    // ==================================================================
    public Map<String, Object> executeAction(String aiResponse, Integer artisanId) {
        Map<String, Object> result = new HashMap<>();

        if (aiResponse == null || aiResponse.isBlank()) {
            result.put("success", false);
            result.put("message", "Empty AI response");
            return result;
        }

        if (!aiResponse.contains("\"action\"")) {
            result.put("success", true);
            result.put("type", "TEXT_RESPONSE");
            result.put("message", aiResponse);
            return result;
        }

        try {
            // Clean JSON: fix commas in numbers (e.g., "price":45,00 → "price":45.00)
            String cleanedJson = aiResponse
                    .replaceAll("(\\d),(\\d)", "$1.$2")
                    .replaceAll(",\\s*}", "}")
                    .trim();

            log.info("Cleaned JSON: {}", cleanedJson);
            JsonNode json = objectMapper.readTree(cleanedJson);
            String action = json.get("action").asText();

            return switch (action) {
                case "ADD_PRODUCT"    -> addProduct(json, artisanId);
                case "DELETE_PRODUCT" -> deleteProduct(json, artisanId);
                case "UPDATE_PRICE"   -> updatePrice(json, artisanId);
                case "GET_ORDERS"     -> getOrders(artisanId);
                case "GET_PRODUCTS"   -> getProducts(artisanId);
                case "GET_STATS"      -> getStats(artisanId);
                default -> {
                    result.put("success", false);
                    result.put("message", "Unknown action: " + action);
                    yield result;
                }
            };
        } catch (Exception e) {
            log.error("Failed to parse AI response: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "Error processing request: " + e.getMessage());
            return result;
        }
    }

    // ==================================================================
    //  ADD PRODUCT
    // ==================================================================
    private Map<String, Object> addProduct(JsonNode json, Integer artisanId) {
        Map<String, Object> result = new HashMap<>();
        try {
            String name     = json.has("productName") ? json.get("productName").asText() : "product";
            double price    = json.has("price")       ? parseDouble(json.get("price"))    : 0.0;
            int    stock    = json.has("stock")       ? json.get("stock").asInt()         : 1;
            String category = json.has("category")    ? json.get("category").asText()     : "handicraft";

            Integer idCategorie = resolveCategory(category);

            // Unsplash image
            String imageUrl = unsplashService.searchProductImage(name, category);
            log.info("Unsplash image for '{}': {}", name, imageUrl);

            String sql = """
                INSERT INTO product
                    (nom_produit, prix_produit, stock_produit, id_artisan, id_categorie,
                     description_produit, image_produit, date_creation, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

            LocalDateTime now = LocalDateTime.now();
            jdbcTemplate.update(sql,
                    name, price, stock, artisanId, idCategorie,
                    "Added via Baladna AI Assistant",
                    imageUrl, now, now);

            Integer newId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Integer.class);

            log.info("Product added: '{}' price={} stock={} id={}", name, price, stock, newId);

            result.put("success", true);
            result.put("type", "ADD_PRODUCT");
            result.put("productId", newId);
            result.put("productName", name);
            result.put("price", price);
            result.put("stock", stock);
            result.put("category", category);
            result.put("imageUrl", imageUrl);
            result.put("message", String.format(
                    "✅ Product \"%s\" added! Price: %.2f TND | Stock: %d | Category: %s",
                    name, price, stock, category));
        } catch (Exception e) {
            log.error("ADD_PRODUCT failed: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "Failed to add product: " + e.getMessage());
        }
        return result;
    }

    // ==================================================================
    //  DELETE PRODUCT
    // ==================================================================
    private Map<String, Object> deleteProduct(JsonNode json, Integer artisanId) {
        Map<String, Object> result = new HashMap<>();
        try {
            int productId = json.has("productId") ? json.get("productId").asInt() : 0;
            if (productId == 0) {
                result.put("success", false);
                result.put("message", "Missing product ID. Try: 'delete product 3'");
                return result;
            }

            int rows = jdbcTemplate.update(
                    "DELETE FROM product WHERE id_produit = ? AND id_artisan = ?",
                    productId, artisanId);

            if (rows > 0) {
                result.put("success", true);
                result.put("message", "✅ Product #" + productId + " deleted.");
            } else {
                result.put("success", false);
                result.put("message", "Product #" + productId + " not found or not yours.");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Delete failed: " + e.getMessage());
        }
        return result;
    }

    // ==================================================================
    //  UPDATE PRICE
    // ==================================================================
    private Map<String, Object> updatePrice(JsonNode json, Integer artisanId) {
        Map<String, Object> result = new HashMap<>();
        try {
            int    productId = json.has("productId") ? json.get("productId").asInt() : 0;
            double newPrice  = json.has("price")     ? parseDouble(json.get("price"))  : 0.0;

            if (productId == 0) {
                result.put("success", false);
                result.put("message", "Missing product ID.");
                return result;
            }

            int rows = jdbcTemplate.update(
                    "UPDATE product SET prix_produit = ?, updated_at = ? WHERE id_produit = ? AND id_artisan = ?",
                    newPrice, LocalDateTime.now(), productId, artisanId);

            if (rows > 0) {
                result.put("success", true);
                result.put("message", String.format("✅ Price for #%d updated to %.2f TND", productId, newPrice));
            } else {
                result.put("success", false);
                result.put("message", "Product not found or not yours.");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Update failed: " + e.getMessage());
        }
        return result;
    }

    // ==================================================================
    //  GET PRODUCTS
    // ==================================================================
    private Map<String, Object> getProducts(Integer artisanId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> products = jdbcTemplate.queryForList(
                    "SELECT id_produit, nom_produit, prix_produit, stock_produit, date_creation " +
                            "FROM product WHERE id_artisan = ? ORDER BY date_creation DESC LIMIT 20",
                    artisanId);

            StringBuilder sb = new StringBuilder("📦 Your products:\n");
            for (Map<String, Object> p : products) {
                sb.append(String.format("• #%s - %s - %.2f TND (stock: %s)\n",
                        p.get("id_produit"), p.get("nom_produit"),
                        p.get("prix_produit"), p.get("stock_produit")));
            }

            result.put("success", true);
            result.put("type", "GET_PRODUCTS");
            result.put("count", products.size());
            result.put("message", sb.toString());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Error fetching products: " + e.getMessage());
        }
        return result;
    }

    // ==================================================================
    //  GET ORDERS
    // ==================================================================
    private Map<String, Object> getOrders(Integer artisanId) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> orders = jdbcTemplate.queryForList(
                    "SELECT c.* FROM commandes c " +
                            "JOIN ligne_commande lc ON c.id = lc.id_commande " +
                            "JOIN product p ON lc.id_produit = p.id_produit " +
                            "WHERE p.id_artisan = ? ORDER BY c.created_at DESC LIMIT 10",
                    artisanId);

            result.put("success", true);
            result.put("type", "GET_ORDERS");
            result.put("count", orders.size());
            result.put("message", "📋 You have " + orders.size() + " recent order(s).");
        } catch (Exception e) {
            log.warn("Orders query failed: {}", e.getMessage());
            result.put("success", true);
            result.put("type", "GET_ORDERS");
            result.put("message", "No orders found.");
        }
        return result;
    }

    // ==================================================================
    //  GET STATS
    // ==================================================================
    private Map<String, Object> getStats(Integer artisanId) {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer nbProducts = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM product WHERE id_artisan = ?", Integer.class, artisanId);

            Double stockValue = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(prix_produit * stock_produit), 0) FROM product WHERE id_artisan = ?",
                    Double.class, artisanId);

            Double avgStock = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(AVG(stock_produit), 0) FROM product WHERE id_artisan = ?",
                    Double.class, artisanId);

            result.put("success", true);
            result.put("type", "GET_STATS");
            result.put("message", String.format(
                    "📊 Stats: %d products | Stock value: %.2f TND | Avg stock: %d units",
                    nbProducts != null ? nbProducts : 0,
                    stockValue != null ? stockValue : 0.0,
                    avgStock != null ? Math.round(avgStock) : 0));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Stats error: " + e.getMessage());
        }
        return result;
    }

    // ==================================================================
    //  RESOLVE CATEGORY
    // ==================================================================
    private Integer resolveCategory(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) return null;
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id_categorie FROM categorie WHERE LOWER(nom_categorie) LIKE ? LIMIT 1",
                    Integer.class, "%" + categoryName.toLowerCase() + "%");
        } catch (Exception e) {
            log.warn("Category '{}' not found", categoryName);
            return null;
        }
    }

    // ==================================================================
    //  UTILITY : parse price (supports "45,00" and "45.00")
    // ==================================================================
    private double parseDouble(JsonNode node) {
        if (node == null) return 0.0;
        try {
            return Double.parseDouble(node.asText().replace(",", ".").trim());
        } catch (NumberFormatException e) {
            return node.asDouble(0.0);
        }
    }
}