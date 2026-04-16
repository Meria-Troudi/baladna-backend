package tn.esprit.spring.baladna.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.transport.dto.WeatherInfo;
import tn.esprit.spring.baladna.transport.entity.Trajet;
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

        applyWeatherStrategy(transport);

        return transportRepository.save(transport);
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
        transport.setWeather(transportDetails.getWeather());
        transport.setWeatherSource(transportDetails.getWeatherSource());
        transport.setTrajet(ownedTrajet);
        transport.setStatus(transportDetails.getStatus());

        if (transportDetails.getAvailableSeats() != null) {
            transport.setAvailableSeats(transportDetails.getAvailableSeats());
        } else if (transport.getAvailableSeats() == null) {
            transport.setAvailableSeats(transportDetails.getTotalCapacity());
        }

        applyWeatherStrategy(transport);

        return transportRepository.save(transport);
    }

    public void deleteTransport(Long id, String hostEmail) {
        Transport transport = getTransportByIdForHost(id, hostEmail);
        if (transport == null) {
            throw new RuntimeException("Transport non trouvé");
        }
        transportRepository.delete(transport);
    }

    private User requireHost(String hostEmail) {
        return userRepository.findByEmail(hostEmail)
                .orElseThrow(() -> new RuntimeException("Host introuvable"));
    }

    private Trajet requireOwnedTrajet(Long trajetId, String hostEmail) {
        return trajetRepository.findByIdAndHostEmail(trajetId, hostEmail)
                .orElseThrow(() -> new RuntimeException("Le trajet sélectionné n'appartient pas à ce host"));
    }

    private void validateTransport(Transport transport) {
        if (transport.getTrajet() == null || transport.getTrajet().getId() == null) {
            throw new RuntimeException("Le trajet est obligatoire");
        }

        if (transport.getTotalCapacity() != null && transport.getAvailableSeats() != null
                && transport.getAvailableSeats() > transport.getTotalCapacity()) {
            throw new RuntimeException("Les places disponibles ne peuvent pas dépasser la capacité totale");
        }
    }

    private void applyWeatherStrategy(Transport transport) {
        String weatherSource = transport.getWeatherSource();
        boolean autoWeather = weatherSource == null || weatherSource.isBlank() || "AUTO".equalsIgnoreCase(weatherSource);

        if (autoWeather) {
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
            return;
        }

        if (transport.getWeather() == null) {
            transport.setWeather(WeatherCondition.SUNNY);
        }

        transport.setWeatherSource("MANUAL");
    }
}