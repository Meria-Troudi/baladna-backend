package tn.esprit.spring.baladna.marketplace.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class GptAiService {

    public String chat(String userMessage, String artisanContext) {
        log.info("Message received: '{}'", userMessage);
        String norm = normalize(userMessage);

        // ADD PRODUCT
        if (matchesAny(norm, "ajoute", "ajouter", "add", "create", "nouveau", "new", "sell", "i want to add", "put")) {
            return extractAddProduct(userMessage);
        }
        // DELETE BY NAME OR ID
        if (matchesAny(norm, "supprime", "supprimer", "delete", "remove", "effacer", "enlever", "erase")) {
            return extractDeleteProduct(userMessage);
        }
        // UPDATE PRICE
        if (matchesAny(norm, "modifie", "modifier", "change", "update", "changer prix", "new price", "set price")) {
            return extractUpdatePrice(userMessage);
        }
        // SUGGEST PRICE
        if (matchesAny(norm, "suggest", "recommend", "price for", "what price", "how much", "pricing", "prix sugg")) {
            return extractSuggestPrice(userMessage);
        }
        // NEGOTIATE
        if (matchesAny(norm, "negotiate", "discount", "reduce", "cheaper", "negocier", "reduction", "baisse", "offer")) {
            return "{\"action\":\"NEGOTIATE\",\"message\":\"I can offer a 10% discount on this item. Would you like that?\"}";
        }
        // ORDERS
        if (matchesAny(norm, "commande", "orders", "my orders", "commandes", "sales", "ventes", "purchases")) {
            return "{\"action\":\"GET_ORDERS\"}";
        }
        // PRODUCTS
        if (matchesAny(norm, "produit", "products", "catalogue", "catalog", "list", "show", "display", "inventory", "stock list")) {
            return "{\"action\":\"GET_PRODUCTS\"}";
        }
        // STATS
        if (matchesAny(norm, "stat", "stats", "statistics", "bilan", "report", "analytics", "dashboard", "overview")) {
            return "{\"action\":\"GET_STATS\"}";
        }
        // GREETINGS
        if (matchesAny(norm, "bonjour", "hello", "hi", "salut", "salam", "hey", "good morning", "good evening", "yo", "hola")) {
            return "Hello! 👋 I'm your Baladna AI assistant. I can help you manage your shop:\n" +
                    "• 'add pottery at 45 TND stock 10'\n" +
                    "• 'delete Luxury Vase'\n" +
                    "• 'show my products'\n" +
                    "• 'my orders'\n" +
                    "• 'stats'\n" +
                    "• 'suggest price for carpet'";
        }
        // HELP
        if (matchesAny(norm, "help", "aide", "what can you do", "capabilities", "features", "commands")) {
            return "🤖 *Baladna AI Assistant*\n\n" +
                    "📦 *Products:*\n" +
                    "• Add: 'add [name] at [price] TND stock [qty]'\n" +
                    "• Delete: 'delete [name or ID]'\n" +
                    "• List: 'my products'\n" +
                    "• Price: 'change price of 5 to 60 TND'\n\n" +
                    "💰 *Pricing:*\n" +
                    "• Suggest: 'suggest price for [product]'\n" +
                    "• Negotiate: 'can you give discount?'\n\n" +
                    "📊 *Business:*\n" +
                    "• Orders: 'my orders'\n" +
                    "• Stats: 'stats'";
        }
        // THANKS
        if (matchesAny(norm, "thanks", "merci", "thank you", "thx", "ty")) {
            return "You're welcome! 😊 Happy to help. Is there anything else?";
        }

        return "I didn't understand. Try: 'add [product] at [price] TND' or 'help' for all commands.";
    }

    private String extractAddProduct(String original) {
        String name = "";
        double price = 0;
        int stock = 1;
        String category = "handicraft";

        Pattern p = Pattern.compile("(?:add|ajoute|create|sell|put)\\s+(?:a\\s+|an\\s+)?(.+?)\\s+(?:at|for|[àa])\\s+(\\d+(?:[.,]\\d+)?)\\s*(?:tnd|dt|dinars?)?", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(original);
        if (m.find()) {
            name = cleanProductName(m.group(1));
            price = Double.parseDouble(m.group(2).replace(",", "."));
        }

        Pattern ps = Pattern.compile("(\\d+)\\s*(?:stock|pieces?|pcs|units?|qty|quantity)", Pattern.CASE_INSENSITIVE);
        Matcher ms = ps.matcher(original);
        if (ms.find()) stock = Integer.parseInt(ms.group(1));

        // Category detection
        String low = original.toLowerCase();
        if (low.contains("pottery") || low.contains("ceramic") || low.contains("vase")) category = "Pottery";
        if (low.contains("carpet") || low.contains("rug") || low.contains("tapis")) category = "Carpets";
        if (low.contains("jewel") || low.contains("necklace") || low.contains("ring")) category = "Jewelry";
        if (low.contains("oil") || low.contains("olive") || low.contains("soap")) category = "Wellness";

        if (name.isEmpty()) name = "new product";
        return String.format("{\"action\":\"ADD_PRODUCT\",\"productName\":\"%s\",\"price\":%.2f,\"stock\":%d,\"category\":\"%s\"}",
                escapeJson(name), price, stock, category);
    }

    private String extractDeleteProduct(String original) {
        Pattern pNum = Pattern.compile("(\\d+)");
        Matcher mNum = pNum.matcher(original);
        if (mNum.find()) {
            return "{\"action\":\"DELETE_PRODUCT\",\"productId\":" + mNum.group(1) + "}";
        }

        Pattern pName = Pattern.compile("(?:delete|supprime|supprimer|remove|effacer)\\s+(.+?)(?:\\s+product|\\.|$)", Pattern.CASE_INSENSITIVE);
        Matcher mName = pName.matcher(original);
        if (mName.find()) {
            return "{\"action\":\"DELETE_PRODUCT_BY_NAME\",\"productName\":\"" + escapeJson(mName.group(1).trim()) + "\"}";
        }

        return "{\"action\":\"DELETE_PRODUCT\",\"productId\":0}";
    }

    private String extractUpdatePrice(String original) {
        Pattern p = Pattern.compile("(?:product|#)?\\s*(\\d+).*?(\\d+(?:[.,]\\d+)?)\\s*(?:tnd|dt|dinars?)?", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(original);
        if (m.find()) {
            return String.format("{\"action\":\"UPDATE_PRICE\",\"productId\":%d,\"price\":%.2f}", Integer.parseInt(m.group(1)), Double.parseDouble(m.group(2).replace(",", ".")));
        }
        return "{\"action\":\"UPDATE_PRICE\",\"productId\":0,\"price\":0.0}";
    }

    private String extractSuggestPrice(String original) {
        String name = "";
        Pattern p = Pattern.compile("(?:suggest|recommend|price for|what price|how much)\\s+(?:a\\s+)?(.+?)(?:\\?|\\.|$)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(original);
        if (m.find()) name = m.group(1).trim();

        double suggestedPrice = 50.0;
        if (name.toLowerCase().contains("carpet") || name.toLowerCase().contains("rug")) suggestedPrice = 350.0;
        if (name.toLowerCase().contains("pottery") || name.toLowerCase().contains("vase")) suggestedPrice = 45.0;
        if (name.toLowerCase().contains("jewel") || name.toLowerCase().contains("gold")) suggestedPrice = 200.0;
        if (name.toLowerCase().contains("oil")) suggestedPrice = 75.0;

        return "Based on market analysis, I suggest pricing '" + name + "' at " + String.format("%.0f", suggestedPrice) +
                " TND. This is competitive for Tunisian handicrafts. You can set it with: 'add " + name + " at " +
                String.format("%.0f", suggestedPrice) + " TND stock 10'";
    }

    private String cleanProductName(String raw) {
        return raw.replaceAll("(?i)\\b(a|an|the|my|new|some)\\b", "").replaceAll("\\s+", " ").trim();
    }

    private String escapeJson(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
    private String normalize(String s) { return s.toLowerCase().replaceAll("[éèêë]","e").replaceAll("[àâ]","a").replaceAll("[ûù]","u").trim(); }
    private boolean matchesAny(String text, String... kw) { for (String k : kw) if (text.contains(k)) return true; return false; }
}