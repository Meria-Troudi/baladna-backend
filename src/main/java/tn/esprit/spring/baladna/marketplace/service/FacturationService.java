package tn.esprit.spring.baladna.marketplace.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.baladna.marketplace.dto.response.FactureResponse;
import tn.esprit.spring.baladna.marketplace.entity.Commande;
import tn.esprit.spring.baladna.marketplace.entity.Facture;
import tn.esprit.spring.baladna.marketplace.entity.LigneCommande;
import tn.esprit.spring.baladna.marketplace.repository.FactureRepository;
import tn.esprit.spring.baladna.marketplace.repository.LigneCommandeRepository;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class FacturationService {

    private final FactureRepository factureRepository;
    private final PdfGenerationService pdfService;
    private final EmailService emailService;
    private final UserRepository userRepository;
    private final LigneCommandeRepository ligneCommandeRepository;

    public FactureResponse genererEtEnvoyerFacture(Commande commande) {
        // 1. Récupérer le client
        User client = userRepository.findById(commande.getUserId())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        // 2. Créer l'entité Facture
        Facture facture = createFacture(commande, client);

        // 3. Numéro de facture
        facture.setNumeroFacture(generateFactureNumber());

        // 4. Sauvegarder
        facture = factureRepository.save(facture);

        // 5. Générer le PDF
        List<LigneCommande> lignes = ligneCommandeRepository.findByIdCommande(commande.getId());
        byte[] pdfContent = pdfService.generateFacturePdf(facture, commande, client, lignes);

        // 6. Envoyer l'email
        try {
            String emailBody = buildEmailBody(facture, client);
            emailService.sendFactureEmail(
                    client.getEmail(),
                    "Votre facture Baladna - " + facture.getNumeroFacture(),
                    emailBody,
                    pdfContent,
                    facture.getNumeroFacture()
            );
            System.out.println("✅ Email envoyé à : " + client.getEmail());
        } catch (Exception e) {
            System.err.println("⚠️ Erreur envoi email : " + e.getMessage());
            // Ne pas bloquer si l'email échoue
        }

        // 7. Réponse
        return FactureResponse.builder()
                .id(facture.getId())
                .numeroFacture(facture.getNumeroFacture())
                .commandeId(commande.getId())
                .touristeId(client.getId())
                .nomTouriste(client.getFirstName() + " " + client.getLastName())
                .emailTouriste(client.getEmail())
                .dateEmission(facture.getDateEmission())
                .montantHT(facture.getMontantHT())
                .tva(facture.getTva())
                .montantTTC(facture.getMontantTTC())
                .statut(facture.getStatut())
                .pdfBase64(Base64.getEncoder().encodeToString(pdfContent))
                .message("Facture générée avec succès")
                .build();
    }

    private Facture createFacture(Commande commande, User client) {
        Facture facture = new Facture();
        facture.setCommande(commande);
        facture.setTouriste(client);
        facture.setStatut("PAYEE");

        BigDecimal totalHT = BigDecimal.valueOf(commande.getTotal());
        BigDecimal tva = totalHT.multiply(new BigDecimal("0.20"));
        BigDecimal totalTTC = totalHT.add(tva);

        facture.setMontantHT(totalHT);
        facture.setTva(tva);
        facture.setMontantTTC(totalTTC);
        facture.setDateEmission(LocalDateTime.now());

        return facture;
    }

    private String generateFactureNumber() {
        String year = String.valueOf(LocalDateTime.now().getYear());
        long count = factureRepository.count() + 1;
        return String.format("FACT-%s-%04d", year, count);
    }

    private String buildEmailBody(Facture facture, User client) {
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background-color: #3B82F6; padding: 20px; border-radius: 10px 10px 0 0; text-align: center;">
                    <h1 style="color: white; margin: 0;">BALADNA</h1>
                    <p style="color: white; margin: 5px 0;">Your Travel Guide</p>
                </div>
                <div style="background-color: #ffffff; padding: 30px; border: 1px solid #e5e7eb; border-top: none;">
                    <h2 style="color: #1E293B;">Thank you for your order!</h2>
                    <p style="color: #64748B;">Dear %s,</p>
                    <p style="color: #64748B;">Your invoice <strong style="color: #1E293B;">n°%s</strong> is ready.</p>
                    <div style="background-color: #F0FDF4; border: 1px solid #22C55E; border-radius: 8px; padding: 15px; margin: 20px 0; text-align: center;">
                        <p style="color: #16A34A; font-size: 18px; font-weight: bold; margin: 0;">Total: %.2f TND</p>
                    </div>
                    <p style="color: #64748B;">You will find your invoice attached to this email.</p>
                    <p style="color: #64748B;">If you have any questions, contact us at <a href="mailto:support@baladna.com" style="color: #3B82F6;">support@baladna.com</a></p>
                    <br>
                    <p style="color: #64748B;">Best regards,</p>
                    <p style="color: #1E293B; font-weight: bold;">The Baladna Team</p>
                </div>
                <div style="background-color: #F8FAFC; padding: 15px; text-align: center; border-radius: 0 0 10px 10px; border: 1px solid #e5e7eb; border-top: none;">
                    <p style="color: #94A3B8; font-size: 12px; margin: 0;">This email was sent by Baladna. © 2026 All rights reserved.</p>
                </div>
            </body>
            </html>
            """,
                client.getFirstName(),
                facture.getNumeroFacture(),
                facture.getMontantTTC()
        );
    }

    public byte[] getFacturePdf(String factureNumber) {
        Facture facture = factureRepository.findByNumeroFacture(factureNumber)
                .orElseThrow(() -> new RuntimeException("Facture introuvable"));
        User client = facture.getTouriste();
        Commande commande = facture.getCommande();
        List<LigneCommande> lignes = ligneCommandeRepository.findByIdCommande(commande.getId());
        return pdfService.generateFacturePdf(facture, commande, client, lignes);
    }
}