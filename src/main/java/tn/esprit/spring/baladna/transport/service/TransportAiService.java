package tn.esprit.spring.baladna.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.transport.dto.BestTransportRecommendationDTO;
import tn.esprit.spring.baladna.transport.dto.HostTransportAiReportDTO;
import tn.esprit.spring.baladna.transport.dto.TransportAiAlertDTO;
import tn.esprit.spring.baladna.transport.dto.TransportAiDelayModelPredictionDTO;
import tn.esprit.spring.baladna.transport.dto.TransportAiOptionDTO;
import tn.esprit.spring.baladna.transport.dto.TransportDelayPredictionDTO;
import tn.esprit.spring.baladna.transport.entity.Reservation;
import tn.esprit.spring.baladna.transport.entity.ReservationStatus;
import tn.esprit.spring.baladna.transport.entity.TrafficCongestionLevel;
import tn.esprit.spring.baladna.transport.entity.Transport;
import tn.esprit.spring.baladna.transport.entity.TransportStatus;
import tn.esprit.spring.baladna.transport.entity.WeatherCondition;
import tn.esprit.spring.baladna.transport.repository.ReservationRepository;
import tn.esprit.spring.baladna.transport.repository.TransportRepository;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransportAiService {

    private static final List<ReservationStatus> ACTIVE_RESERVATION_STATUSES = List.of(
            ReservationStatus.PENDING_APPROVAL,
            ReservationStatus.CONFIRMED,
            ReservationStatus.BOARDED
    );

    private final TransportRepository transportRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final TransportAiDatasetService transportAiDatasetService;
    private final TransportAiDelayModelService transportAiDelayModelService;

    public BestTransportRecommendationDTO recommendBestTransport(
            String departureQuery,
            String arrivalQuery,
            String userEmail
    ) {
        User user = findUser(userEmail).orElse(null);
        List<Transport> candidates = transportRepository.findAvailableTransports(LocalDateTime.now()).stream()
                .filter(this::isBookableTransport)
                .filter(transport -> matchesTransportSearch(transport, departureQuery, arrivalQuery))
                .filter(transport -> canRecommendToUser(user, transport))
                .toList();

        if (candidates.isEmpty()) {
            return BestTransportRecommendationDTO.builder()
                    .generatedAt(LocalDateTime.now())
                    .departureQuery(trimToEmpty(departureQuery))
                    .arrivalQuery(trimToEmpty(arrivalQuery))
                    .analyzedCount(0)
                    .summary("No suitable transport matched the current search.")
                    .recommended(null)
                    .alternatives(List.of())
                    .build();
        }

        double minPrice = candidates.stream()
                .map(Transport::getBasePrice)
                .filter(Objects::nonNull)
                .min(Double::compareTo)
                .orElse(0.0);

        double maxPrice = candidates.stream()
                .map(Transport::getBasePrice)
                .filter(Objects::nonNull)
                .max(Double::compareTo)
                .orElse(minPrice);

        List<TransportAiOptionDTO> rankedOptions = candidates.stream()
                .map(transport -> toAiOption(transport, minPrice, maxPrice))
                .sorted(Comparator
                        .comparingInt((TransportAiOptionDTO option) -> option.getAiScore() != null ? option.getAiScore() : 0)
                        .reversed()
                        .thenComparing(TransportAiOptionDTO::getDepartureDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        TransportAiOptionDTO recommended = rankedOptions.get(0);
        List<TransportAiOptionDTO> alternatives = rankedOptions.stream()
                .skip(1)
                .limit(2)
                .toList();

        Long recommendationFeedbackId = transportAiDatasetService.recordRecommendationGenerated(
                user,
                trimToEmpty(departureQuery),
                trimToEmpty(arrivalQuery),
                recommended
        );

        return BestTransportRecommendationDTO.builder()
                .generatedAt(LocalDateTime.now())
                .departureQuery(trimToEmpty(departureQuery))
                .arrivalQuery(trimToEmpty(arrivalQuery))
                .analyzedCount(rankedOptions.size())
                .summary(buildRecommendationSummary(recommended, rankedOptions.size()))
                .recommendationFeedbackId(recommendationFeedbackId)
                .recommended(recommended)
                .alternatives(alternatives)
                .build();
    }

    public TransportDelayPredictionDTO predictDelay(Transport transport) {
        int baseDelay = transport.calculateDelay();
        int predictedDelay = baseDelay;
        List<String> factors = new ArrayList<>();
        TrafficCongestionLevel congestionLevel = transport.getEffectiveTrafficCongestionLevel();

        WeatherCondition weather = transport.getWeather();
        if (weather == WeatherCondition.RAIN) {
            factors.add("Rain may slow down road conditions and boarding.");
        } else if (weather == WeatherCondition.SANDSTORM) {
            factors.add("Sandstorm conditions reduce visibility and operational speed.");
        } else if (weather == WeatherCondition.STORM) {
            factors.add("Storm conditions create a major disruption risk.");
        } else {
            factors.add("Current weather looks operationally stable.");
        }

        if (congestionLevel == TrafficCongestionLevel.LOW) {
            factors.add("Light corridor congestion is expected for this departure.");
        } else if (congestionLevel == TrafficCongestionLevel.MEDIUM) {
            factors.add("Moderate corridor congestion is already included in the forecast.");
        } else if (congestionLevel == TrafficCongestionLevel.HIGH) {
            predictedDelay += 5;
            factors.add("Heavy corridor congestion raises the delay risk beyond the base route estimate.");
        }

        if (isRushHour(transport.getDepartureDate())) {
            predictedDelay += 10;
            factors.add("Departure is scheduled during rush hour.");
        }

        if (isPeakWeekday(transport.getDepartureDate())) {
            predictedDelay += 5;
            factors.add("The selected day tends to be busier than average.");
        }

        double distanceKm = transport.getTrajet() != null && transport.getTrajet().getDistanceKm() != null
                ? transport.getTrajet().getDistanceKm()
                : 0.0;
        if (distanceKm >= 120) {
            predictedDelay += 8;
            factors.add("Longer routes are more exposed to cascading delays.");
        }

        int estimatedDurationMinutes = transport.getTrajet() != null && transport.getTrajet().getEstimatedDurationMinutes() != null
                ? transport.getTrajet().getEstimatedDurationMinutes()
                : 0;
        if (estimatedDurationMinutes >= 120) {
            predictedDelay += 6;
            factors.add("The mapped route already shows a long travel window.");
        } else if (estimatedDurationMinutes >= 75) {
            predictedDelay += 3;
            factors.add("The route baseline suggests a fairly long trip, which leaves more room for slippage.");
        }

        double occupancyRate = getOccupancyRate(transport);
        if (occupancyRate >= 85) {
            predictedDelay += 4;
            factors.add("High occupancy can slow boarding and dispatch.");
        }

        Double windSpeed = transport.getWeatherWindSpeed();
        if (windSpeed != null && windSpeed >= 60) {
            predictedDelay += 12;
            factors.add("Strong wind may reduce the operating speed.");
        } else if (windSpeed != null && windSpeed >= 40) {
            predictedDelay += 6;
            factors.add("Moderate wind can create minor slowdowns.");
        }

        Double precipitation = transport.getWeatherPrecipitation();
        if (precipitation != null && precipitation >= 10) {
            predictedDelay += 10;
            factors.add("Heavy precipitation raises disruption probability.");
        } else if (precipitation != null && precipitation >= 3) {
            predictedDelay += 4;
            factors.add("Light precipitation may create small operational delays.");
        }

        TransportAiDelayModelPredictionDTO modelPrediction =
                transportAiDelayModelService.predictDelayWithModel(transport, predictedDelay);
        String predictionSource = "RULE_BASED";
        Long trainedModelId = null;
        Integer trainedModelSampleCount = null;
        Double trainedModelMeanAbsoluteError = null;

        if (modelPrediction != null) {
            predictedDelay = modelPrediction.getPredictionMinutes() != null
                    ? modelPrediction.getPredictionMinutes()
                    : predictedDelay;
            predictionSource = "TRAINED_MODEL";
            trainedModelId = modelPrediction.getModelId();
            trainedModelSampleCount = modelPrediction.getSampleCount();
            trainedModelMeanAbsoluteError = modelPrediction.getMeanAbsoluteError();
            if (trainedModelSampleCount != null && trainedModelSampleCount > 0) {
                factors.add("A trained delay model refined the forecast using " + trainedModelSampleCount + " trip record(s).");
            } else {
                factors.add("A trained delay model refined the forecast.");
            }
        }

        predictedDelay = Math.max(baseDelay, predictedDelay);
        predictedDelay = Math.min(predictedDelay, 120);

        String riskLevel = determineRiskLevel(predictedDelay, weather);
        String primaryReason = buildPrimaryReason(factors, predictedDelay, weather, congestionLevel);
        int confidencePercent = calculateConfidencePercent(predictionSource, trainedModelSampleCount, trainedModelMeanAbsoluteError, factors.size(), riskLevel);
        LocalDateTime estimatedArrivalDate = transport.getDepartureDate();
        if (estimatedArrivalDate != null) {
            int estimatedDuration = transport.getTrajet() != null && transport.getTrajet().getEstimatedDurationMinutes() != null
                    ? transport.getTrajet().getEstimatedDurationMinutes()
                    : 0;
            estimatedArrivalDate = estimatedArrivalDate.plusMinutes((long) predictedDelay + estimatedDuration);
        }

        TransportDelayPredictionDTO prediction = TransportDelayPredictionDTO.builder()
                .transportId(transport.getId())
                .ruleBasedDelayMinutes(baseDelay)
                .predictedDelayMinutes(predictedDelay)
                .riskLevel(riskLevel)
                .confidencePercent(confidencePercent)
                .primaryReason(primaryReason)
                .estimatedArrivalDate(estimatedArrivalDate)
                .predictionSource(predictionSource)
                .trainedModelId(trainedModelId)
                .trainedModelSampleCount(trainedModelSampleCount)
                .trainedModelMeanAbsoluteError(trainedModelMeanAbsoluteError)
                .explanation(buildDelayExplanation(predictedDelay, factors))
                .recommendation(buildDelayRecommendation(riskLevel))
                .factors(factors)
                .build();

        return prediction;
    }

    public List<TransportAiAlertDTO> getHostAlerts(String hostEmail) {

        List<Transport> hostTransports =
                transportRepository.findByHostEmailOrderByDepartureDateDesc(hostEmail);

        List<Reservation> activeReservations =
                reservationRepository.findByTransportHostEmail(hostEmail).stream()
                        .filter(this::isActiveReservation)
                        .toList();

        Map<Long, List<Reservation>> reservationsByTransport =
                activeReservations.stream()
                        .collect(Collectors.groupingBy(r -> r.getTransport().getId()));

        LocalDateTime now = LocalDateTime.now();
        List<TransportAiAlertDTO> alerts = new ArrayList<>();

        for (Transport transport : hostTransports) {

            if (transport.getDepartureDate() == null
                    || !transport.getDepartureDate().isAfter(now)) continue;

            if (transport.getStatus() == TransportStatus.CANCELLED
                    || transport.getStatus() == TransportStatus.COMPLETED) continue;

            TransportDelayPredictionDTO prediction = predictDelay(transport);

            int delay = prediction.getPredictedDelayMinutes() != null
                    ? prediction.getPredictedDelayMinutes()
                    : 0;

            int occupancy = getOccupancyRate(transport);
            long hours = ChronoUnit.HOURS.between(now, transport.getDepartureDate());

            boolean severeWeather = isSevereWeather(transport.getWeather());

            // ===============================
            // ? CRITICAL ALERT
            // ===============================
            if ((severeWeather && delay >= 20) || delay >= 45) {

                alerts.add(TransportAiAlertDTO.builder()
                        .transportId(transport.getId())
                        .severity("CRITICAL")
                        .title("Critical disruption risk")
                        .message("Severe conditions and high delay risk detected.")
                        .suggestedAction("Consider cancellation or urgent rerouting.")
                        .routeLabel(getRouteLabel(transport))
                        .departureDate(transport.getDepartureDate())
                        .predictedDelayMinutes(delay)
                        .build());

                continue;
            }

            // ===============================
            // ?? WARNING ALERT
            // ===============================
            if (delay >= 30) {

                alerts.add(TransportAiAlertDTO.builder()
                        .transportId(transport.getId())
                        .severity("WARNING")
                        .title("High delay risk")
                        .message("Delay may impact passenger experience.")
                        .suggestedAction("Notify passengers and monitor closely.")
                        .routeLabel(getRouteLabel(transport))
                        .departureDate(transport.getDepartureDate())
                        .predictedDelayMinutes(delay)
                        .build());

            }

            // ===============================
            // ? PASSENGER ALERT
            // ===============================
            int activeBookings = reservationsByTransport
                    .getOrDefault(transport.getId(), List.of()).size();

            if (activeBookings > 0 && delay >= 25) {

                alerts.add(TransportAiAlertDTO.builder()
                        .transportId(transport.getId())
                        .severity("WARNING")
                        .title("Passengers impacted")
                        .message(activeBookings + " passengers may be affected.")
                        .suggestedAction("Send delay notification.")
                        .routeLabel(getRouteLabel(transport))
                        .departureDate(transport.getDepartureDate())
                        .predictedDelayMinutes(delay)
                        .build());
            }

            // ===============================
            // ? LOW DEMAND ALERT
            // ===============================
            if (hours <= 24 && occupancy <= 25) {

                alerts.add(TransportAiAlertDTO.builder()
                        .transportId(transport.getId())
                        .severity("INFO")
                        .title("Low demand detected")
                        .message("Transport has low occupancy close to departure.")
                        .suggestedAction("Consider promotion or rescheduling.")
                        .routeLabel(getRouteLabel(transport))
                        .departureDate(transport.getDepartureDate())
                        .predictedDelayMinutes(delay)
                        .build());
            }
        }

        return alerts.stream()
                .sorted(Comparator
                        .comparingInt((TransportAiAlertDTO a) -> severityRank(a.getSeverity()))
                        .reversed()
                        .thenComparing(TransportAiAlertDTO::getDepartureDate))
                .limit(5)
                .toList();
    }

    public HostTransportAiReportDTO getHostReport(String hostEmail) {
        List<Transport> hostTransports = transportRepository.findByHostEmailOrderByDepartureDateDesc(hostEmail);
        List<Reservation> activeReservations = reservationRepository.findByTransportHostEmail(hostEmail).stream()
                .filter(this::isActiveReservation)
                .toList();

        LocalDateTime now = LocalDateTime.now();
        List<Transport> upcomingTransports = hostTransports.stream()
                .filter(transport -> transport.getDepartureDate() != null && transport.getDepartureDate().isAfter(now))
                .filter(transport -> transport.getStatus() != TransportStatus.CANCELLED && transport.getStatus() != TransportStatus.COMPLETED)
                .toList();

        int atRiskTransports = (int) upcomingTransports.stream()
                .map(this::predictDelay)
                .filter(prediction -> prediction.getPredictedDelayMinutes() != null && prediction.getPredictedDelayMinutes() >= 25)
                .count();

        int lowOccupancyTransports = (int) upcomingTransports.stream()
                .filter(transport -> ChronoUnit.HOURS.between(now, transport.getDepartureDate()) <= 24)
                .filter(transport -> getOccupancyRate(transport) < 35)
                .count();

        int averageOccupancyRate = upcomingTransports.isEmpty()
                ? 0
                : (int) Math.round(
                        upcomingTransports.stream()
                                .mapToInt(this::getOccupancyRate)
                                .average()
                                .orElse(0)
                );

        double estimatedRevenue = roundMoney(
                activeReservations.stream()
                        .map(Reservation::getTotalPrice)
                        .filter(Objects::nonNull)
                        .mapToDouble(Double::doubleValue)
                        .sum()
        );

        Map<String, Integer> bookingsByRoute = new HashMap<>();
        for (Reservation reservation : activeReservations) {
            String routeLabel = getRouteLabel(reservation.getTransport());
            bookingsByRoute.merge(routeLabel, reservation.getReservedSeats(), Integer::sum);
        }

        Map.Entry<String, Integer> topRouteEntry = bookingsByRoute.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(null);

        HostTransportAiReportDTO report = HostTransportAiReportDTO.builder()
                .generatedAt(LocalDateTime.now())
                .totalUpcomingTransports(upcomingTransports.size())
                .activeBookings(activeReservations.size())
                .atRiskTransports(atRiskTransports)
                .lowOccupancyTransports(lowOccupancyTransports)
                .averageOccupancyRate(averageOccupancyRate)
                .estimatedRevenue(estimatedRevenue)
                .topRouteLabel(topRouteEntry != null ? topRouteEntry.getKey() : "No route data yet")
                .topRouteBookingCount(topRouteEntry != null ? topRouteEntry.getValue() : 0)
                .recommendations(buildHostRecommendations(atRiskTransports, lowOccupancyTransports, averageOccupancyRate))
                .build();

        return report;
    }

    private Optional<User> findUser(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(userEmail);
    }

    private boolean isBookableTransport(Transport transport) {
        return transport.getStatus() == TransportStatus.SCHEDULED
                && transport.getDepartureDate() != null
                && transport.getDepartureDate().isAfter(LocalDateTime.now())
                && transport.getAvailableSeats() != null
                && transport.getAvailableSeats() > 0;
    }

    private boolean canRecommendToUser(User user, Transport transport) {
        if (user == null || user.getId() == null) {
            return true;
        }

        return !reservationRepository.existsByUserIdAndTransportIdAndStatusIn(
                user.getId(),
                transport.getId(),
                ACTIVE_RESERVATION_STATUSES
        );
    }

    private TransportAiOptionDTO toAiOption(Transport transport, double minPrice, double maxPrice) {
        TransportDelayPredictionDTO prediction = predictDelay(transport);
        int aiScore = calculateRecommendationScore(transport, prediction, minPrice, maxPrice);

        return TransportAiOptionDTO.builder()
                .transportId(transport.getId())
                .routeLabel(getRouteLabel(transport))
                .departurePoint(transport.getDeparturePoint())
                .departureCity(getDepartureCity(transport))
                .arrivalCity(getArrivalCity(transport))
                .departureDate(transport.getDepartureDate())
                .availableSeats(transport.getAvailableSeats())
                .totalCapacity(transport.getTotalCapacity())
                .basePrice(transport.getBasePrice())
                .weather(transport.getWeather() != null ? transport.getWeather().name() : null)
                .predictedDelayMinutes(prediction.getPredictedDelayMinutes())
                .aiScore(aiScore)
                .reasons(buildRecommendationReasons(transport, prediction, minPrice, maxPrice))
                .warnings(buildRecommendationWarnings(transport, prediction))
                .build();
    }

    private int calculateRecommendationScore(
            Transport transport,
            TransportDelayPredictionDTO prediction,
            double minPrice,
            double maxPrice
    ) {
        double price = transport.getBasePrice() != null ? transport.getBasePrice() : minPrice;

        double priceScore = maxPrice <= minPrice
                ? 1.0
                : 1.0 - ((price - minPrice) / (maxPrice - minPrice));

        int delay = prediction.getPredictedDelayMinutes() != null
                ? prediction.getPredictedDelayMinutes()
                : 0;

        double delayScore = 1.0 - Math.min(delay, 60) / 60.0;

        double seatScore = 0.0;
        if (transport.getTotalCapacity() != null
                && transport.getTotalCapacity() > 0
                && transport.getAvailableSeats() != null) {

            double ratio = transport.getAvailableSeats() / (double) transport.getTotalCapacity();
            seatScore = Math.max(0.0, Math.min(1.0, ratio));

            if (transport.getAvailableSeats() <= 2) {
                seatScore *= 0.5;
            }
        }

        WeatherCondition weather = transport.getWeather() != null
                ? transport.getWeather()
                : WeatherCondition.SUNNY;

        double weatherScore = switch (weather) {
            case SUNNY -> 1.0;
            case RAIN -> 0.6;
            case SANDSTORM -> 0.3;
            case STORM -> 0.1;
        };

        double departureScore = getDepartureTimingScore(transport.getDepartureDate());

        if (transport.getDepartureDate() != null) {
            long hours = ChronoUnit.HOURS.between(LocalDateTime.now(), transport.getDepartureDate());
            if (hours < 2) {
                departureScore *= 0.4;
            }
        }

        if (weather == WeatherCondition.STORM && delay >= 30) {
            delayScore *= 0.5;
        }

        double weightedScore =
                (priceScore * 20)
                        + (delayScore * 30)
                        + (seatScore * 20)
                        + (weatherScore * 15)
                        + (departureScore * 15);

        return (int) Math.max(0, Math.min(100, Math.round(weightedScore)));
    }

    private List<String> buildRecommendationReasons(
            Transport transport,
            TransportDelayPredictionDTO prediction,
            double minPrice,
            double maxPrice
    ) {
        List<String> reasons = new ArrayList<>();

        Integer predictedDelay = prediction.getPredictedDelayMinutes();
        if (predictedDelay != null && predictedDelay <= 10) {
            reasons.add("Very low predicted delay.");
        } else if (predictedDelay != null && predictedDelay <= 20) {
            reasons.add("Moderate delay risk remains manageable.");
        }

        if (transport.getWeather() == WeatherCondition.SUNNY) {
            reasons.add("Weather conditions are favorable.");
        } else if (transport.getWeather() == WeatherCondition.RAIN) {
            reasons.add("Weather remains acceptable compared with riskier departures.");
        }

        double price = transport.getBasePrice() != null ? transport.getBasePrice() : minPrice;
        double priceScore = maxPrice <= minPrice ? 1.0 : 1.0 - ((price - minPrice) / (maxPrice - minPrice));
        if (priceScore >= 0.7) {
            reasons.add("Competitive price for the current route.");
        }

        if (transport.getAvailableSeats() != null && transport.getTotalCapacity() != null) {
            double seatsRatio = transport.getAvailableSeats() / (double) transport.getTotalCapacity();
            if (seatsRatio >= 0.4) {
                reasons.add("Healthy seat availability.");
            }
        }

        if (getDepartureTimingScore(transport.getDepartureDate()) >= 0.8) {
            reasons.add("Convenient upcoming departure time.");
        }

        if (reasons.isEmpty()) {
            reasons.add("Balanced trade-off across reliability, price, and seat availability.");
        }

        return reasons.stream().limit(4).toList();
    }

    private List<String> buildRecommendationWarnings(Transport transport, TransportDelayPredictionDTO prediction) {
        List<String> warnings = new ArrayList<>();

        if (prediction.getPredictedDelayMinutes() != null && prediction.getPredictedDelayMinutes() >= 25) {
            warnings.add("Delay risk is elevated for this departure.");
        }

        if (transport.getAvailableSeats() != null && transport.getAvailableSeats() <= 2) {
            warnings.add("Very limited seat inventory remains.");
        }

        if (isSevereWeather(transport.getWeather())) {
            warnings.add("Severe weather may force schedule changes.");
        }

        return warnings;
    }

    private boolean matchesTransportSearch(Transport transport, String departureQuery, String arrivalQuery) {
        String departureText = normalizeText(String.join(" ",
                trimToEmpty(transport.getDeparturePoint()),
                trimToEmpty(getRouteLabel(transport)),
                trimToEmpty(getDepartureStationName(transport)),
                trimToEmpty(getDepartureCity(transport))
        ));

        String arrivalText = normalizeText(String.join(" ",
                trimToEmpty(getRouteLabel(transport)),
                trimToEmpty(getArrivalStationName(transport)),
                trimToEmpty(getArrivalCity(transport))
        ));

        boolean matchesDeparture = departureQuery == null || departureQuery.isBlank()
                || departureText.contains(normalizeText(departureQuery));
        boolean matchesArrival = arrivalQuery == null || arrivalQuery.isBlank()
                || arrivalText.contains(normalizeText(arrivalQuery));

        return matchesDeparture && matchesArrival;
    }

    private String buildRecommendationSummary(TransportAiOptionDTO recommended, int analyzedCount) {
        if (recommended == null) {
            return "No recommendation is currently available.";
        }

        return "AI selected this departure as the best balance of reliability, price, and availability across "
                + analyzedCount
                + " option(s).";
    }

    private String buildDelayExplanation(int predictedDelay, List<String> factors) {
        String dominantFactors = factors.stream()
                .limit(3)
                .collect(Collectors.joining(" "));

        return "Expected delay: +" + predictedDelay + " min. This estimate uses current weather, route duration, traffic conditions, departure timing, and previous trip history when available. " + dominantFactors;
    }

    private String buildPrimaryReason(
            List<String> factors,
            int predictedDelay,
            WeatherCondition weather,
            TrafficCongestionLevel congestionLevel
    ) {
        if (weather == WeatherCondition.STORM || weather == WeatherCondition.SANDSTORM) {
            return "Severe weather is the main delay risk.";
        }
        if (congestionLevel == TrafficCongestionLevel.HIGH) {
            return "Heavy traffic is the main delay risk.";
        }
        if (isHighDelay(predictedDelay)) {
            return factors.stream()
                    .filter(factor -> factor != null && !factor.toLowerCase(Locale.ROOT).contains("trained delay model"))
                    .findFirst()
                    .orElse("Several trip conditions are increasing the delay risk.");
        }
        return "Trip conditions look stable, with only limited delay risk.";
    }

    private int calculateConfidencePercent(
            String predictionSource,
            Integer trainedModelSampleCount,
            Double trainedModelMeanAbsoluteError,
            int factorCount,
            String riskLevel
    ) {
        int confidence = "TRAINED_MODEL".equals(predictionSource) ? 82 : 68;

        if (trainedModelSampleCount != null) {
            confidence += Math.min(10, trainedModelSampleCount / 100);
        }
        if (trainedModelMeanAbsoluteError != null) {
            confidence -= Math.min(20, (int) Math.round(trainedModelMeanAbsoluteError * 1.5));
        }
        if (factorCount >= 4) {
            confidence += 4;
        }
        if ("CRITICAL".equals(riskLevel)) {
            confidence -= 6;
        }

        return Math.max(50, Math.min(95, confidence));
    }

    private boolean isHighDelay(int predictedDelay) {
        return predictedDelay >= 25;
    }

    private String buildDelayRecommendation(String riskLevel) {
        return switch (riskLevel) {
            case "LOW" -> "Proceed as planned.";
            case "MEDIUM" -> "Monitor conditions and keep passengers informed if the forecast changes.";
            case "HIGH" -> "Prepare a delay message and review backup departures.";
            default -> "Escalate to the host immediately and consider rerouting or cancellation.";
        };
    }

    private String determineRiskLevel(int predictedDelay, WeatherCondition weather) {
        if (weather == WeatherCondition.STORM || predictedDelay >= 45) {
            return "CRITICAL";
        }
        if (predictedDelay >= 30) {
            return "HIGH";
        }
        if (predictedDelay >= 15) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private boolean isRushHour(LocalDateTime departureDate) {
        if (departureDate == null) {
            return false;
        }
        int hour = departureDate.getHour();
        return (hour >= 7 && hour <= 9) || (hour >= 16 && hour <= 19);
    }

    private boolean isPeakWeekday(LocalDateTime departureDate) {
        if (departureDate == null) {
            return false;
        }
        DayOfWeek dayOfWeek = departureDate.getDayOfWeek();
        return dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.MONDAY;
    }

    private boolean isActiveReservation(Reservation reservation) {
        return ACTIVE_RESERVATION_STATUSES.contains(reservation.getStatus());
    }

    private boolean isSevereWeather(WeatherCondition weatherCondition) {
        return weatherCondition == WeatherCondition.STORM || weatherCondition == WeatherCondition.SANDSTORM;
    }

    private int getOccupancyRate(Transport transport) {
        if (transport.getTotalCapacity() == null || transport.getTotalCapacity() <= 0 || transport.getAvailableSeats() == null) {
            return 0;
        }

        int occupiedSeats = Math.max(0, transport.getTotalCapacity() - transport.getAvailableSeats());
        return (int) Math.round((occupiedSeats / (double) transport.getTotalCapacity()) * 100);
    }

    private double getDepartureTimingScore(LocalDateTime departureDate) {
        if (departureDate == null) {
            return 0.0;
        }

        long hoursUntilDeparture = ChronoUnit.HOURS.between(LocalDateTime.now(), departureDate);
        if (hoursUntilDeparture < 1) {
            return 0.2;
        }
        if (hoursUntilDeparture <= 12) {
            return 1.0;
        }
        if (hoursUntilDeparture <= 24) {
            return 0.9;
        }
        if (hoursUntilDeparture <= 48) {
            return 0.7;
        }
        if (hoursUntilDeparture <= 72) {
            return 0.5;
        }
        return 0.3;
    }

    private String getRouteLabel(Transport transport) {
        if (transport.getTrajet() == null) {
            return "Transport route";
        }
        return getDepartureStationName(transport) + " -> " + getArrivalStationName(transport);
    }

    private String getDepartureStationName(Transport transport) {
        if (transport.getTrajet() == null || transport.getTrajet().getDepartureStation() == null) {
            return "Departure";
        }
        return trimToEmpty(transport.getTrajet().getDepartureStation().getName());
    }

    private String getArrivalStationName(Transport transport) {
        if (transport.getTrajet() == null || transport.getTrajet().getArrivalStation() == null) {
            return "Arrival";
        }
        return trimToEmpty(transport.getTrajet().getArrivalStation().getName());
    }

    private String getDepartureCity(Transport transport) {
        if (transport.getTrajet() == null || transport.getTrajet().getDepartureStation() == null) {
            return "";
        }
        return trimToEmpty(transport.getTrajet().getDepartureStation().getCity());
    }

    private String getArrivalCity(Transport transport) {
        if (transport.getTrajet() == null || transport.getTrajet().getArrivalStation() == null) {
            return "";
        }
        return trimToEmpty(transport.getTrajet().getArrivalStation().getCity());
    }

    private List<String> buildHostRecommendations(int atRisk, int lowOccupancy, int averageOccupancyRate) {
        List<String> recommendations = new ArrayList<>();

        if (atRisk > 0) {
            recommendations.add("Prioritize passenger communication for at-risk departures.");
        }
        if (lowOccupancy > 0) {
            recommendations.add("Review pricing or consolidation for low-demand departures in the next 24 hours.");
        }
        if (averageOccupancyRate >= 85) {
            recommendations.add("Strong occupancy suggests adding capacity on your best-performing routes.");
        }
        if (averageOccupancyRate > 0 && averageOccupancyRate < 50) {
            recommendations.add("Demand is soft overall, so focus on promotion or schedule optimization.");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Operations look stable. Maintain the current schedule and continue monitoring weather.");
        }

        return recommendations;
    }

    private int severityRank(String severity) {
        if ("CRITICAL".equalsIgnoreCase(severity)) {
            return 3;
        }
        if ("WARNING".equalsIgnoreCase(severity)) {
            return 2;
        }
        return 1;
    }

    private String normalizeText(String value) {
        return trimToEmpty(value).toLowerCase(Locale.ROOT);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private double roundMoney(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
