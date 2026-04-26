package tn.esprit.spring.baladna.marketplace.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.marketplace.dto.request.ReviewRequest;
import tn.esprit.spring.baladna.marketplace.dto.response.ReviewResponse;
import tn.esprit.spring.baladna.marketplace.service.ReviewService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReviewController {

    private final ReviewService reviewService;

    // POST /reviews → créer un avis
    @PostMapping("/reviews")
    public ResponseEntity<ReviewResponse> createReview(@Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.createReview(request));
    }

    // GET /products/{id}/reviews → avis d'un produit
    @GetMapping("/products/{id}/reviews")
    public ResponseEntity<List<ReviewResponse>> getReviewsByProduct(@PathVariable Integer id) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(id));
    }

    // GET /reviews/product/{id} → alias for frontend compatibility
    @GetMapping("/reviews/product/{id}")
    public ResponseEntity<List<ReviewResponse>> getReviewsByProductAlias(@PathVariable Integer id) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(id));
    }
}
