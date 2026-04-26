package tn.esprit.spring.baladna.marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.marketplace.entity.Commande;
import tn.esprit.spring.baladna.marketplace.entity.StatutCommande;
import tn.esprit.spring.baladna.marketplace.service.CommandeService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/commandes")
@CrossOrigin(origins = "*")
public class CommandeController {

    @Autowired
    private CommandeService commandeService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Commande>> getByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(commandeService.getCommandesByUser(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Commande> getById(@PathVariable Long id) {
        return ResponseEntity.ok(commandeService.getCommandeById(id));
    }

    /**
     * PUT /commandes/{id}/statut
     * Body: { "statut": "PREPARING" }
     * Transitions valides: CONFIRMED→PREPARING→SHIPPED→IN_TRANSIT→DELIVERED
     */
    @PutMapping("/{id}/statut")
    public ResponseEntity<?> updateStatut(
            @PathVariable Long id,
            @RequestBody Map<String, String> body
    ) {
        try {
            StatutCommande newStatut = StatutCommande.valueOf(body.get("statut"));
            Commande updated = commandeService.updateStatut(id, newStatut);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Statut invalide : " + body.get("statut")));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}