package tn.esprit.spring.baladna.marketplace.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.baladna.marketplace.entity.Commande;
import tn.esprit.spring.baladna.marketplace.entity.StatutCommande;
import tn.esprit.spring.baladna.marketplace.repository.CommandeRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class CommandeService {

    @Autowired
    private CommandeRepository commandeRepository;

    // Allowed transitions
    private static final Map<StatutCommande, StatutCommande> TRANSITIONS = Map.of(
        StatutCommande.CONFIRMED,  StatutCommande.PREPARING,
        StatutCommande.PREPARING,  StatutCommande.SHIPPED,
        StatutCommande.SHIPPED,    StatutCommande.IN_TRANSIT,
        StatutCommande.IN_TRANSIT, StatutCommande.DELIVERED
    );

    public List<Commande> getCommandesByUser(Long userId) {
        return commandeRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Commande getCommandeById(Long id) {
        return commandeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Commande introuvable : " + id));
    }

    @Transactional
    public Commande updateStatut(Long id, StatutCommande newStatut) {
        Commande commande = getCommandeById(id);
        StatutCommande currentStatut = commande.getStatut();

        // Validate transition
        StatutCommande expectedNext = TRANSITIONS.get(currentStatut);
        if (expectedNext == null || !expectedNext.equals(newStatut)) {
            throw new IllegalStateException(
                "Transition invalide : " + currentStatut + " → " + newStatut +
                ". Transition attendue : " + currentStatut + " → " + expectedNext
            );
        }

        commande.setStatut(newStatut);
        commande.setUpdatedAt(LocalDateTime.now());  // ✔ updatedAt mis à jour
        return commandeRepository.save(commande);
    }
}
