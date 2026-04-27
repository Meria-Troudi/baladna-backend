package tn.esprit.spring.baladna.marketplace.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.marketplace.dto.request.FavorisRequest;
import tn.esprit.spring.baladna.marketplace.dto.response.FavorisResponse;
import tn.esprit.spring.baladna.marketplace.service.FavorisService;

import java.util.List;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FavorisController {

    private final FavorisService favorisService;

    // POST /favorites → ajouter aux favoris
    @PostMapping
    public ResponseEntity<FavorisResponse> addFavoris(@Valid @RequestBody FavorisRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(favorisService.addFavoris(request));
    }

    // DELETE /favorites → retirer des favoris
    @DeleteMapping
    public ResponseEntity<Void> removeFavoris(@Valid @RequestBody FavorisRequest request) {
        favorisService.removeFavoris(request);
        return ResponseEntity.noContent().build();
    }

    // GET /favorites/{userId} → tous les favoris d'un utilisateur
    @GetMapping("/{userId}")
    public ResponseEntity<List<FavorisResponse>> getFavorisByUser(@PathVariable Integer userId) {
        return ResponseEntity.ok(favorisService.getFavorisByUser(userId));
    }
}
