package tn.esprit.spring.baladna.transport.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.baladna.transport.entity.TransportAiTripDataset;
import tn.esprit.spring.baladna.transport.repository.TransportAiTripDatasetRepository;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates 1000 realistic synthetic transport trip records for Tunisia.
 * Used to pre-train the AI delay prediction model before real data accumulates.
 * All records are tagged BOOTSTRAP_IMPORT and have no linked real transport.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransportAiBootstrapSeeder {

    private static final String DATA_ORIGIN = "BOOTSTRAP_IMPORT";
    private static final int SEED_COUNT = 1000;

    private final TransportAiTripDatasetRepository tripDatasetRepository;
    private final UserRepository userRepository;

    private static final String[][] ROUTES = {
            {"Tunis", "Sousse", "140"},
            {"Tunis", "Sfax", "270"},
            {"Tunis", "Bizerte", "65"},
            {"Tunis", "Nabeul", "67"},
            {"Tunis", "Hammamet", "75"},
            {"Tunis", "Kairouan", "155"},
            {"Tunis", "Monastir", "162"},
            {"Tunis", "Gabes", "400"},
            {"Tunis", "Tozeur", "450"},
            {"Tunis", "Djerba", "510"},
            {"Sousse", "Sfax", "130"},
            {"Sousse", "Monastir", "22"},
            {"Sousse", "Kairouan", "60"},
            {"Sousse", "Hammamet", "85"},
            {"Sfax", "Gabes", "130"},
            {"Sfax", "Djerba", "280"},
            {"Sfax", "Kairouan", "180"},
            {"Bizerte", "Tunis", "65"},
            {"Nabeul", "Hammamet", "12"},
            {"Gabes", "Tozeur", "200"},
            {"Gabes", "Djerba", "155"},
            {"Tozeur", "Douz", "110"},
            {"Kairouan", "Monastir", "80"},
            {"Monastir", "Mahdia", "45"},
            {"Tunis", "Tabarka", "175"},
            {"Tunis", "Le Kef", "170"},
            {"Tunis", "Beja", "105"},
            {"Tunis", "Jendouba", "155"},
            {"Sousse", "Tunis", "140"},
            {"Sfax", "Tunis", "270"},
    };

    private static final String[] STATUSES = {
            "COMPLETED", "COMPLETED", "COMPLETED", "COMPLETED", "COMPLETED", "CANCELLED"
    };

    @Transactional
    public int seedBootstrapData(String hostEmail) {
        User host = userRepository.findByEmail(hostEmail)
                .orElseThrow(() -> new RuntimeException("Host not found: " + hostEmail));

        long existing = tripDatasetRepository.countByHostEmailAndDataOrigin(hostEmail, DATA_ORIGIN);
        if (existing > 0) {
            log.info("[AI] Removing {} existing bootstrap records for {}", existing, hostEmail);
            tripDatasetRepository.deleteByHostEmailAndDataOrigin(hostEmail, DATA_ORIGIN);
        }

        Random random = new Random(42);
        List<TransportAiTripDataset> batch = new ArrayList<>(500);
        int totalInserted = 0;

        for (int i = 0; i < SEED_COUNT; i++) {
            batch.add(generateRecord(host, hostEmail, random));

            if (batch.size() >= 500) {
                tripDatasetRepository.saveAll(batch);
                totalInserted += batch.size();
                batch.clear();
                log.info("[AI] Inserted {}/{} bootstrap records for {}", totalInserted, SEED_COUNT, hostEmail);
            }
        }

        if (!batch.isEmpty()) {
            tripDatasetRepository.saveAll(batch);
            totalInserted += batch.size();
        }

        log.info("[AI] Bootstrap seeding complete: {} records for {}", totalInserted, hostEmail);
        return totalInserted;
    }

    private TransportAiTripDataset generateRecord(User host, String hostEmail, Random random) {
        String[] route = ROUTES[random.nextInt(ROUTES.length)];
        String departureCity = route[0];
        String arrivalCity = route[1];
        double distanceKm = Double.parseDouble(route[2]) + (random.nextDouble() * 20 - 10);

        LocalDateTime departureDate = LocalDateTime.now()
                .minusDays(random.nextInt(365))
                .withHour(5 + random.nextInt(17))
                .withMinute(random.nextInt(60))
                .withSecond(0)
                .withNano(0);

        DayOfWeek dayOfWeek = departureDate.getDayOfWeek();
        boolean isWeekend = dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
        int hour = departureDate.getHour();
        boolean isRushHour = (hour >= 7 && hour <= 9) || (hour >= 16 && hour <= 19);
        int month = departureDate.getMonthValue();

        // Seasonal weather
        String weather;
        if (month >= 6 && month <= 9) {
            double r = random.nextDouble();
            weather = r < 0.85 ? "SUNNY" : r < 0.95 ? "SANDSTORM" : "RAIN";
        } else if (month == 12 || month <= 2) {
            double r = random.nextDouble();
            weather = r < 0.50 ? "SUNNY" : r < 0.85 ? "RAIN" : r < 0.95 ? "STORM" : "SANDSTORM";
        } else {
            double r = random.nextDouble();
            weather = r < 0.60 ? "SUNNY" : r < 0.82 ? "RAIN" : r < 0.91 ? "STORM" : "SANDSTORM";
        }

        double baseTemp = switch (month) {
            case 6, 7, 8 -> 30 + random.nextDouble() * 12;
            case 12, 1, 2 -> 8 + random.nextDouble() * 10;
            default -> 16 + random.nextDouble() * 14;
        };
        if ("RAIN".equals(weather) || "STORM".equals(weather)) baseTemp -= 3 + random.nextDouble() * 4;

        double windSpeed = switch (weather) {
            case "STORM" -> 50 + random.nextDouble() * 40;
            case "SANDSTORM" -> 40 + random.nextDouble() * 35;
            case "RAIN" -> 15 + random.nextDouble() * 30;
            default -> 2 + random.nextDouble() * 20;
        };

        double precipitation = switch (weather) {
            case "STORM" -> 10 + random.nextDouble() * 25;
            case "RAIN" -> 2 + random.nextDouble() * 15;
            default -> 0.0;
        };

        boolean trafficJam;
        if (isRushHour && !isWeekend) trafficJam = random.nextDouble() < 0.35;
        else if (!isWeekend) trafficJam = random.nextDouble() < 0.10;
        else trafficJam = random.nextDouble() < 0.05;

        int totalCapacity = 10 + random.nextInt(41);
        double occupancyRate = isRushHour
                ? 55 + random.nextDouble() * 45
                : isWeekend ? 20 + random.nextDouble() * 50 : 30 + random.nextDouble() * 55;
        int bookedSeats = (int) Math.round(totalCapacity * occupancyRate / 100.0);
        int availableSeats = Math.max(0, totalCapacity - bookedSeats);
        double basePrice = 5 + distanceKm * 0.08 + random.nextDouble() * 5;

        String status = STATUSES[random.nextInt(STATUSES.length)];
        boolean wasCancelled = "CANCELLED".equals(status);

        // Delay calculation (ML target variable)
        int actualDelay = 0;
        actualDelay += switch (weather) {
            case "STORM" -> 25 + random.nextInt(30);
            case "SANDSTORM" -> 15 + random.nextInt(25);
            case "RAIN" -> 5 + random.nextInt(20);
            default -> random.nextInt(5);
        };
        if (trafficJam) actualDelay += 10 + random.nextInt(20);
        if (isRushHour && !isWeekend) actualDelay += 5 + random.nextInt(10);
        if (dayOfWeek == DayOfWeek.MONDAY || dayOfWeek == DayOfWeek.FRIDAY) actualDelay += random.nextInt(8);
        if (distanceKm > 120) actualDelay += random.nextInt(12);
        if (occupancyRate > 85) actualDelay += 3 + random.nextInt(7);
        if (windSpeed > 60) actualDelay += 5 + random.nextInt(10);
        if (precipitation > 10) actualDelay += 3 + random.nextInt(8);
        actualDelay += random.nextInt(11) - 5;
        actualDelay = Math.max(0, Math.min(120, actualDelay));

        int ruleBasedDelay = switch (weather) {
            case "RAIN" -> 25;
            case "SANDSTORM" -> 30;
            case "STORM" -> 40;
            default -> 0;
        };
        if (trafficJam) ruleBasedDelay += 20;

        return TransportAiTripDataset.builder()
                .host(host)
                .hostEmail(hostEmail)
                .transport(null)
                .trajet(null)
                .departureStation(null)
                .arrivalStation(null)
                .dataOrigin(DATA_ORIGIN)
                .departureCity(departureCity)
                .arrivalCity(arrivalCity)
                .departurePoint(departureCity + " Station")
                .departureDate(departureDate)
                .departureDayOfWeek(dayOfWeek.name())
                .departureHour(hour)
                .isWeekend(isWeekend)
                .distanceKm(round(distanceKm))
                .estimatedDurationMinutes((int) (distanceKm * 0.8 + 10))
                .transportStatus(status)
                .weather(weather)
                .weatherTemperature(round(baseTemp))
                .weatherWindSpeed(round(windSpeed))
                .weatherPrecipitation(round(precipitation))
                .trafficJam(trafficJam)
                .totalCapacity(totalCapacity)
                .availableSeats(availableSeats)
                .bookedSeats(bookedSeats)
                .occupancyRate(round(occupancyRate))
                .basePrice(round(basePrice))
                .ruleBasedDelayMinutes(ruleBasedDelay)
                .actualDepartureDate(wasCancelled ? null : departureDate.plusMinutes(actualDelay))
                .actualDelayMinutes(wasCancelled ? null : actualDelay)
                .wasCancelled(wasCancelled)
                .build();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}