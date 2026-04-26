package tn.esprit.spring.baladna.marketplace.ai.dto;

import lombok.Data;

@Data
public class AiProductPayload {
    private String productName;
    private Double price;
    private Integer stock;
    private String category;
}
