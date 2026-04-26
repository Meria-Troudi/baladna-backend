package tn.esprit.spring.baladna.marketplace.controller;

import tn.esprit.spring.baladna.marketplace.service.FacturationService;
import tn.esprit.spring.baladna.marketplace.service.CommandeService;
import tn.esprit.spring.baladna.marketplace.entity.Commande;
import tn.esprit.spring.baladna.marketplace.dto.response.FactureResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/factures")
@CrossOrigin(origins = "http://localhost:4200")
public class FactureController {

    @Autowired
    private FacturationService facturationService;

    @Autowired
    private CommandeService commandeService;

    @PostMapping("/generer/{commandeId}")
    public ResponseEntity<?> genererFacture(@PathVariable Long commandeId) {
        try {
            Commande commande = commandeService.getCommandeById(commandeId);
            FactureResponse facture = facturationService.genererEtEnvoyerFacture(commande);
            return ResponseEntity.ok(facture);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur: " + e.getMessage());
        }
    }

    @GetMapping("/telecharger/{factureNumber}")
    public ResponseEntity<byte[]> telechargerFacture(@PathVariable String factureNumber) {
        byte[] pdfContent = facturationService.getFacturePdf(factureNumber);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename("Facture_" + factureNumber + ".pdf")
                .build());

        return new ResponseEntity<>(pdfContent, headers, HttpStatus.OK);
    }




}