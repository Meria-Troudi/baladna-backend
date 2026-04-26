package tn.esprit.spring.baladna.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.transport.dto.WeatherInfo;
import tn.esprit.spring.baladna.transport.dto.WeatherPreviewDTO;
import tn.esprit.spring.baladna.transport.entity.Trajet;
import tn.esprit.spring.baladna.transport.entity.TrafficCongestionLevel;
import tn.esprit.spring.baladna.transport.entity.Transport;
import tn.esprit.spring.baladna.transport.entity.TransportStatus;
import tn.esprit.spring.baladna.transport.entity.WeatherCondition;
import tn.esprit.spring.baladna.transport.repository.TrajetRepository;
import tn.esprit.spring.baladna.transport.repository.TransportRepository;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransportService {

    private final TransportRepository transportRepository;
    private final TrajetRepository trajetRepository;
    private final WeatherService weatherService;
    private final UserRepository userRepository;
    private final TransportAiDatasetService transportAiDatasetService;
    private final TransportAiDelayModelService transportAiDelayModelService;
    private final TransportAiService transportAiService;

    public List<Transport> getAllTransports() {
        return transportRepository.findAll();
    }

    public List<Transport> getTransportsForHost(String hostEmail) {
        return transportRepository.findByHostEmailOrderByDepartureDateDesc(hostEmail);
    }

    public Transport getTransportById(Long id) {
        return transportRepository.findById(id).orElse(null);
    }

    public Transport getTransportByIdForHost(Long id, String hostEmail) {
        return transportRepository.findByIdAndHostEmail(id, hostEmail).orElse(null);
    }

    public List<Transport> getTransportsByTrajet(Long trajetId) {
        return transportRepository.findByTrajetId(trajetId);
    }

    public List<Transport> getTransportsByTrajetForHost(Long trajetId, String hostEmail) {
        return transportRepository.findByTrajetIdAndHostEmail(trajetId, hostEmail);
    }

    public List<Transport> getAvailableTransports() {
        return transportRepository.findAvailableTransports(LocalDateTime.now());
    }

    public List<Transport> getAvailableTransportsForHost(String hostEmail) {
        return transportRepository.findAvailableTransportsByHost(hostEmail, LocalDateTime.now());
    }

    public List<Transport> getTransportsByCities(String departureCity, String arrivalCity) {
        return transportRepository.findTransportsByCities(departureCity, arrivalCity);
    }

    public List<Transport> getTransportsByCitiesForHost(String departureCity, String arrivalCity, String hostEmail) {
        return transportRepository.findTransportsByCitiesAndHost(departureCity, arrivalCity, hostEmail);
    }

    public Transport createTransport(Transport transport, String hostEmail) {
        validateTransport(transport);

        User host = requireHost(hostEmail);
        Trajet ownedTrajet = requireOwnedTrajet(transport.getTrajet().getId(), hostEmail);

        transport.setTrajet(ownedTrajet);
        transport.setHost(host);

        if (transport.getAvailableSeats() == null) {
            transport.setAvailableSeats(transport.getTotalCapacity());
        }

        if (transport.getStatus() == null) {
            transport.setStatus(TransportStatus.SCHEDULED);
        }

        if (transport.getTrafficJam() == null) {
            transport.setTrafficJam(false);
        }
        transport.setTrafficCongestionLevel(resolveTrafficCongestionLevel(
                transport.getTrafficJam(),
                transport.getTrafficCongestionLevel()
        ));
        transport.setTrafficJam(transport.getTrafficCongestionLevel() != TrafficCongestionLevel.NONE);

        applyWeatherStrategy(transport);
        transport.setActualDelayMinutes(resolveActualDelayMinutes(transport));

        Transport saved = transportRepository.save(transport);
        transportAiDatasetService.syncTransportTripDataset(saved);
        maybeRefreshHostDelayModel(saved);
        return saved;
    }

    public Transport updateTransport(Long id, Transport transportDetails, String hostEmail) {
        Transport transport = getTransportByIdForHost(id, hostEmail);

        if (transport == null) {
            return null;
        }

        validateTransport(transportDetails);

        Trajet ownedTrajet = requireOwnedTrajet(transportDetails.getTrajet().getId(), hostEmail);

        transport.setDeparturePoint(transportDetails.getDeparturePoint());
        transport.setDepartureDate(transportDetails.getDepartureDate());
        transport.setTotalCapacity(transportDetails.getTotalCapacity());
        transport.setBasePrice(transportDetails.getBasePrice());
        transport.setTrafficJam(transportDetails.getTrafficJam());
        transport.setTrafficCongestionLevel(resolveTrafficCongestionLevel(
                transportDetails.getTrafficJam(),
                transportDetails.getTrafficCongestionLevel()
        ));
        transport.setTrafficJam(transport.getTrafficCongestionLevel() != TrafficCongestionLevel.NONE);
        transport.setTrajet(ownedTrajet);
        transport.setStatus(transportDetails.getStatus());

        if (transportDetails.getAvailableSeats() != null) {
            if (transportDetails.getAvailableSeats() > transportDetails.getTotalCapacity()) {
                throw new RuntimeException("Les places disponibles ne peuvent pas depasser la capacite totale");
            }
            transport.setAvailableSeats(transportDetails.getAvailableSeats());
        } else if (transport.getAvailableSeats() == null) {
            transport.setAvailableSeats(transportDetails.getTotalCapacity());
        }

        applyWeatherStrategy(transport);

        transport.setActualDelayMinutes(resolveActualDelayMinutes(transportDetails));

        Transport saved = transportRepository.save(transport);
        transportAiDatasetService.syncTransportTripDataset(saved);
        maybeRefreshHostDelayModel(saved);
        return saved;
    }

    public void deleteTransport(Long id, String hostEmail) {
        Transport transport = getTransportByIdForHost(id, hostEmail);
        if (transport == null) {
            throw new RuntimeException("Transport non trouve");
        }
        transportAiDatasetService.cleanupTransportDerivedData(transport.getId());
        transportRepository.delete(transport);
    }

    public WeatherPreviewDTO previewWeather(Long trajetId, LocalDateTime departureDate, TrafficCongestionLevel trafficCongestionLevel, String hostEmail) {
        if (trajetId == null) {
            throw new RuntimeException("Le trajet est obligatoire");
        }

        if (departureDate == null) {
            throw new RuntimeException("La date de depart est obligatoire");
        }

        Trajet trajet = requireOwnedTrajet(trajetId, hostEmail);

        WeatherInfo weatherInfo = weatherService.getWeatherForDeparture(
                trajet.getDepartureStation(),
                departureDate
        );

        WeatherCondition condition = weatherInfo.getCondition() != null
                ? weatherInfo.getCondition()
                : WeatherCondition.SUNNY;
        TrafficCongestionLevel effectiveTrafficLevel = trafficCongestionLevel != null
                ? trafficCongestionLevel
                : TrafficCongestionLevel.NONE;
        Transport previewTransport = Transport.builder()
                .departurePoint(
                        trajet.getDepartureStation() != null && trajet.getDepartureStation().getName() != null
                                ? trajet.getDepartureStation().getName()
                                : "Preview departure"
                )
                .departureDate(departureDate)
                .totalCapacity(10)
                .availableSeats(10)
                .status(TransportStatus.SCHEDULED)
                .basePrice(1.0)
                .trafficJam(effectiveTrafficLevel != TrafficCongestionLevel.NONE)
                .trafficCongestionLevel(effectiveTrafficLevel)
                .weather(condition)
                .weatherSource("AUTO")
                .weatherTemperature(weatherInfo.getTemperature())
                .weatherWindSpeed(weatherInfo.getWindSpeed())
                .weatherPrecipitation(weatherInfo.getPrecipitation())
                .trajet(trajet)
                .build();
        var delayPrediction = transportAiService.predictDelay(previewTransport);

        return WeatherPreviewDTO.builder()
                .weather(condition)
                .weatherSource("AUTO")
                .routingSource(resolveRoutingSource(trajet))
                .weatherTemperature(weatherInfo.getTemperature())
                .weatherWindSpeed(weatherInfo.getWindSpeed())
                .weatherPrecipitation(weatherInfo.getPrecipitation())
                .routeDistanceKm(trajet.getDistanceKm())
                .estimatedDurationMinutes(trajet.getEstimatedDurationMinutes())
                .trafficCongestionLevel(effectiveTrafficLevel)
                .weatherDelayMinutes(buildWeatherDelayMinutes(condition))
                .trafficDelayMinutes(previewTransport.getTrafficDelayMinutes())
                .delayMinutes(delayPrediction.getPredictedDelayMinutes() != null ? delayPrediction.getPredictedDelayMinutes() : 0)
                .confidencePercent(delayPrediction.getConfidencePercent())
                .primaryReason(delayPrediction.getPrimaryReason())
                .build();
    }

    private User requireHost(String hostEmail) {
        return userRepository.findByEmail(hostEmail)
                .orElseThrow(() -> new RuntimeException("Host introuvable"));
    }

    private Trajet requireOwnedTrajet(Long trajetId, String hostEmail) {
        return trajetRepository.findByIdAndHostEmail(trajetId, hostEmail)
                .orElseThrow(() -> new RuntimeException("Le trajet selectionne n'appartient pas a ce host"));
    }

    private void validateTransport(Transport transport) {
        if (transport.getDepartureDate() == null) {
            throw new RuntimeException("La date de depart est obligatoire");
        }

        if (transport.getTrajet() == null || transport.getTrajet().getId() == null) {
            throw new RuntimeException("Le trajet est obligatoire");
        }

        if (transport.getTotalCapacity() != null && transport.getAvailableSeats() != null
                && transport.getAvailableSeats() > transport.getTotalCapacity()) {
            throw new RuntimeException("Les places disponibles ne peuvent pas depasser la capacite totale");
        }

        if (transport.getActualDelayMinutes() != null && transport.getActualDelayMinutes() < 0) {
            throw new RuntimeException("Le retard reel ne peut pas etre negatif");
        }

        TransportStatus effectiveStatus = transport.getStatus() != null
                ? transport.getStatus()
                : TransportStatus.SCHEDULED;

        if ((effectiveStatus == TransportStatus.SCHEDULED || effectiveStatus == TransportStatus.IN_PROGRESS)
                && !transport.getDepartureDate().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("La date de depart doit etre dans le futur pour un transport planifie ou en cours.");
        }

        if (effectiveStatus == TransportStatus.COMPLETED && transport.getDepartureDate().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Un transport complete ne peut pas avoir une date de depart dans le futur.");
        }

        if (effectiveStatus == TransportStatus.COMPLETED && transport.getActualDelayMinutes() == null) {
            throw new RuntimeException("Le retard reel est obligatoire pour un transport complete.");
        }
    }

    private Integer resolveActualDelayMinutes(Transport transport) {
        if (transport == null || transport.getStatus() != TransportStatus.COMPLETED) {
            return null;
        }
        return transport.getActualDelayMinutes();
    }

    private void applyWeatherStrategy(Transport transport) {
        WeatherInfo weatherInfo = weatherService.getWeatherForDeparture(
                transport.getTrajet().getDepartureStation(),
                transport.getDepartureDate()
        );

        transport.setWeather(
                weatherInfo.getCondition() != null
                        ? weatherInfo.getCondition()
                        : WeatherCondition.SUNNY
        );
        transport.setWeatherTemperature(weatherInfo.getTemperature());
        transport.setWeatherWindSpeed(weatherInfo.getWindSpeed());
        transport.setWeatherPrecipitation(weatherInfo.getPrecipitation());
        transport.setWeatherSource("AUTO");
    }

    private TrafficCongestionLevel resolveTrafficCongestionLevel(Boolean trafficJam, TrafficCongestionLevel trafficCongestionLevel) {
        if (trafficCongestionLevel != null) {
            return trafficCongestionLevel;
        }
        return Boolean.TRUE.equals(trafficJam) ? TrafficCongestionLevel.MEDIUM : TrafficCongestionLevel.NONE;
    }

    private Integer buildWeatherDelayMinutes(WeatherCondition weatherCondition) {
        if (weatherCondition == null) {
            return 0;
        }

        return switch (weatherCondition) {
            case RAIN -> 25;
            case SANDSTORM -> 30;
            case STORM -> 40;
            default -> 0;
        };
    }

    private String resolveRoutingSource(Trajet trajet) {
        if (trajet == null) {
            return "ROUTE_BASELINE";
        }

        if (trajet.getRouteGeoJson() != null && !trajet.getRouteGeoJson().isBlank()) {
            return "OSRM";
        }

        return "ROUTE_BASELINE";
    }


    private void maybeRefreshHostDelayModel(Transport transport) {
        if (transport == null
                || transport.getHost() == null
                || transport.getHost().getEmail() == null) {
            return;
        }

        transportAiDelayModelService.maybeRefreshModelForHost(transport.getHost().getEmail());
    }
}
