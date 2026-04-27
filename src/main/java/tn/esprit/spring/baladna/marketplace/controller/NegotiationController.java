package tn.esprit.spring.baladna.marketplace.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.marketplace.ai.service.NegotiationService;

@RestController
@RequestMapping("/api/negotiate")
@CrossOrigin(origins = "*")
public class NegotiationController {

    @Autowired
    private NegotiationService negotiationService;

    @PostMapping
    public ResponseEntity<NegotiationService.NegotiationResponse> negotiate(
            @RequestBody NegotiationService.NegotiationRequest request) {
        return ResponseEntity.ok(negotiationService.negotiate(request));
    }
}