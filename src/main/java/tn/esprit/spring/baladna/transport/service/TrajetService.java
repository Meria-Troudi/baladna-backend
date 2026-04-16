package tn.esprit.spring.baladna.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.transport.dto.RouteInfo;
import tn.esprit.spring.baladna.transport.entity.Station;
import tn.esprit.spring.baladna.transport.entity.Trajet;
import tn.esprit.spring.baladna.transport.repository.StationRepository;
import tn.esprit.spring.baladna.transport.repository.TrajetRepository;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrajetService {

    private final TrajetRepository trajetRepository;
    private final StationRepository stationRepository;
    private final RoutingService routingService;
    private final UserRepository userRepository;

    public List<Trajet> getAllTrajets() {
        return trajetRepository.findAll();
    }

    public List<Trajet> getTrajetsForHost(String hostEmail) {
        return trajetRepository.findByHostEmailOrderByIdDesc(hostEmail);
    }

    public Trajet getTrajetById(Long id) {
        return trajetRepository.findById(id).orElse(null);
    }

    public Trajet getTrajetByIdForHost(Long id, String hostEmail) {
        return trajetRepository.findByIdAndHostEmail(id, hostEmail).orElse(null);
    }

    public List<Trajet> getTrajetsByDepartureStation(Long stationId) {
        Station station = stationRepository.findById(stationId).orElse(null);
        if (station == null) return List.of();
        return trajetRepository.findByDepartureStation(station);
    }

    public List<Trajet> getTrajetsByDepartureStationForHost(Long stationId, String hostEmail) {
        Station station = stationRepository.findByIdAndHostEmail(stationId, hostEmail).orElse(null);
        if (station == null) return List.of();
        return trajetRepository.findByHostEmailAndDepartureStation(hostEmail, station);
    }

    public List<Trajet> getTrajetsByArrivalStation(Long stationId) {
        Station station = stationRepository.findById(stationId).orElse(null);
        if (station == null) return List.of();
        return trajetRepository.findByArrivalStation(station);
    }

    public List<Trajet> getTrajetsByArrivalStationForHost(Long stationId, String hostEmail) {
        Station station = stationRepository.findByIdAndHostEmail(stationId, hostEmail).orElse(null);
        if (station == null) return List.of();
        return trajetRepository.findByHostEmailAndArrivalStation(hostEmail, station);
    }

    public List<Trajet> getTrajetsBetweenStations(Long departureId, Long arrivalId) {
        Station departure = stationRepository.findById(departureId).orElse(null);
        Station arrival = stationRepository.findById(arrivalId).orElse(null);

        if (departure == null || arrival == null) return List.of();

        return trajetRepository.findByDepartureStationAndArrivalStation(departure, arrival);
    }

    public List<Trajet> getTrajetsBetweenStationsForHost(Long departureId, Long arrivalId, String hostEmail) {
        Station departure = stationRepository.findByIdAndHostEmail(departureId, hostEmail).orElse(null);
        Station arrival = stationRepository.findByIdAndHostEmail(arrivalId, hostEmail).orElse(null);

        if (departure == null || arrival == null) return List.of();

        return trajetRepository.findByHostEmailAndDepartureStationAndArrivalStation(hostEmail, departure, arrival);
    }

    public Trajet createTrajet(Trajet trajet, String hostEmail) {
        validateTrajet(trajet);
        User host = requireHost(hostEmail);
        Station departure = requireOwnedStation(trajet.getDepartureStation().getId(), hostEmail);
        Station arrival = requireOwnedStation(trajet.getArrivalStation().getId(), hostEmail);

        trajet.setDepartureStation(departure);
        trajet.setArrivalStation(arrival);
        trajet.setHost(host);
        enrichRouteData(trajet);
        return trajetRepository.save(trajet);
    }

    public Trajet updateTrajet(Long id, Trajet trajetDetails, String hostEmail) {
        Trajet trajet = getTrajetByIdForHost(id, hostEmail);

        if (trajet == null) return null;

        validateTrajet(trajetDetails);

        Station departure = requireOwnedStation(trajetDetails.getDepartureStation().getId(), hostEmail);
        Station arrival = requireOwnedStation(trajetDetails.getArrivalStation().getId(), hostEmail);

        trajet.setDepartureStation(departure);
        trajet.setArrivalStation(arrival);
        trajet.setDistanceKm(trajetDetails.getDistanceKm());
        trajet.setEstimatedDurationMinutes(trajetDetails.getEstimatedDurationMinutes());
        trajet.setPricePerKm(trajetDetails.getPricePerKm());
        trajet.setRouteGeoJson(trajetDetails.getRouteGeoJson());

        enrichRouteData(trajet);

        return trajetRepository.save(trajet);
    }

    public void deleteTrajet(Long id, String hostEmail) {
        Trajet trajet = getTrajetByIdForHost(id, hostEmail);
        if (trajet == null) {
            throw new RuntimeException("Trajet non trouvé");
        }
        trajetRepository.delete(trajet);
    }

    public RouteInfo previewRoute(Long departureStationId, Long arrivalStationId) {
        Station departureStation = stationRepository.findById(departureStationId).orElse(null);
        Station arrivalStation = stationRepository.findById(arrivalStationId).orElse(null);

        if (departureStation == null || arrivalStation == null) {
            throw new RuntimeException("Les stations de départ et d'arrivée sont introuvables");
        }

        if (departureStation.getId().equals(arrivalStation.getId())) {
            throw new RuntimeException("La station de départ doit être différente de la station d'arrivée");
        }

        return routingService.getRouteInfo(departureStation, arrivalStation);
    }

    public void refreshTrajetsForStation(Station station) {
        if (station == null || station.getId() == null) {
            return;
        }

        List<Trajet> linkedTrajets = trajetRepository.findLinkedToStation(station);
        if (linkedTrajets.isEmpty()) {
            return;
        }

        linkedTrajets.forEach(this::refreshPersistedRouteData);
        trajetRepository.saveAll(linkedTrajets);
    }

    private void validateTrajet(Trajet trajet) {
        if (trajet.getDepartureStation() == null || trajet.getArrivalStation() == null) {
            throw new RuntimeException("Les stations de départ et d'arrivée sont obligatoires");
        }

        if (trajet.getDepartureStation().getId() == null || trajet.getArrivalStation().getId() == null) {
            throw new RuntimeException("Les IDs des stations sont obligatoires");
        }

        if (trajet.getDepartureStation().getId().equals(trajet.getArrivalStation().getId())) {
            throw new RuntimeException("La station de départ doit être différente de la station d'arrivée");
        }
    }

    private Station requireOwnedStation(Long stationId, String hostEmail) {
        return stationRepository.findByIdAndHostEmail(stationId, hostEmail)
                .orElseThrow(() -> new RuntimeException("La station sélectionnée n'appartient pas à ce host"));
    }

    private User requireHost(String hostEmail) {
        return userRepository.findByEmail(hostEmail)
                .orElseThrow(() -> new RuntimeException("Host introuvable"));
    }

    private void enrichRouteData(Trajet trajet) {
        RouteInfo routeInfo = routingService.getRouteInfo(
                trajet.getDepartureStation(),
                trajet.getArrivalStation()
        );

        if (routeInfo == null) {
            return;
        }

        if (routeInfo.getDistanceKm() != null) {
            trajet.setDistanceKm(routeInfo.getDistanceKm());
        }

        if (routeInfo.getDurationMinutes() != null) {
            trajet.setEstimatedDurationMinutes(routeInfo.getDurationMinutes());
        }

        if (routeInfo.getRouteGeoJson() != null && !routeInfo.getRouteGeoJson().isBlank()) {
            trajet.setRouteGeoJson(routeInfo.getRouteGeoJson());
        }
    }

    private void refreshPersistedRouteData(Trajet trajet) {
        RouteInfo routeInfo = routingService.getRouteInfo(
                trajet.getDepartureStation(),
                trajet.getArrivalStation()
        );

        if (routeInfo == null) {
            trajet.setRouteGeoJson(null);
            return;
        }

        if (routeInfo.getDistanceKm() != null) {
            trajet.setDistanceKm(routeInfo.getDistanceKm());
        }

        if (routeInfo.getDurationMinutes() != null) {
            trajet.setEstimatedDurationMinutes(routeInfo.getDurationMinutes());
        }

        if (routeInfo.getRouteGeoJson() != null && !routeInfo.getRouteGeoJson().isBlank()) {
            trajet.setRouteGeoJson(routeInfo.getRouteGeoJson());
        } else {
            trajet.setRouteGeoJson(null);
        }
    }
}
