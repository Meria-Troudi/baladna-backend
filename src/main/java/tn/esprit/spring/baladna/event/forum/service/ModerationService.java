package tn.esprit.spring.baladna.event.forum.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Lightweight content moderation using a local Ollama model.
 * The model classifies free-form text into SAFE / TOXIC / SPAM and returns a short reason.
 * <p>
 * Design notes:
 * - Fails open: on any error (Ollama down, timeout, bad JSON) the post is treated as SAFE
 *   so moderation never blocks regular posting.
 * - No streaming, low temperature, JSON-only output for deterministic parsing.
 */
@Service
public class ModerationService {

    private static final Logger log = LoggerFactory.getLogger(ModerationService.class);

    public enum Label { SAFE, TOXIC, SPAM }

    public record Verdict(Label label, String reason) {
        public boolean isBlocking() { return label == Label.TOXIC || label == Label.SPAM; }
    }

    private static final String SYSTEM_PROMPT = """
            You are a strict content moderation classifier for a community forum about travel,\s
            culture, and local life in Tunisia. Classify the user post into exactly ONE of:
            - "safe": normal, acceptable community content. Includes personal stories, travel tips,
              restaurant / cafe / hotel recommendations by regular users, cultural discussions,
              questions, opinions, reviews. These are SAFE, not spam.
            - "toxic": hate speech, harassment, threats, slurs, sexual content, graphic violence,
              telling someone to kill themselves.
            - "spam": overt advertising of products, scams, unsolicited promotional links, crypto
              pump schemes, "click here buy now" patterns, repeated gibberish.
            
            Respond ONLY with a compact JSON object of the shape:
            {"label": "safe" | "toxic" | "spam", "reason": "short explanation under 120 chars"}
            No prose, no markdown, no code fences. JSON only.
            """;

    private final boolean enabled;
    private final String model;
    private final RestClient restClient;

    public ModerationService(
            @Value("${ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ollama.model:gemma3:1b}") String model,
            @Value("${moderation.enabled:true}") boolean enabled,
            @Value("${ollama.timeout-ms:12000}") int timeoutMs) {
        this.model = model;
        this.enabled = enabled;

        java.net.http.HttpClient http = java.net.http.HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        org.springframework.http.client.JdkClientHttpRequestFactory factory =
                new org.springframework.http.client.JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    /** Classify text. Returns SAFE on any failure so posting is never blocked. */
    public Verdict classify(String content) {
        if (!enabled) return new Verdict(Label.SAFE, "moderation disabled");
        if (content == null || content.isBlank()) return new Verdict(Label.SAFE, "empty");

        String trimmed = content.length() > 2000 ? content.substring(0, 2000) : content;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("stream", false);
        body.put("format", "json");
        body.put("system", SYSTEM_PROMPT);
        body.put("prompt", "Post to classify:\n\n" + trimmed);
        body.put("options", Map.of("temperature", 0.0, "num_ctx", 2048));

        try {
            Map<String, Object> outer = restClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (outer == null) {
                log.warn("Moderation: null body from Ollama, defaulting to SAFE");
                return new Verdict(Label.SAFE, "model returned empty response");
            }

            String response = String.valueOf(outer.getOrDefault("response", "")).trim();
            if (response.isBlank()) {
                log.warn("Moderation: empty response from Ollama, defaulting to SAFE");
                return new Verdict(Label.SAFE, "model returned empty response");
            }

            Map<String, String> inner = parseSimpleJsonObject(response);
            String label = inner.getOrDefault("label", "safe").trim().toLowerCase(Locale.ROOT);
            String reason = inner.getOrDefault("reason", "").trim();
            if (reason.length() > 240) reason = reason.substring(0, 240);

            // Tolerant matching: small models sometimes emit "toxic|hate", "toxic / spam",
            // or squeeze extra words into the label field. Treat any occurrence as the verdict.
            Label parsed;
            if (label.contains("toxic")) parsed = Label.TOXIC;
            else if (label.contains("spam")) parsed = Label.SPAM;
            else parsed = Label.SAFE;
            log.info("Moderation verdict: {} ({})", parsed, reason);
            return new Verdict(parsed, reason.isBlank() ? parsed.name().toLowerCase(Locale.ROOT) : reason);

        } catch (Exception e) {
            log.warn("Moderation call failed ({}), defaulting to SAFE", e.getMessage());
            return new Verdict(Label.SAFE, "moderation unavailable");
        }
    }

    /**
     * Minimal parser for the flat {"label": "...", "reason": "..."} shape that the prompt forces.
     * Avoids a Jackson dependency on the compile classpath. Very forgiving: on any error we
     * return an empty map so the caller falls back to SAFE.
     */
    static Map<String, String> parseSimpleJsonObject(String json) {
        Map<String, String> out = new LinkedHashMap<>();
        if (json == null) return out;
        int start = json.indexOf('{');
        int end = json.lastIndexOf('}');
        if (start < 0 || end <= start) return out;
        String body = json.substring(start + 1, end);

        int i = 0;
        while (i < body.length()) {
            int keyStart = body.indexOf('"', i);
            if (keyStart < 0) break;
            int keyEnd = body.indexOf('"', keyStart + 1);
            if (keyEnd < 0) break;
            String key = body.substring(keyStart + 1, keyEnd);

            int colon = body.indexOf(':', keyEnd + 1);
            if (colon < 0) break;

            int valStart = colon + 1;
            while (valStart < body.length() && Character.isWhitespace(body.charAt(valStart))) valStart++;
            if (valStart >= body.length()) break;

            String value;
            if (body.charAt(valStart) == '"') {
                StringBuilder sb = new StringBuilder();
                int j = valStart + 1;
                while (j < body.length()) {
                    char c = body.charAt(j);
                    if (c == '\\' && j + 1 < body.length()) {
                        char n = body.charAt(j + 1);
                        sb.append(switch (n) {
                            case 'n' -> '\n'; case 't' -> '\t'; case 'r' -> '\r';
                            case '"' -> '"'; case '\\' -> '\\'; default -> n;
                        });
                        j += 2;
                    } else if (c == '"') {
                        break;
                    } else {
                        sb.append(c); j++;
                    }
                }
                value = sb.toString();
                i = j + 1;
            } else {
                int comma = body.indexOf(',', valStart);
                int stop = comma < 0 ? body.length() : comma;
                value = body.substring(valStart, stop).trim();
                i = stop + 1;
            }

            out.put(key, value);
            int nextComma = body.indexOf(',', i);
            if (nextComma < 0) break;
            i = nextComma + 1;
        }
        return out;
    }
}
