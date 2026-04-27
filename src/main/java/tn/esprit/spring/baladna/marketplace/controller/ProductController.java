package tn.esprit.spring.baladna.marketplace.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.spring.baladna.marketplace.dto.request.ProductRequest;
import tn.esprit.spring.baladna.marketplace.dto.response.ProductResponse;
import tn.esprit.spring.baladna.marketplace.service.CloudinaryService;
import tn.esprit.spring.baladna.marketplace.service.ProductService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductService productService;
    private final CloudinaryService cloudinaryService;

    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Integer id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/artisan/{idArtisan}")
    public ResponseEntity<List<ProductResponse>> getProductsByArtisan(@PathVariable Integer idArtisan) {
        return ResponseEntity.ok(productService.getProductsByArtisan(idArtisan));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Integer id,
            @Valid @RequestBody ProductRequest request,
            @RequestParam Integer currentArtisanId) {
        return ResponseEntity.ok(productService.updateProduct(id, request, currentArtisanId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Integer id,
            @RequestParam Integer currentArtisanId) {
        productService.deleteProduct(id, currentArtisanId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categorie/{idCategorie}")
    public ResponseEntity<List<ProductResponse>> getByCategorie(@PathVariable Integer idCategorie) {
        return ResponseEntity.ok(productService.getProductsByCategorie(idCategorie));
    }

    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = cloudinaryService.uploadFile(file);
            Map<String, String> response = new HashMap<>();
            response.put("url", url);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}