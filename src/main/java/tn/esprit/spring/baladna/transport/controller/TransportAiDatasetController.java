package tn.esprit.spring.baladna.transport.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.baladna.transport.dto.HostTransportAiReportDTO;
import tn.esprit.spring.baladna.transport.dto.TransportAiDatasetSummaryDTO;
import tn.esprit.spring.baladna.transport.service.TransportAiBootstrapSeeder;
import tn.esprit.spring.baladna.transport.service.TransportAiDatasetService;
import tn.esprit.spring.baladna.transport.service.TransportAiService;

import java.util.Map;

@RestController
@RequestMapping("/api/transports/ai/dataset")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TransportAiDatasetController {

    private final TransportAiDatasetService transportAiDatasetService;
    private final TransportAiService transportAiService;
    private final TransportAiBootstrapSeeder bootstrapSeeder;

    @GetMapping("/summary")
    public ResponseEntity<TransportAiDatasetSummaryDTO> getSummary(Authentication authentication) {
        if (!isHost(authentication)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(
                transportAiDatasetService.getDatasetSummaryForHost(authentication.getName())
        );
    }

    @PostMapping("/sync")
    public ResponseEntity<TransportAiDatasetSummaryDTO> syncCurrentHostDatasets(Authentication authentication) {
        if (!isHost(authentication)) {
            return ResponseEntity.status(403).build();
        }

        HostTransportAiReportDTO report = transportAiService.getHostReport(authentication.getName());
        return ResponseEntity.ok(
                transportAiDatasetService.syncCurrentHostDatasets(authentication.getName(), report)
        );
    }

    /**
     * Generates 1000 synthetic trip records for AI model training.
     * This allows the host to train the delay prediction model
     * immediately without waiting for real transport history.
     *
     * POST /api/transports/ai/dataset/seed
     */
    @PostMapping("/seed")
    public ResponseEntity<Map<String, Object>> seedBootstrapData(Authentication authentication) {
        if (!isHost(authentication)) {
            return ResponseEntity.status(403).build();
        }

        try {
            int inserted = bootstrapSeeder.seedBootstrapData(authentication.getName());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "recordsGenerated", inserted,
                    "message", inserted + " synthetic trip records generated successfully. You can now train the AI model."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        }
    }

    private boolean isHost(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_HOST".equals(authority.getAuthority()));
    }
}