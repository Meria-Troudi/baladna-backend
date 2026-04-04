package tn.esprit.spring.baladna.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.transport.entity.Station;
import tn.esprit.spring.baladna.transport.entity.Trajet;
import tn.esprit.spring.baladna.transport.repository.StationRepository;
import tn.esprit.spring.baladna.transport.repository.TrajetRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrajetService {

    private final TrajetRepository trajetRepository;
    private final StationRepository stationRepository;

    public List<Trajet> getAllTrajets() {
        return trajetRepository.findAll();
    }

    public Trajet getTrajetById(Long id) {
        return trajetRepository.findById(id).orElse(null);
    }

    public List<Trajet> getTrajetsByDepartureStation(Long stationId) {
        Station station = stationRepository.findById(stationId).orElse(null);
        if (station == null) return List.of();
        return trajetRepository.findByDepartureStation(station);
    }

    public List<Trajet> getTrajetsByArrivalStation(Long stationId) {
        Station station = stationRepository.findById(stationId).orElse(null);
        if (station == null) return List.of();
        return trajetRepository.findByArrivalStation(station);
    }

    public List<Trajet> getTrajetsBetweenStations(Long departureId, Long arrivalId) {
        Station departure = stationRepository.findById(departureId).orElse(null);
        Station arrival = stationRepository.findById(arrivalId).orElse(null);

        if (departure == null || arrival == null) return List.of();

        return trajetRepository.findByDepartureStationAndArrivalStation(departure, arrival);
    }

    public Trajet createTrajet(Trajet trajet) {
        validateTrajet(trajet);
        return trajetRepository.save(trajet);
    }

    public Trajet updateTrajet(Long id, Trajet trajetDetails) {
        Trajet trajet = getTrajetById(id);

        if (trajet == null) return null;

        validateTrajet(trajetDetails);

        trajet.setDepartureStation(trajetDetails.getDepartureStation());
        trajet.setArrivalStation(trajetDetails.getArrivalStation());
        trajet.setDistanceKm(trajetDetails.getDistanceKm());
        trajet.setEstimatedDurationMinutes(trajetDetails.getEstimatedDurationMinutes());
        trajet.setPricePerKm(trajetDetails.getPricePerKm());

        return trajetRepository.save(trajet);
    }

    public void deleteTrajet(Long id) {
        trajetRepository.deleteById(id);
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
}