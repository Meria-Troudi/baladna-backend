package tn.esprit.spring.baladna.transport.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tn.esprit.spring.baladna.transport.repository.TransportAiTripDatasetRepository;
import tn.esprit.spring.baladna.user.entity.Role;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.util.List;

/**
 * Runs once at startup:
 *   1. Keeps exactly 1000 synthetic bootstrap records for EVERY host
 *   2. Trains the AI delay prediction model for EVERY host when needed
 *
 * This ensures the AI works for ANY host and ANY tourist viewing ANY transport.
 * On subsequent restarts, bootstrap data is kept at exactly 1000 rows per host.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(10)
public class TransportAiStartupRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TransportAiBootstrapSeeder bootstrapSeeder;
    private final TransportAiDelayModelService delayModelService;
    private static final int EXPECTED_BOOTSTRAP_ROWS = 1000;

    private final TransportAiTripDatasetRepository tripDatasetRepository;

    @Override
    public void run(String... args) {
        log.info("==============================================");
        log.info("[AI] Starting AI bootstrap check...");
        log.info("==============================================");

        try {
            List<User> hosts = userRepository.findAll().stream()
                    .filter(user -> user.getRole() == Role.HOST)
                    .toList();

            log.info("[AI] Found {} HOST users", hosts.size());

            if (hosts.isEmpty()) {
                log.info("[AI] No HOST users found. Skipping AI bootstrap.");
                return;
            }

            // Seed and train for EVERY host
            for (User host : hosts) {
                String hostEmail = host.getEmail();
                log.info("[AI] Processing host: {} ({})", host.getFirstName(), hostEmail);

                try {
                    seedAndTrain(hostEmail);
                } catch (Exception e) {
                    log.warn("[AI] Failed for host {}: {}", hostEmail, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("[AI] STARTUP ERROR: {}", e.getMessage(), e);
        }

        log.info("==============================================");
        log.info("[AI] AI startup check completed for all hosts.");
        log.info("==============================================");
    }

    private void seedAndTrain(String hostEmail) {

        // Step 1: keep bootstrap data exactly aligned with the expected dataset size
        long bootstrapCount = tripDatasetRepository.countByHostEmailAndDataOrigin(
                hostEmail, TransportAiDatasetService.DATA_ORIGIN_BOOTSTRAP_IMPORT
        );
        log.info("[AI]   Existing bootstrap records for {}: {}", hostEmail, bootstrapCount);

        boolean bootstrapRegenerated = false;
        if (bootstrapCount != EXPECTED_BOOTSTRAP_ROWS) {
            try {
                log.info("[AI]   Rebuilding bootstrap dataset to {} synthetic records for {}...", EXPECTED_BOOTSTRAP_ROWS, hostEmail);
                int seeded = bootstrapSeeder.seedBootstrapData(hostEmail);
                log.info("[AI]   SUCCESS: {} records created for {}", seeded, hostEmail);
                bootstrapRegenerated = true;
            } catch (Exception e) {
                log.error("[AI]   SEED FAILED for {}: {}", hostEmail, e.getMessage());
                return;
            }
        } else {
            log.info("[AI]   Bootstrap data already aligned ({} rows). Skipping seed.", bootstrapCount);
        }

        // Step 2: train model if not already trained
        boolean needsTraining = true;
        try {
            var modelSummary = delayModelService.getModelSummaryForHost(hostEmail);
            if (!bootstrapRegenerated && modelSummary.isTrained() && modelSummary.getSampleCount() != null
                    && modelSummary.getSampleCount() >= EXPECTED_BOOTSTRAP_ROWS) {
                log.info("[AI]   Model already trained ({} samples, MAE={}). Skipping.",
                        modelSummary.getSampleCount(), modelSummary.getMeanAbsoluteError());
                needsTraining = false;
            }
        } catch (Exception e) {
            log.info("[AI]   No trained model found. Will train now.");
        }

        if (needsTraining) {
            try {
                log.info("[AI]   Training AI delay model for {}...", hostEmail);
                var result = delayModelService.trainModelForHost(hostEmail);
                log.info("[AI]   MODEL TRAINED: {} samples, MAE={}, RMSE={}",
                        result.getSampleCount(),
                        result.getMeanAbsoluteError(),
                        result.getRootMeanSquaredError());
            } catch (Exception e) {
                log.error("[AI]   TRAINING FAILED for {}: {}", hostEmail, e.getMessage());
            }
        }
    }
}