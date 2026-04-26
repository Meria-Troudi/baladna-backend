package tn.esprit.spring.baladna.transport.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import tn.esprit.spring.baladna.transport.dto.BestTransportRecommendationDTO;
import tn.esprit.spring.baladna.transport.dto.HostTransportAiReportDTO;
import tn.esprit.spring.baladna.transport.dto.TransportAiAlertDTO;
import tn.esprit.spring.baladna.transport.dto.TransportDelayPredictionDTO;
import tn.esprit.spring.baladna.transport.entity.Transport;
import tn.esprit.spring.baladna.transport.service.TransportAiService;
import tn.esprit.spring.baladna.transport.service.TransportService;

import java.util.List;

@RestController
@RequestMapping("/api/transports/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class TransportAiController {

    private final TransportAiService transportAiService;
    private final TransportService transportService;

    @GetMapping("/recommendation")
    public ResponseEntity<BestTransportRecommendationDTO> getBestRecommendation(
            @RequestParam(required = false) String departure,
            @RequestParam(required = false) String arrival,
            Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(
                transportAiService.recommendBestTransport(
                        departure,
                        arrival,
                        authentication.getName()
                )
        );
    }

    @GetMapping("/delay-prediction/{transportId}")
    public ResponseEntity<TransportDelayPredictionDTO> getDelayPrediction(
            @PathVariable Long transportId,
            Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        Transport transport = isHost(authentication)
                ? transportService.getTransportByIdForHost(transportId, authentication.getName())
                : transportService.getTransportById(transportId);

        if (transport == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(transportAiService.predictDelay(transport));
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<TransportAiAlertDTO>> getHostAlerts(Authentication authentication) {
        if (!isHost(authentication)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(
                transportAiService.getHostAlerts(authentication.getName())
        );
    }

    @GetMapping("/report")
    public ResponseEntity<HostTransportAiReportDTO> getHostReport(Authentication authentication) {
        if (!isHost(authentication)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(
                transportAiService.getHostReport(authentication.getName())
        );
    }

    private boolean isHost(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_HOST".equals(authority.getAuthority()));
    }
}