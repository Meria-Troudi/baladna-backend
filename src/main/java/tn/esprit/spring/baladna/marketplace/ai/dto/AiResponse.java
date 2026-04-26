package tn.esprit.spring.baladna.marketplace.ai.dto;

import lombok.Data;

@Data
public class AiResponse {
    private boolean success;
    private String  transcribedText;
    private String  message;
    private String  rawAiReply;
    private String  action;

    private Object payload;   // données extraites (produit)
    private boolean requiresConfirmation;


}
