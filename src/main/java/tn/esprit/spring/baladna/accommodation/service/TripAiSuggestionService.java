package tn.esprit.spring.baladna.accommodation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tn.esprit.spring.baladna.accommodation.dto.AccommodationResponseDto;
import tn.esprit.spring.baladna.accommodation.dto.TripSuggestionResponseDto;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TripAiSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(TripAiSuggestionService.class);
    private static final int MAX_LISTINGS_FOR_AI = 40;
    private static final int MAX_RESULTS = 10;

    private final AccommodationService accommodationService;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.gemini-api-key:}")
    private String geminiApiKey;

    @Value("${app.ai.gemini-model:gemini-2.0-flash}")
    private String geminiModel;

    private final RestClient geminiClient = RestClient.builder()
            .baseUrl("https://generativelanguage.googleapis.com")
            .build();

    public TripSuggestionResponseDto suggest(String rawDescription) {
        String description = rawDescription == null ? "" : rawDescription.trim();
        if (description.isEmpty()) {
            return TripSuggestionResponseDto.builder()
                    .accommodations(List.of())
                    .note("Please describe your trip in a few words.")
                    .mode("keyword")
                    .build();
        }

        List<AccommodationResponseDto> catalog = accommodationService.listAllPublic();
        if (catalog.isEmpty()) {
            return TripSuggestionResponseDto.builder()
                    .accommodations(List.of())
                    .note("No listings are available yet.")
                    .mode("keyword")
                    .build();
        }

        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            try {
                return suggestWithGemini(description, catalog);
            } catch (Exception e) {
                log.warn("Gemini suggestion failed, using keyword fallback: {}", e.getMessage());
            }
        }

        return keywordFallback(description, catalog, geminiApiKey == null || geminiApiKey.isBlank()
                ? "Matched keywords from your text (add a free Gemini API key in application.properties for smarter picks)."
                : "Matched keywords from your text (AI call failed; check logs).");
    }

    private TripSuggestionResponseDto suggestWithGemini(String description, List<AccommodationResponseDto> catalog)
            throws Exception {
        String safeDesc = description.replace('\n', ' ').replace('\r', ' ').trim();

        List<AccommodationResponseDto> slice = catalog.stream()
                .limit(MAX_LISTINGS_FOR_AI)
                .collect(Collectors.toList());

        String catalogJson = buildCompactCatalogJson(slice);
        String prompt = """
                You are a travel assistant for Tunisia. The user describes their trip.
                User text: "%s"

                Here is a JSON array of existing accommodation listings (you MUST only recommend ids from this list):
                %s

                Return a single JSON object with exactly these keys:
                - "orderedIds": array of listing id strings, best match first, at most %d entries, only ids from the catalog.
                - "note": one short friendly sentence in English explaining why these listings fit.

                If nothing fits well, still pick the closest listings and say so in "note".
                """.formatted(escapeJsonString(safeDesc), catalogJson, MAX_RESULTS);

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0.25);
        generationConfig.put("maxOutputTokens", 1024);
        generationConfig.put("responseMimeType", "application/json");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
        body.put("generationConfig", generationConfig);

        String path = "/v1beta/models/" + geminiModel + ":generateContent";
        JsonNode root = geminiClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(path)
                        .queryParam("key", geminiApiKey.trim())
                        .build())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (root == null) {
            throw new IllegalStateException("Empty Gemini response");
        }

        String text = extractGeminiText(root);
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("No text in Gemini response");
        }

        text = stripMarkdownJsonFence(text);
        JsonNode parsed = objectMapper.readTree(text);
        List<String> orderedIds = new ArrayList<>();
        if (parsed.has("orderedIds") && parsed.get("orderedIds").isArray()) {
            for (JsonNode idNode : parsed.get("orderedIds")) {
                if (idNode.isTextual()) {
                    orderedIds.add(idNode.asText());
                }
            }
        }
        String note = parsed.has("note") && parsed.get("note").isTextual()
                ? parsed.get("note").asText()
                : "Here are listings that best match your trip.";

        Map<String, AccommodationResponseDto> byId = catalog.stream()
                .collect(Collectors.toMap(AccommodationResponseDto::getId, a -> a, (a, b) -> a));

        List<AccommodationResponseDto> ordered = new ArrayList<>();
        for (String id : orderedIds) {
            AccommodationResponseDto dto = byId.get(id);
            if (dto != null && ordered.stream().noneMatch(x -> x.getId().equals(id))) {
                ordered.add(dto);
            }
            if (ordered.size() >= MAX_RESULTS) {
                break;
            }
        }

        if (ordered.isEmpty()) {
            return keywordFallback(description, catalog, "AI returned no known ids; showing keyword matches instead.");
        }

        return TripSuggestionResponseDto.builder()
                .accommodations(ordered)
                .note(note)
                .mode("gemini")
                .build();
    }

    private static String stripMarkdownJsonFence(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) {
                t = t.substring(firstNl + 1);
            }
            int end = t.lastIndexOf("```");
            if (end > 0) {
                t = t.substring(0, end);
            }
        }
        return t.trim();
    }

    private static String extractGeminiText(JsonNode root) {
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            return null;
        }
        JsonNode parts = candidates.get(0).path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            return null;
        }
        return parts.get(0).path("text").asText(null);
    }

    private String buildCompactCatalogJson(List<AccommodationResponseDto> slice) throws Exception {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (AccommodationResponseDto a : slice) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", a.getId());
            row.put("title", a.getTitle());
            row.put("address", a.getAddress());
            row.put("type", a.getType() != null ? a.getType().name() : "");
            row.put("maxGuests", a.getMaxGuests());
            String desc = a.getDescription() == null ? "" : a.getDescription();
            if (desc.length() > 280) {
                desc = desc.substring(0, 277) + "...";
            }
            row.put("description", desc);
            String am = a.getAmenities() == null ? "" : a.getAmenities();
            if (am.length() > 160) {
                am = am.substring(0, 157) + "...";
            }
            row.put("amenities", am);
            rows.add(row);
        }
        return objectMapper.writeValueAsString(rows);
    }

    private static String escapeJsonString(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private TripSuggestionResponseDto keywordFallback(String description, List<AccommodationResponseDto> catalog,
                                                     String note) {
        List<AccommodationResponseDto> ranked = rankByKeywords(description, catalog);
        return TripSuggestionResponseDto.builder()
                .accommodations(ranked.stream().limit(MAX_RESULTS).collect(Collectors.toList()))
                .note(note)
                .mode("keyword")
                .build();
    }

    private List<AccommodationResponseDto> rankByKeywords(String userText, List<AccommodationResponseDto> catalog) {
        Locale locale = Locale.ROOT;
        String norm = userText.toLowerCase(locale);
        String[] tokens = norm.split("[^a-z0-9àâäéèêëïîôùûç]+");
        List<String> meaningful = Arrays.stream(tokens)
                .map(String::trim)
                .filter(t -> t.length() > 2)
                .distinct()
                .collect(Collectors.toList());

        record Scored(AccommodationResponseDto dto, int score) {}

        List<Scored> scored = new ArrayList<>();
        for (AccommodationResponseDto a : catalog) {
            String hay = (a.getTitle() + " " + a.getAddress() + " "
                    + nullToEmpty(a.getDescription()) + " "
                    + nullToEmpty(a.getAmenities()) + " "
                    + nullToEmpty(a.getRules()) + " "
                    + (a.getType() != null ? a.getType().name() : "")).toLowerCase(locale);
            int score = 1;
            for (String t : meaningful) {
                if (hay.contains(t)) {
                    score += 3;
                }
            }
            scored.add(new Scored(a, score));
        }
        scored.sort(Comparator.comparingInt(Scored::score).reversed());
        return scored.stream().map(Scored::dto).collect(Collectors.toList());
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
