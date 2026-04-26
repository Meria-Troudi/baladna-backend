package tn.esprit.spring.baladna.transport.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.spring.baladna.transport.service.TransportAiBootstrapSeeder;
import tn.esprit.spring.baladna.transport.service.TransportAiDelayModelService;
import tn.esprit.spring.baladna.user.entity.Role;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Local-only test endpoint for manually seeding AI data during development.
 * Disabled unless the Spring profile local-ai-test is active.

 */
@RestController
@Profile("local-ai-test")
@RequestMapping("/api/transports/ai/test")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransportAiTestController {

    private final UserRepository userRepository;
    private final TransportAiBootstrapSeeder bootstrapSeeder;
    private final TransportAiDelayModelService delayModelService;

    @GetMapping("/seed")
    public ResponseEntity<Map<String, Object>> testSeed() {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            // Find first host
            User host = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.HOST)
                    .findFirst()
                    .orElse(null);

            if (host == null) {
                result.put("error", "No HOST user found in database");
                return ResponseEntity.ok(result);
            }

            result.put("step1_host", host.getEmail());
            result.put("step1_status", "OK");

            // Seed data
            int count = bootstrapSeeder.seedBootstrapData(host.getEmail());
            result.put("step2_seeded", count);
            result.put("step2_status", "OK");

            // Train model
            var modelResult = delayModelService.trainModelForHost(host.getEmail());
            result.put("step3_samples", modelResult.getSampleCount());
            result.put("step3_mae", modelResult.getMeanAbsoluteError());
            result.put("step3_status", "OK");

            result.put("success", true);
            result.put("message", "AI ready! " + count + " records seeded, model trained.");

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());

            // Get root cause
            Throwable cause = e;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            if (cause != e) {
                result.put("rootCause", cause.getClass().getSimpleName() + ": " + cause.getMessage());
            }
        }

        return ResponseEntity.ok(result);
    }
}