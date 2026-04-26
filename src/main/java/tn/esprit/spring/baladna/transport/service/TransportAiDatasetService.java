package tn.esprit.spring.baladna.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.baladna.transport.dto.HostTransportAiReportDTO;
import tn.esprit.spring.baladna.transport.dto.TransportAiDatasetSummaryDTO;
import tn.esprit.spring.baladna.transport.dto.TransportAiOptionDTO;
import tn.esprit.spring.baladna.transport.dto.TransportDelayPredictionDTO;
import tn.esprit.spring.baladna.transport.entity.Reservation;
import tn.esprit.spring.baladna.transport.entity.ReservationStatus;
import tn.esprit.spring.baladna.transport.entity.Trajet;
import tn.esprit.spring.baladna.transport.entity.Transport;
import tn.esprit.spring.baladna.transport.entity.TransportAiHostDailyMetrics;
import tn.esprit.spring.baladna.transport.entity.TransportAiIncidentDataset;
import tn.esprit.spring.baladna.transport.entity.TransportAiRecommendationFeedback;
import tn.esprit.spring.baladna.transport.entity.TransportAiTripDataset;
import tn.esprit.spring.baladna.transport.entity.TransportStatus;
import tn.esprit.spring.baladna.transport.entity.WeatherCondition;
import tn.esprit.spring.baladna.transport.repository.TransportAiHostDailyMetricsRepository;
import tn.esprit.spring.baladna.transport.repository.TransportAiIncidentDatasetRepository;
import tn.esprit.spring.baladna.transport.repository.TransportAiRecommendationFeedbackRepository;
import tn.esprit.spring.baladna.transport.repository.TransportAiTripDatasetRepository;
import tn.esprit.spring.baladna.transport.repository.TransportRepository;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TransportAiDatasetService {

    public static final String DATA_ORIGIN_REAL = "REAL";
    public static final String DATA_ORIGIN_BOOTSTRAP_IMPORT = "BOOTSTRAP_IMPORT";

    private final TransportRepository transportRepository;
    private final UserRepository userRepository;
    private final TransportAiTripDatasetRepository tripDatasetRepository;
    private final TransportAiIncidentDatasetRepository incidentDatasetRepository;
    private final TransportAiHostDailyMetricsRepository hostDailyMetricsRepository;
    private final TransportAiRecommendationFeedbackRepository recommendationFeedbackRepository;

    @Transactional
    public TransportAiTripDataset syncTransportTripDataset(Transport transport) {
        if (transport == null || transport.getId() == null) {
            return null;
        }

        TransportAiTripDataset dataset = tripDatasetRepository.findByTransportId(transport.getId())
                .orElseGet(TransportAiTripDataset::new);

        Trajet trajet = transport.getTrajet();
        Integer totalCapacity = transport.getTotalCapacity();
        Integer availableSeats = transport.getAvailableSeats() != null
                ? transport.getAvailableSeats()
                : totalCapacity;
        int bookedSeats = totalCapacity != null && availableSeats != null
                ? Math.max(0, totalCapacity - availableSeats)
                : 0;
        double occupancyRate = totalCapacity != null && totalCapacity > 0
                ? roundDouble((bookedSeats / (double) totalCapacity) * 100.0)
                : 0.0;

        LocalDateTime departureDate = transport.getDepartureDate();
        Integer actualDelayMinutes = transport.getActualDelayMinutes();
        LocalDateTime actualDepartureDate = departureDate != null && actualDelayMinutes != null
                ? departureDate.plusMinutes(actualDelayMinutes)
                : null;
        LocalDateTime actualArrivalDate = actualDepartureDate != null && trajet != null && trajet.getEstimatedDurationMinutes() != null
                ? actualDepartureDate.plusMinutes(trajet.getEstimatedDurationMinutes())
                : null;

        dataset.setTransport(transport);
        dataset.setTrajet(trajet);
        dataset.setHost(transport.getHost());
        dataset.setDataOrigin(DATA_ORIGIN_REAL);
        dataset.setDepartureStation(trajet != null ? trajet.getDepartureStation() : null);
        dataset.setArrivalStation(trajet != null ? trajet.getArrivalStation() : null);
        dataset.setDepartureCity(trajet != null && trajet.getDepartureStation() != null ? trimToNull(trajet.getDepartureStation().getCity()) : null);
        dataset.setArrivalCity(trajet != null && trajet.getArrivalStation() != null ? trimToNull(trajet.getArrivalStation().getCity()) : null);
        dataset.setDeparturePoint(trimToNull(transport.getDeparturePoint()));
        dataset.setDepartureDate(departureDate);
        dataset.setDepartureDayOfWeek(departureDate != null ? departureDate.getDayOfWeek().name() : null);
        dataset.setDepartureHour(departureDate != null ? departureDate.getHour() : null);
        dataset.setIsWeekend(isWeekend(departureDate));
        dataset.setDistanceKm(trajet != null ? trajet.getDistanceKm() : null);
        dataset.setEstimatedDurationMinutes(trajet != null ? trajet.getEstimatedDurationMinutes() : null);
        dataset.setTransportStatus(transport.getStatus() != null ? transport.getStatus().name() : null);
        dataset.setWeather(transport.getWeather() != null ? transport.getWeather().name() : null);
        dataset.setWeatherTemperature(transport.getWeatherTemperature());
        dataset.setWeatherWindSpeed(transport.getWeatherWindSpeed());
        dataset.setWeatherPrecipitation(transport.getWeatherPrecipitation());
        dataset.setTrafficJam(Boolean.TRUE.equals(transport.getTrafficJam()));
        dataset.setTotalCapacity(totalCapacity);
        dataset.setAvailableSeats(availableSeats);
        dataset.setBookedSeats(bookedSeats);
        dataset.setOccupancyRate(occupancyRate);
        dataset.setBasePrice(transport.getBasePrice());
        dataset.setRuleBasedDelayMinutes(transport.calculateDelay());
        dataset.setActualDepartureDate(actualDepartureDate);
        dataset.setActualArrivalDate(actualArrivalDate);
        dataset.setActualDelayMinutes(actualDelayMinutes);
        dataset.setWasCancelled(transport.getStatus() == TransportStatus.CANCELLED);

        return tripDatasetRepository.save(dataset);
    }

    @Transactional
    public void syncHostTripDatasets(String hostEmail) {
        transportRepository.findByHostEmailOrderByDepartureDateDesc(hostEmail)
                .forEach(this::syncTransportTripDataset);
    }

    @Transactional
    public TransportAiIncidentDataset recordAlertIncident(
            Transport transport,
            TransportDelayPredictionDTO prediction,
            String severity,
            String title,
            String message,
            String suggestedAction,
            int affectedPassengersCount
    ) {
        if (transport == null || transport.getId() == null || severity == null || severity.isBlank()) {
            return null;
        }

        String incidentType = determineIncidentType(transport, title);
        Optional<TransportAiIncidentDataset> existing = incidentDatasetRepository
                .findTopByTransportIdAndIncidentTypeAndIncidentSeverityAndResolvedAtIsNullOrderByCreatedAtDesc(
                        transport.getId(),
                        incidentType,
                        severity
                );

        TransportAiIncidentDataset dataset = existing
                .filter(item -> item.getCreatedAt() != null && item.getCreatedAt().toLocalDate().equals(LocalDate.now()))
                .orElseGet(TransportAiIncidentDataset::new);

        dataset.setTransport(transport);
        dataset.setTrajet(transport.getTrajet());
        dataset.setHost(transport.getHost());
        dataset.setIncidentType(incidentType);
        dataset.setIncidentSeverity(severity);
        dataset.setIncidentSource("AI_ALERT_ENGINE");
        dataset.setIncidentTitle(trimToMaxLength(title, 180));
        dataset.setIncidentDescription(trimToNull(message));
        dataset.setWeather(transport.getWeather() != null ? transport.getWeather().name() : null);
        dataset.setTrafficJam(Boolean.TRUE.equals(transport.getTrafficJam()));
        dataset.setPredictedDelayMinutes(prediction != null ? prediction.getPredictedDelayMinutes() : transport.calculateDelay());
        dataset.setActualDelayMinutes(transport.getActualDelayMinutes());
        dataset.setActionTaken(trimToMaxLength(suggestedAction, 50));
        dataset.setActionResult(trimToMaxLength("OPEN", 50));
        dataset.setAffectedPassengersCount(Math.max(affectedPassengersCount, 0));

        return incidentDatasetRepository.save(dataset);
    }

    @Transactional
    public TransportAiHostDailyMetrics upsertHostDailyMetrics(String hostEmail, HostTransportAiReportDTO report) {
        if (hostEmail == null || hostEmail.isBlank() || report == null) {
            return null;
        }

        User host = userRepository.findByEmail(hostEmail).orElse(null);
        if (host == null) {
            return null;
        }

        TransportAiHostDailyMetrics metrics = hostDailyMetricsRepository.findByHostEmailAndMetricDate(hostEmail, LocalDate.now())
                .orElseGet(TransportAiHostDailyMetrics::new);

        metrics.setHost(host);
        metrics.setMetricDate(LocalDate.now());
        metrics.setTotalTransports(getSafeInteger(report.getTotalUpcomingTransports()));
        metrics.setUpcomingTransports(getSafeInteger(report.getTotalUpcomingTransports()));
        metrics.setCompletedTransports(countHostTransportsByStatus(hostEmail, TransportStatus.COMPLETED));
        metrics.setCancelledTransports(countHostTransportsByStatus(hostEmail, TransportStatus.CANCELLED));
        metrics.setTotalBookings(getSafeInteger(report.getActiveBookings()));
        metrics.setConfirmedBookings(getSafeInteger(report.getActiveBookings()));
        metrics.setCancelledBookings(countCancelledReservationsForHost(hostEmail));
        metrics.setTotalRevenue(roundMoney(report.getEstimatedRevenue()));
        metrics.setAverageOccupancyRate(getSafeDouble(report.getAverageOccupancyRate()));
        metrics.setAverageDelayMinutes(roundDouble(calculateAverageDelay(hostEmail)));
        metrics.setAtRiskTransportsCount(getSafeInteger(report.getAtRiskTransports()));
        metrics.setLowOccupancyTransportsCount(getSafeInteger(report.getLowOccupancyTransports()));
        metrics.setTopRouteLabel(trimToNull(report.getTopRouteLabel()));
        metrics.setTopRouteBookingCount(getSafeInteger(report.getTopRouteBookingCount()));

        return hostDailyMetricsRepository.save(metrics);
    }

    @Transactional
    public Long recordRecommendationGenerated(
            User user,
            String departureQuery,
            String arrivalQuery,
            TransportAiOptionDTO recommended
    ) {
        if (user == null || user.getId() == null || recommended == null || recommended.getTransportId() == null) {
            return null;
        }

        Transport transport = transportRepository.findById(recommended.getTransportId()).orElse(null);
        if (transport == null) {
            return null;
        }

        TransportAiRecommendationFeedback feedback = TransportAiRecommendationFeedback.builder()
                .user(user)
                .recommendedTransport(transport)
                .departureQuery(trimToNull(departureQuery))
                .arrivalQuery(trimToNull(arrivalQuery))
                .recommendedScore(recommended.getAiScore())
                .predictedDelayMinutes(recommended.getPredictedDelayMinutes())
                .priceAtRecommendation(recommended.getBasePrice())
                .weatherAtRecommendation(trimToNull(recommended.getWeather()))
                .availableSeatsAtRecommendation(recommended.getAvailableSeats())
                .clicked(false)
                .booked(false)
                .build();

        return recommendationFeedbackRepository.save(feedback).getId();
    }

    @Transactional
    public void markRecommendationBooked(Long feedbackId, String userEmail, Reservation reservation) {
        if (feedbackId == null || userEmail == null || userEmail.isBlank() || reservation == null) {
            return;
        }

        recommendationFeedbackRepository.findByIdAndUserEmail(feedbackId, userEmail)
                .ifPresent(feedback -> {
                    feedback.setClicked(true);
                    feedback.setBooked(true);
                    feedback.setBooking(reservation);
                    recommendationFeedbackRepository.save(feedback);
                });
    }

    @Transactional(readOnly = true)
    public TransportAiDatasetSummaryDTO getDatasetSummaryForHost(String hostEmail) {
        return TransportAiDatasetSummaryDTO.builder()
                .hostEmail(hostEmail)
                .tripRecords(tripDatasetRepository.countByHostEmail(hostEmail))
                .trainingReadyTripRecords(tripDatasetRepository.countByHostEmailAndActualDelayMinutesIsNotNull(hostEmail))
                .realTripRecords(tripDatasetRepository.countByHostEmailAndDataOrigin(hostEmail, DATA_ORIGIN_REAL))
                .bootstrapTripRecords(tripDatasetRepository.countByHostEmailAndDataOrigin(hostEmail, DATA_ORIGIN_BOOTSTRAP_IMPORT))
                .realTrainingReadyTripRecords(tripDatasetRepository.countByHostEmailAndDataOriginAndActualDelayMinutesIsNotNull(hostEmail, DATA_ORIGIN_REAL))
                .bootstrapTrainingReadyTripRecords(tripDatasetRepository.countByHostEmailAndDataOriginAndActualDelayMinutesIsNotNull(hostEmail, DATA_ORIGIN_BOOTSTRAP_IMPORT))
                .incidentRecords(incidentDatasetRepository.countByHostEmail(hostEmail))
                .hostMetricRecords(hostDailyMetricsRepository.countByHostEmail(hostEmail))
                .recommendationRecords(recommendationFeedbackRepository.countByRecommendedTransportHostEmail(hostEmail))
                .generatedAt(LocalDateTime.now())
                .build();
    }

    @Transactional
    public TransportAiDatasetSummaryDTO syncCurrentHostDatasets(String hostEmail, HostTransportAiReportDTO report) {
        syncHostTripDatasets(hostEmail);
        upsertHostDailyMetrics(hostEmail, report);
        return getDatasetSummaryForHost(hostEmail);
    }

    @Transactional
    public void cleanupTransportDerivedData(Long transportId) {
        if (transportId == null) {
            return;
        }

        recommendationFeedbackRepository.deleteByRecommendedTransportId(transportId);
        incidentDatasetRepository.deleteByTransportId(transportId);
        tripDatasetRepository.deleteByTransportId(transportId);
    }

    @Transactional
    public void cleanupReservationReferences(Long reservationId) {
        if (reservationId == null) {
            return;
        }

        recommendationFeedbackRepository.clearBookingReferenceByReservationId(reservationId);
    }

    private boolean isWeekend(LocalDateTime departureDate) {
        if (departureDate == null) {
            return false;
        }

        DayOfWeek dayOfWeek = departureDate.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private String determineIncidentType(Transport transport, String title) {
        if (transport.getWeather() == WeatherCondition.STORM || transport.getWeather() == WeatherCondition.SANDSTORM) {
            return "WEATHER";
        }
        if (Boolean.TRUE.equals(transport.getTrafficJam())) {
            return "TRAFFIC";
        }

        String normalizedTitle = trimToEmpty(title).toLowerCase(Locale.ROOT);
        if (normalizedTitle.contains("occupancy")) {
            return "CAPACITY";
        }
        if (normalizedTitle.contains("cancel")) {
            return "CANCELLATION";
        }
        return "DELAY";
    }

    private int countHostTransportsByStatus(String hostEmail, TransportStatus status) {
        return (int) transportRepository.findByHostEmailOrderByDepartureDateDesc(hostEmail).stream()
                .filter(transport -> transport.getStatus() == status)
                .count();
    }

    private int countCancelledReservationsForHost(String hostEmail) {
        return (int) transportRepository.findByHostEmailOrderByDepartureDateDesc(hostEmail).stream()
                .flatMap(transport -> transport.getReservations().stream())
                .filter(reservation -> reservation.getStatus() == ReservationStatus.CANCELLED)
                .count();
    }

    private double calculateAverageDelay(String hostEmail) {
        List<TransportAiTripDataset> datasets = tripDatasetRepository.findByHostEmailOrderByDepartureDateDesc(hostEmail);
        if (datasets.isEmpty()) {
            return 0.0;
        }

        return datasets.stream()
                .map(TransportAiTripDataset::getActualDelayMinutes)
                .filter(value -> value != null)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }

    private Integer getSafeInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private Double getSafeDouble(Integer value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private Double roundMoney(Double value) {
        if (value == null) {
            return 0.0;
        }
        return Math.round(value * 100.0) / 100.0;
    }

    private Double roundDouble(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String trimToNull(String value) {
        String trimmed = trimToEmpty(value);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimToMaxLength(String value, int maxLength) {
        String trimmed = trimToEmpty(value);
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
