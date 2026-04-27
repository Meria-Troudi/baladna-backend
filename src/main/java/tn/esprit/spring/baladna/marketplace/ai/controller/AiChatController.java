package tn.esprit.spring.baladna.marketplace.ai.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.marketplace.ai.service.GptAiService;
import tn.esprit.spring.baladna.marketplace.ai.service.ProductActionService;
import tn.esprit.spring.baladna.marketplace.ai.service.SpeechToTextService;

import java.util.Map;

/**
 * AiChatController — Endpoints REST pour l'assistant IA Baladna
 *
 * POST /api/ai/chat    → message texte → IA → action BD
 * POST /api/ai/speech  → texte transcrit (voix) → IA → action BD
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
@Slf4j
public class AiChatController {

    @Autowired
    private GptAiService gptAiService;

    @Autowired
    private SpeechToTextService speechToTextService;

    @Autowired
    private ProductActionService productActionService;

    // ─────────────────────────────────────────────────────────
    //  POST /api/ai/chat
    //  Body: { "message": "ajoute plante à 28 dinars 80 de stock", "artisanId": 1 }
    // ─────────────────────────────────────────────────────────
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> body) {
        String  message   = (String) body.getOrDefault("message", "");
        Integer artisanId = (Integer) body.getOrDefault("artisanId", 1);
        String  context   = buildContext(artisanId);

        log.info("💬 /api/ai/chat — artisan={} message='{}'", artisanId, message);

        // 1. IA → JSON action ou texte
        String aiResponse = gptAiService.chat(message, context);

        // 2. Exécuter l'action en BD
        Map<String, Object> result = productActionService.executeAction(aiResponse, artisanId);
        result.put("aiRaw", aiResponse);

        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────
    //  POST /api/ai/speech
    //  Body: { "text": "ajouter une plante à vingt-huit dinars quatre-vingt pièces", "artisanId": 1 }
    // ─────────────────────────────────────────────────────────
    @PostMapping("/speech")
    public ResponseEntity<Map<String, Object>> speech(@RequestBody Map<String, Object> body) {
        String  text      = (String) body.getOrDefault("text", "");
        Integer artisanId = (Integer) body.getOrDefault("artisanId", 1);
        String  context   = buildContext(artisanId);

        log.info("🎤 /api/ai/speech — artisan={} text='{}'", artisanId, text);

        // 1. SpeechToText normalise + passe à l'IA
        String aiResponse = speechToTextService.processTranscribedText(text, context);

        // 2. Exécuter l'action en BD
        Map<String, Object> result = productActionService.executeAction(aiResponse, artisanId);
        result.put("transcribedText", text);
        result.put("aiRaw", aiResponse);

        return ResponseEntity.ok(result);
    }

    // ─────────────────────────────────────────────────────────
    //  GET /api/ai/health — vérification que le service est UP
    // ─────────────────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "Baladna AI",
            "version", "3.1"
        ));
    }

    // ─────────────────────────────────────────────────────────
    //  Contexte artisan (à enrichir selon votre modèle User)
    // ─────────────────────────────────────────────────────────
    private String buildContext(Integer artisanId) {
        return "Artisan ID=" + artisanId + " sur la marketplace Baladna (Tunisie)";
    }







}
