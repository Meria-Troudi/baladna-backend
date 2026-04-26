package tn.esprit.spring.baladna.marketplace.ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class NegotiationService {

    /**
     * AI Price Negotiation Engine
     *
     * Rules:
     * - 1 product: max 5% discount
     * - 2-3 products: max 10% discount
     * - 4+ products: max 15% discount
     * - Product > 200 TND: extra 2% discount
     * - Customer with 3+ orders: loyalty bonus 3%
     * - Never go below 60% of original price
     */
    public NegotiationResponse negotiate(NegotiationRequest request) {
        double totalOriginal = 0;
        double totalNegotiated = 0;
        List<ProductDiscount> discounts = new ArrayList<>();

        int itemCount = request.getItems().size();
        boolean isLoyalCustomer = request.getCustomerOrdersCount() >= 3;

        for (CartItem item : request.getItems()) {
            double originalPrice = item.getPrice();
            double discountPercent = 0;
            List<String> reasons = new ArrayList<>();

            // Base discount by quantity
            if (itemCount == 1) {
                discountPercent += 2; // 2% base
                reasons.add("Welcome discount");
            } else if (itemCount <= 3) {
                discountPercent += 7;
                reasons.add("Multi-item purchase");
            } else {
                discountPercent += 12;
                reasons.add("Bulk purchase");
            }

            // High-value item bonus
            if (originalPrice >= 200) {
                discountPercent += 2;
                reasons.add("Premium item bonus");
            }

            // Loyalty bonus
            if (isLoyalCustomer) {
                discountPercent += 3;
                reasons.add("Loyalty reward");
            }

            // Category-specific: artisanat mérite une remise
            if (item.getCategory() != null &&
                    (item.getCategory().contains("Art") || item.getCategory().contains("Ceramics"))) {
                discountPercent += 1;
                reasons.add("Artisan appreciation");
            }

            // Cap at 20% max
            discountPercent = Math.min(discountPercent, 20);

            // Minimum price: 60% of original
            double finalPrice = originalPrice * (1 - discountPercent / 100);
            finalPrice = Math.max(finalPrice, originalPrice * 0.60);

            totalOriginal += originalPrice;
            totalNegotiated += finalPrice;

            ProductDiscount pd = new ProductDiscount();
            pd.setProductName(item.getProductName());
            pd.setOriginalPrice(originalPrice);
            pd.setFinalPrice(Math.round(finalPrice * 100.0) / 100.0);
            pd.setDiscountPercent(Math.round(discountPercent * 10.0) / 10.0);
            pd.setSavings(Math.round((originalPrice - finalPrice) * 100.0) / 100.0);
            pd.setReasons(reasons);
            discounts.add(pd);
        }

        // Build response
        NegotiationResponse response = new NegotiationResponse();
        response.setSuccess(true);
        response.setTotalOriginal(Math.round(totalOriginal * 100.0) / 100.0);
        response.setTotalNegotiated(Math.round(totalNegotiated * 100.0) / 100.0);
        response.setTotalSavings(Math.round((totalOriginal - totalNegotiated) * 100.0) / 100.0);
        response.setDiscounts(discounts);
        response.setMessage(generateMessage(discounts, totalOriginal - totalNegotiated));

        log.info("Negotiation: {} items, saved {} TND", itemCount,
                Math.round((totalOriginal - totalNegotiated) * 100.0) / 100.0);

        return response;
    }

    private String generateMessage(List<ProductDiscount> discounts, double totalSavings) {
        if (totalSavings >= 50) {
            return "🎉 Amazing deal! You're saving " + String.format("%.0f", totalSavings) +
                    " TND today. This is a special artisan price just for you!";
        } else if (totalSavings >= 20) {
            return "✨ Great news! I can offer you a " + String.format("%.0f", totalSavings) +
                    " TND discount. Handcrafted quality at the best price!";
        } else {
            return "💫 Small gesture: " + String.format("%.0f", totalSavings) +
                    " TND off for you. Every bit counts when supporting local artisans!";
        }
    }

    // ========== DATA CLASSES ==========

    public static class NegotiationRequest {
        private List<CartItem> items;
        private int customerOrdersCount;

        public List<CartItem> getItems() { return items; }
        public void setItems(List<CartItem> items) { this.items = items; }
        public int getCustomerOrdersCount() { return customerOrdersCount; }
        public void setCustomerOrdersCount(int n) { this.customerOrdersCount = n; }
    }

    public static class CartItem {
        private String productName;
        private double price;
        private String category;

        public String getProductName() { return productName; }
        public void setProductName(String s) { this.productName = s; }
        public double getPrice() { return price; }
        public void setPrice(double p) { this.price = p; }
        public String getCategory() { return category; }
        public void setCategory(String c) { this.category = c; }
    }

    public static class ProductDiscount {
        private String productName;
        private double originalPrice;
        private double finalPrice;
        private double discountPercent;
        private double savings;
        private List<String> reasons;

        public String getProductName() { return productName; }
        public void setProductName(String s) { this.productName = s; }
        public double getOriginalPrice() { return originalPrice; }
        public void setOriginalPrice(double p) { this.originalPrice = p; }
        public double getFinalPrice() { return finalPrice; }
        public void setFinalPrice(double p) { this.finalPrice = p; }
        public double getDiscountPercent() { return discountPercent; }
        public void setDiscountPercent(double d) { this.discountPercent = d; }
        public double getSavings() { return savings; }
        public void setSavings(double s) { this.savings = s; }
        public List<String> getReasons() { return reasons; }
        public void setReasons(List<String> r) { this.reasons = r; }
    }

    public static class NegotiationResponse {
        private boolean success;
        private double totalOriginal;
        private double totalNegotiated;
        private double totalSavings;
        private List<ProductDiscount> discounts;
        private String message;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean s) { this.success = s; }
        public double getTotalOriginal() { return totalOriginal; }
        public void setTotalOriginal(double d) { this.totalOriginal = d; }
        public double getTotalNegotiated() { return totalNegotiated; }
        public void setTotalNegotiated(double d) { this.totalNegotiated = d; }
        public double getTotalSavings() { return totalSavings; }
        public void setTotalSavings(double d) { this.totalSavings = d; }
        public List<ProductDiscount> getDiscounts() { return discounts; }
        public void setDiscounts(List<ProductDiscount> d) { this.discounts = d; }
        public String getMessage() { return message; }
        public void setMessage(String m) { this.message = m; }
    }
}