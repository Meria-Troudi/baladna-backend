package tn.esprit.spring.baladna.marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.*;

@RestController
@RequestMapping("/api/visual-search")
@CrossOrigin(origins = "*")
public class VisualSearchController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String PYTHON_SEARCH_URL = "http://localhost:8002/search";

    @PostMapping("/search")
    public ResponseEntity<?> searchByImage(@RequestParam("file") MultipartFile file) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() { return file.getOriginalFilename(); }
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    PYTHON_SEARCH_URL, HttpMethod.POST, entity, Map.class);

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/product-features")
    public ResponseEntity<?> getProductFeatures() {
        try {
            List<Map<String, Object>> products = jdbcTemplate.queryForList(
                    "SELECT id_produit, nom_produit, prix_produit, image_produit, id_categorie FROM product WHERE image_produit IS NOT NULL AND image_produit != ''"
            );

            Map<String, Object> metadata = new HashMap<>();
            for (Map<String, Object> p : products) {
                Map<String, Object> meta = new HashMap<>();
                meta.put("name", p.get("nom_produit"));
                meta.put("price", p.get("prix_produit"));
                meta.put("image", p.get("image_produit"));
                meta.put("category", p.get("id_categorie"));
                metadata.put(p.get("id_produit").toString(), meta);
            }

            return ResponseEntity.ok(Map.of("metadata", metadata, "count", products.size()));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("metadata", new HashMap<>(), "count", 0));
        }
    }
}