package tn.esprit.spring.baladna.event.forum.service;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tn.esprit.spring.baladna.event.forum.entity.PostTopic;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TopicInferenceService {

    private final RestClient.Builder restClientBuilder;

    @org.springframework.beans.factory.annotation.Value("${OLLAMA_BASE_URL:http://127.0.0.1:11434}")
    private String baseUrl;

    @org.springframework.beans.factory.annotation.Value("${OLLAMA_MODEL:qwen2.5:7b-instruct}")
    private String model;

    public record Result(PostTopic topic, double confidence, String reason) {}

    public Result classify(String content) {

        if (content == null || content.isBlank()) {
            return new Result(PostTopic.OTHER, 0.0, "empty");
        }

        String prompt = """
You are a STRICT single-label classifier for a Tunisian community + travel forum.

You MUST follow this decision process exactly:

========================
STEP 1 — READ INPUT
========================
Understand the post intent in 1 pass.

Do NOT explain.
Do NOT think aloud.
Do NOT output anything except final JSON.

========================
STEP 2 — DECISION PRIORITY RULES (HARD ORDER)
========================

If multiple categories match, ALWAYS choose in this order:

1. MARKETPLACE (buy/sell/job/service/price/offer)
2. ACCOMMODATION (stay/hotel/rent/booking)
3. FOOD (eat/restaurant/recipe)
4. TRAVEL (transport/directions/trip)
5. EVENT (event/festival/concert/date)
6. QUESTION (ONLY if it is general informational request not covered above)
7. OTHER (everything else)

IMPORTANT OVERRIDES:
- If "sell / buy / price / hiring / offer" → ALWAYS MARKETPLACE
- If "where to stay / hotel / Airbnb" → ALWAYS ACCOMMODATION
- If "where to eat / restaurant / dish" → ALWAYS FOOD
- If "how to go / transport / from A to B" → ALWAYS TRAVEL
- If post contains multiple intents → choose strongest ACTION intent (not sentiment)

========================
STEP 3 — CONFIDENCE RULES
========================

Confidence MUST follow these rules:

- 0.90 – 1.00 → very explicit intent, no ambiguity
- 0.75 – 0.89 → clear but slightly generic
- 0.60 – 0.74 → weak signal, but still classifiable
- < 0.60 → use OTHER unless strong keyword match exists

NEVER output confidence > 0.80 for vague posts.

========================
STEP 4 — CATEGORY DEFINITIONS (STRICT)
========================

ACCOMMODATION:
stay, hotel, hostel, apartment, Airbnb, rent, booking, room, lodging

FOOD:
restaurant, cafe, dish, recipe, eat, cuisine, street food, menu

TRAVEL:
bus, train, flight, taxi, airport, directions, route, travel between cities

EVENT:
concert, festival, meetup, party, workshop, conference, exhibition, match

QUESTION:
ONLY general information requests not covered above

MARKETPLACE:
buy, sell, price, job, hiring, service, offer, freelance, "for sale"

OTHER:
greetings, emotions, storytelling, vague posts, spam-like text

========================
STEP 5 — FEW-SHOT BEHAVIOR EXAMPLES
========================

"Need a cheap hotel in Sousse"
→ ACCOMMODATION

"Who sells iPhone 14?"
→ MARKETPLACE

"Best couscous in Tunis?"
→ FOOD

"How do I go from Tunis to Sfax?"
→ TRAVEL

"Is there a concert this weekend?"
→ EVENT

"Hello everyone, I just arrived!"
→ OTHER

"What time does train leave?"
→ TRAVEL (NOT QUESTION)

========================
OUTPUT FORMAT (STRICT JSON ONLY)

{
  "topic": "ACCOMMODATION|FOOD|TRAVEL|EVENT|QUESTION|MARKETPLACE|OTHER",
  "confidence": 0.00,
  "reason": "max 50 chars, keyword only"
}

RULES:
- NO markdown
- NO explanation
- NO extra keys
- NO text before/after JSON
- reason must be extremely short

POST:
""" + content;
        Map<String, Object> req = Map.of(
                "model", model,
                "stream", false,
                "format", "json",
                "prompt", prompt,
                "options", Map.of("temperature", 0.0)
        );

        try {
            RestClient restClient = restClientBuilder.baseUrl(baseUrl).build();
            Map<String, Object> res = restClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(req)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            String raw = res == null ? "" : String.valueOf(res.get("response"));
            return parse(raw);
        } catch (Exception e) {
            return new Result(PostTopic.OTHER, 0.0, "inference_error");
        }
    }

    private Result parse(String raw) {
        try {
            Map<String, String> map = parseSimpleJsonObject(raw);

            PostTopic topic;
            try {
                topic = PostTopic.valueOf(map.getOrDefault("topic", "OTHER").trim().toUpperCase());
            } catch (Exception ignored) {
                topic = PostTopic.OTHER;
            }

            double confidence;
            try {
                confidence = Double.parseDouble(map.getOrDefault("confidence", "0.5"));
            } catch (Exception ignored) {
                confidence = 0.5;
            }
            confidence = Math.max(0.0, Math.min(1.0, confidence));

            String reason = map.getOrDefault("reason", "");
            return new Result(topic, confidence, reason);
        } catch (Exception e) {
            return new Result(PostTopic.OTHER, 0.0, "parse_error");
        }
    }

    // Minimal parser for {"topic":"...","confidence":...,"reason":"..."}
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