package tn.esprit.spring.baladna.marketplace.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class GptAiService {

    @Value("${huggingface.api.key:}")
    private String hfApiKey;

    private static final String PYTHON_LLM_URL = "http://localhost:8000/chat";

    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ==================================================================
    //  ENTRY POINT
    // ==================================================================
    public String chat(String userMessage, String artisanContext) {
        log.info("Message received: '{}'", userMessage);

        // 1. Try Python LLM (Mistral-7B)
        String pythonResult = callPythonLLM(userMessage);
        if (pythonResult != null && !pythonResult.isBlank()) {
            log.info("✅ [Python LLM]: {}", pythonResult);
            return pythonResult;
        }

        // 2. Try HuggingFace
        String hfResult = callHuggingFace(userMessage);
        if (hfResult != null && !hfResult.isBlank()) {
            log.info("✅ [HuggingFace]: {}", hfResult);
            return hfResult;
        }

        // 3. Fallback NLP Local
        String localResult = advancedLocalNlp(userMessage);
        log.info("📝 [NLP Local]: {}", localResult);
        return localResult;
    }

    // ==================================================================
    //  PYTHON LLM (Mistral-7B via FastAPI)
    // ==================================================================
    private String callPythonLLM(String userMessage) {
        try {
            String escapedMessage = userMessage.replace("\"", "\\\"");
            String jsonBody = String.format(
                    "{\"message\":\"%s\",\"artisanId\":27,\"artisanName\":\"Artisan\"}",
                    escapedMessage);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(PYTHON_LLM_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                // Extraire le JSON action ou le message texte
                return extractActionFromResponse(body);
            } else {
                log.warn("Python LLM returned status {}", response.statusCode());
                return null;
            }
        } catch (Exception e) {
            log.warn("Python LLM unavailable: {}", e.getMessage());
            return null;
        }
    }

    // ==================================================================
    //  HUGGINGFACE (fallback)
    // ==================================================================
    private String callHuggingFace(String userMessage) {
        if (hfApiKey == null || hfApiKey.isBlank() || !hfApiKey.startsWith("hf_")) {
            return null;
        }
        // Essayer Mixtral
        String result = callHuggingFaceModel(
                "https://api-inference.huggingface.co/models/mistralai/Mixtral-8x7B-Instruct-v0.1",
                userMessage, 300);
        if (result != null) return result;

        // Essayer Mistral
        result = callHuggingFaceModel(
                "https://api-inference.huggingface.co/models/mistralai/Mistral-7B-Instruct-v0.2",
                userMessage, 250);
        return result;
    }

    private String callHuggingFaceModel(String modelUrl, String userMessage, int maxTokens) {
        try {
            String prompt = buildPrompt(userMessage);

            String jsonBody = String.format(
                    "{\"inputs\":\"%s\",\"parameters\":{\"max_new_tokens\":%d,\"temperature\":0.1,\"do_sample\":false}}",
                    prompt.replace("\"", "\\\"").replace("\n", "\\n"),
                    maxTokens);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(modelUrl))
                    .header("Authorization", "Bearer " + hfApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(java.time.Duration.ofSeconds(45))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                // Extraire "generated_text"
                Pattern p = Pattern.compile("\"generated_text\"\\s*:\\s*\"(.*?)\"\\s*[,\\}]", Pattern.DOTALL);
                Matcher m = p.matcher(body);
                if (m.find()) {
                    String text = m.group(1);
                    text = text.replace("\\n", "\n").replace("\\\"", "\"");
                    return extractActionFromResponse(text);
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("HuggingFace model {} error: {}", modelUrl, e.getMessage());
            return null;
        }
    }

    private String buildPrompt(String userMessage) {
        return """
            <s>[INST] You are Baladna AI assistant for a Tunisian artisan marketplace.
            Help artisans manage products. Return ONLY JSON actions or short friendly replies.
            
            JSON FORMATS:
            Add product: {"action":"ADD_PRODUCT","productName":"...","price":X,"stock":Y,"category":"..."}
            Delete product: {"action":"DELETE_PRODUCT","productId":X}
            Update price: {"action":"UPDATE_PRICE","productId":X,"price":Y}
            Get products: {"action":"GET_PRODUCTS"}
            Get orders: {"action":"GET_ORDERS"}
            Get stats: {"action":"GET_STATS"}
            
            User message: """ + userMessage + "\n[/INST]";
    }

    // ==================================================================
    //  EXTRACT ACTION FROM RESPONSE
    // ==================================================================
    private String extractActionFromResponse(String response) {
        if (response == null || response.isBlank()) return null;

        // Chercher un JSON action
        Pattern jsonPattern = Pattern.compile("\\{\"action\"\\s*:\\s*\"(.*?)\".*?\\}", Pattern.DOTALL);
        Matcher m = jsonPattern.matcher(response);
        if (m.find()) {
            String json = m.group();
            // Nettoyer : virgule → point dans les nombres
            json = json.replaceAll("(\\d),(\\d)", "$1.$2");
            return json;
        }

        // Sinon retourner le texte tel quel
        String cleaned = response
                .replaceAll("(?i)<s>|</s>|\\[INST\\]|\\[/INST\\]", "")
                .trim();

        if (cleaned.length() > 300) {
            cleaned = cleaned.substring(0, 300) + "...";
        }

        return cleaned;
    }

    // ==================================================================
    //  NLP LOCAL (fallback ultime)
    // ==================================================================
    private String advancedLocalNlp(String userMessage) {
        String norm = normalize(userMessage);

        // ADD PRODUCT
        if (matchesAny(norm, "ajoute", "ajouter", "creer", "add", "create", "nouveau")) {
            return extractAddProduct(userMessage, norm);
        }

        // DELETE PRODUCT
        if (matchesAny(norm, "supprime", "supprimer", "delete", "remove", "effacer")) {
            return extractDeleteProduct(userMessage);
        }

        // UPDATE PRICE
        if (matchesAny(norm, "modifie", "modifier", "change", "update", "changer prix")) {
            return extractUpdatePrice(userMessage);
        }

        // GET ORDERS
        if (matchesAny(norm, "commande", "commandes", "orders", "my orders")) {
            return "{\"action\":\"GET_ORDERS\"}";
        }

        // GET PRODUCTS
        if (matchesAny(norm, "produit", "products", "catalogue", "list", "show")) {
            return "{\"action\":\"GET_PRODUCTS\"}";
        }

        // GET STATS
        if (matchesAny(norm, "stat", "stats", "statistics", "bilan", "report")) {
            return "{\"action\":\"GET_STATS\"}";
        }

        // GREETINGS
        if (matchesAny(norm, "bonjour", "hello", "hi", "salut", "salam", "hey")) {
            return "Hello! I'm your Baladna assistant. How can I help you today? " +
                    "You can say things like 'add a Berber carpet at 350 TND stock 5' or 'show my products'.";
        }

        // HELP
        if (matchesAny(norm, "help", "aide", "what can you do")) {
            return "I can help you manage your artisan shop:\n" +
                    "• Add product: 'add [name] at [price] TND stock [qty]'\n" +
                    "• Delete: 'delete product 3'\n" +
                    "• Update price: 'change price of product 2 to 60 TND'\n" +
                    "• View products: 'my products'\n" +
                    "• Stats: 'show stats'\n" +
                    "• Orders: 'my orders'";
        }

        return "I didn't understand. Try: 'add [product] at [price] TND' or 'help'.";
    }

    // ==================================================================
    //  LOCAL NLP EXTRACTORS
    // ==================================================================
    private String extractAddProduct(String original, String norm) {
        String productName = "";
        double price = 0.0;
        int stock = 1;

        // Pattern: "add [name] at [price] TND stock [qty]"
        Pattern p1 = Pattern.compile(
                "(?:ajoute[r]?|add|create|ajout)\\s+(?:a\\s+|an\\s+|un[e]?\\s+)?(.+?)\\s+(?:at|[àa])\\s+(\\d+(?:[.,]\\d+)?)\\s*(?:tnd|dt|dinars?)?",
                Pattern.CASE_INSENSITIVE);
        Matcher m1 = p1.matcher(original);
        if (m1.find()) {
            productName = cleanName(m1.group(1));
            price = toDouble(m1.group(2));
        } else {
            // Just price
            Pattern pPrice = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:tnd|dt|dinars?)", Pattern.CASE_INSENSITIVE);
            Matcher mp = pPrice.matcher(original);
            if (mp.find()) price = toDouble(mp.group(1));
            String nameRaw = original.replaceAll("(?i)(ajoute[r]?|add|create|at|[àa]|tnd|dt|dinars?|stock|qty|pieces?)", " ");
            productName = cleanName(nameRaw);
        }

        // Extract stock
        Pattern pStock = Pattern.compile("(?:stock|qty)\\s*(\\d+)|(\\d+)\\s*(?:pieces?|pcs|units?)", Pattern.CASE_INSENSITIVE);
        Matcher ms = pStock.matcher(original);
        if (ms.find()) {
            stock = Integer.parseInt(ms.group(1) != null ? ms.group(1) : ms.group(2));
        }

        if (productName.isBlank()) productName = "new product";

        return String.format(
                "{\"action\":\"ADD_PRODUCT\",\"productName\":\"%s\",\"price\":%.2f,\"stock\":%d,\"category\":\"%s\"}",
                escapeJson(productName), price, stock, detectCategory(norm));
    }

    private String extractDeleteProduct(String original) {
        Pattern p = Pattern.compile("(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(original);
        int id = m.find() ? Integer.parseInt(m.group(1)) : 0;
        if (id == 0) return "{\"action\":\"DELETE_PRODUCT\",\"productId\":0,\"message\":\"Which product number?\"}";
        return "{\"action\":\"DELETE_PRODUCT\",\"productId\":" + id + "}";
    }

    private String extractUpdatePrice(String original) {
        Pattern p = Pattern.compile(
                "(?:product)?\\s*#?\\s*(\\d+).*?(\\d+(?:[.,]\\d+)?)\\s*(?:tnd|dt|dinars?)?",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(original);
        if (m.find()) {
            int id = Integer.parseInt(m.group(1));
            double price = toDouble(m.group(2));
            return String.format("{\"action\":\"UPDATE_PRICE\",\"productId\":%d,\"price\":%.2f}", id, price);
        }
        return "{\"action\":\"UPDATE_PRICE\",\"productId\":0,\"message\":\"Specify: 'change price of product X to Y TND'\"}";
    }

    // ==================================================================
    //  UTILITIES
    // ==================================================================
    private String normalize(String s) {
        return s.toLowerCase()
                .replaceAll("[éèêë]", "e").replaceAll("[àâ]", "a")
                .replaceAll("[ùû]", "u").replaceAll("[îï]", "i")
                .replaceAll("[ôö]", "o").replaceAll("[ç]", "c")
                .trim();
    }

    private String cleanName(String raw) {
        return raw.replaceAll("(?i)\\b(the|a|an|un|une|le|la|les|with|stock|qty|at|[àa])\\b", "")
                .replaceAll("\\s+", " ").trim();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private double toDouble(String s) {
        try { return Double.parseDouble(s.trim().replace(",", ".")); }
        catch (Exception e) { return 0.0; }
    }

    private boolean matchesAny(String text, String... keywords) {
        for (String kw : keywords) if (text.contains(kw)) return true;
        return false;
    }

    private String detectCategory(String norm) {
        if (matchesAny(norm, "poterie", "ceramic", "ceramique", "vase", "bol")) return "Ceramics";
        if (matchesAny(norm, "tapis", "carpet", "kilim", "tissage", "woven", "broderie", "fouta")) return "Art";
        if (matchesAny(norm, "bijou", "jewelry", "collier", "bracelet", "bague", "gold", "silver")) return "Jewelry";
        if (matchesAny(norm, "plante", "herb", "miel", "honey", "huile", "savon", "cosmetique", "creme", "naturel")) return "Wellness";
        if (matchesAny(norm, "gateau", "patisserie", "pastry", "tarte", "makroud", "biscuit", "chocolat")) return "Pastry";
        if (matchesAny(norm, "repas", "meal", "couscous", "brik", "plat")) return "Ready Meals";
        if (matchesAny(norm, "vetement", "habit", "dress", "fashion", "jebba", "costume")) return "Fashion";
        if (matchesAny(norm, "cadeau", "gift", "souvenir")) return "Gifts";
        if (matchesAny(norm, "deco", "decor", "maison", "home", "meuble", "cadre", "lampe")) return "Home Decor";
        return "Gifts"; // catégorie par défaut
    }
}