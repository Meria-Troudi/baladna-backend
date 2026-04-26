package tn.esprit.spring.baladna.marketplace.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.marketplace.dto.request.CartItemRequest;
import tn.esprit.spring.baladna.marketplace.dto.response.CartResponse;
import tn.esprit.spring.baladna.marketplace.service.CartService;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CartController {

    private final CartService cartService;

    // GET /cart/{userId} → voir le panier
    @GetMapping("/{userId}")
    public ResponseEntity<CartResponse> getCart(@PathVariable Integer userId) {
        return ResponseEntity.ok(cartService.getCartByUser(userId));
    }

    // POST /cart/add → ajouter un article
    @PostMapping("/add")
    public ResponseEntity<CartResponse> addItem(@Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(request));
    }

    // PUT /cart/update → modifier la quantité
    @PutMapping("/update")
    public ResponseEntity<CartResponse> updateItem(@Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.updateItem(request));
    }

    // DELETE /cart/remove → supprimer un article
    @DeleteMapping("/remove")
    public ResponseEntity<CartResponse> removeItem(@Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.removeItem(request));
    }
}
