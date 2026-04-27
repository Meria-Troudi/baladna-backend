package tn.esprit.spring.baladna.marketplace.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SpeechToTextService — Baladna IA
 *
 * La reconnaissance vocale se fait côté frontend (Web Speech API, gratuit, natif dans Chrome/Edge).
 * Ce service backend reçoit le texte déjà transcrit et le prépare pour le traitement.
 *
 * Workflow :
 *   1. Frontend : Web Speech API capture la voix → transcrit en texte
 *   2. Frontend : envoie le texte transcrit à /api/ai/speech
 *   3. Ce service : normalise + transmet à GptAiService
 *   4. GptAiService : traite et retourne l'action JSON ou la réponse
 */
@Service
@Slf4j
public class SpeechToTextService {

    private final GptAiService gptAiService;

    public SpeechToTextService(GptAiService gptAiService) {
        this.gptAiService = gptAiService;
    }

    /**
     * Traite le texte transcrit par le Web Speech API frontend.
     * @param transcribedText Texte brut issu de la reconnaissance vocale
     * @param artisanContext  Contexte de l'artisan connecté
     * @return Réponse IA (JSON action ou texte)
     */
    public String processTranscribedText(String transcribedText, String artisanContext) {
        if (transcribedText == null || transcribedText.isBlank()) {
            log.warn("⚠️ Texte transcrit vide");
            return "{\"error\":\"Aucun texte reçu. Vérifiez votre microphone.\"}";
        }

        // Normalisation du texte vocal (la voix génère parfois des variations)
        String cleaned = normalizeVoiceInput(transcribedText);
        log.info("🎤 Texte vocal reçu: '{}' → nettoyé: '{}'", transcribedText, cleaned);

        // Délégation au moteur IA principal
        return gptAiService.chat(cleaned, artisanContext);
    }

    /**
     * Normalise les erreurs de transcription vocale communes en français/arabe tunisien.
     */
    private String normalizeVoiceInput(String text) {
        return text
            // Corrections orthographiques fréquentes via voix
            .replaceAll("(?i)\\bajouter\\b", "ajoute")
            .replaceAll("(?i)\\bcréer\\b", "ajoute")
            .replaceAll("(?i)\\bnouveaux?\\s+produits?\\b", "ajoute")
            .replaceAll("(?i)\\bdinard\\b", "dinars")   // erreur TTS "dinard"
            .replaceAll("(?i)\\bDT\\b", "dinars")
            .replaceAll("(?i)\\bTND\\b", "dinars")
            .replaceAll("(?i)\\bpieces\\b", "pieces")
            .replaceAll("(?i)\\bunités\\b", "unites")
            // Chiffres écrits en lettres (fréquent avec la voix)
            .replaceAll("(?i)\\bzéro\\b", "0")
            .replaceAll("(?i)\\bun\\b(?=\\s+\\w)", "1")
            .replaceAll("(?i)\\bdeux\\b", "2")
            .replaceAll("(?i)\\btrois\\b", "3")
            .replaceAll("(?i)\\bquatre\\b", "4")
            .replaceAll("(?i)\\bcinq\\b", "5")
            .replaceAll("(?i)\\bsix\\b", "6")
            .replaceAll("(?i)\\bsept\\b", "7")
            .replaceAll("(?i)\\bhuit\\b", "8")
            .replaceAll("(?i)\\bneuf\\b", "9")
            .replaceAll("(?i)\\bdix\\b", "10")
            .replaceAll("(?i)\\bvingt\\b", "20")
            .replaceAll("(?i)\\btrente\\b", "30")
            .replaceAll("(?i)\\bquarante\\b", "40")
            .replaceAll("(?i)\\bcinquante\\b", "50")
            .replaceAll("(?i)\\bsoixante\\b", "60")
            .replaceAll("(?i)\\bquatre-vingts?\\b", "80")
            .replaceAll("(?i)\\bcent\\b", "100")
            .trim();
    }
}
