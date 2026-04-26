package tn.esprit.spring.baladna.marketplace.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.marketplace.dto.request.CheckoutRequest;
import tn.esprit.spring.baladna.marketplace.dto.response.OrderResponse;
import tn.esprit.spring.baladna.marketplace.entity.StatutCommande;
import tn.esprit.spring.baladna.marketplace.service.OrderService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * POST /orders/checkout
     * Simule le paiement + crée la commande en DB avec statut CONFIRMED
     */
    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(@RequestBody CheckoutRequest request) {
        OrderResponse response = orderService.checkout(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /orders/user/{userId}
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<OrderResponse>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getOrdersByUser(userId));
    }

    /**
     * GET /orders/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    /**
     * GET /orders/artisan/{idArtisan}
     */
    @GetMapping("/artisan/{idArtisan}")
    public ResponseEntity<List<OrderResponse>> getByArtisan(@PathVariable Integer idArtisan) {
        return ResponseEntity.ok(orderService.getOrdersByArtisan(idArtisan));
    }

    /**
     * PUT /orders/{id}/statut
     * Body: { "statut": "PREPARING" }
     * Transitions: CONFIRMED→PREPARING→SHIPPED→IN_TRANSIT→DELIVERED
     */
    @PutMapping("/{id}/statut")
    public ResponseEntity<?> updateStatut(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        try {
            StatutCommande newStatut = StatutCommande.valueOf(body.get("statut"));
            OrderResponse updated = orderService.updateStatut(id, newStatut);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Statut invalide : " + body.get("statut")));
        }
    }
}