package tn.esprit.spring.baladna.transport.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.baladna.transport.dto.TransportAiDelayModelSummaryDTO;
import tn.esprit.spring.baladna.transport.service.TransportAiDelayModelService;

@RestController
@RequestMapping("/api/transports/ai/model")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TransportAiDelayModelController {

    private final TransportAiDelayModelService transportAiDelayModelService;

    @GetMapping("/summary")
    public ResponseEntity<TransportAiDelayModelSummaryDTO> getSummary(Authentication authentication) {
        if (!isHost(authentication)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(
                transportAiDelayModelService.getModelSummaryForHost(authentication.getName())
        );
    }

    @PostMapping("/train")
    public ResponseEntity<?> trainModel(Authentication authentication) {
        if (!isHost(authentication)) {
            return ResponseEntity.status(403).build();
        }

        try {
            return ResponseEntity.ok(
                    transportAiDelayModelService.trainModelForHost(authentication.getName())
            );
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().body(exception.getMessage());
        }
    }

    private boolean isHost(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_HOST".equals(authority.getAuthority()));
    }
}
