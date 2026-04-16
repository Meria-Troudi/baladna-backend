package tn.esprit.spring.baladna.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.transport.entity.Station;
import tn.esprit.spring.baladna.transport.repository.StationRepository;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StationService {

    private static final double TUNISIA_MIN_LATITUDE = 30.0;
    private static final double TUNISIA_MAX_LATITUDE = 37.6;
    private static final double TUNISIA_MIN_LONGITUDE = 7.0;
    private static final double TUNISIA_MAX_LONGITUDE = 11.8;

    private final StationRepository stationRepository;
    private final TrajetService trajetService;
    private final UserRepository userRepository;

    public List<Station> getAllStations() {
        return stationRepository.findAll();
    }

    public List<Station> getStationsForHost(String hostEmail) {
        return stationRepository.findByHostEmailOrderByCityAscNameAsc(hostEmail);
    }

    public Station getStationById(Long id) {
        return stationRepository.findById(id).orElse(null);
    }

    public Station getStationByIdForHost(Long id, String hostEmail) {
        return stationRepository.findByIdAndHostEmail(id, hostEmail).orElse(null);
    }

    public List<Station> getStationsByCity(String city) {
        return stationRepository.findByCityContainingIgnoreCase(city);
    }

    public List<Station> getDowntownStations() {
        return stationRepository.findByDowntownTrue();
    }

    public List<Station> searchByName(String name) {
        return stationRepository.searchByNameOrCity(name);
    }

    public Station createStation(Station station, String hostEmail) {
        validateTunisiaCoordinates(station);
        if (stationRepository.existsByNameAndCityAndHostEmail(station.getName(), station.getCity(), hostEmail)) {
            throw new RuntimeException("Cette station existe déjà dans cette ville pour ce host");
        }

        station.setHost(requireHost(hostEmail));
        return stationRepository.save(station);
    }

    public Station updateStation(Long id, Station stationDetails, String hostEmail) {
        validateTunisiaCoordinates(stationDetails);
        Station station = getStationByIdForHost(id, hostEmail);
        if (station != null) {
            if (stationRepository.existsByNameAndCityAndIdNotAndHostEmail(stationDetails.getName(), stationDetails.getCity(), id, hostEmail)) {
                throw new RuntimeException("Cette station existe déjà dans cette ville pour ce host");
            }
            station.setName(stationDetails.getName());
            station.setCity(stationDetails.getCity());
            station.setSurcharge(stationDetails.getSurcharge());
            station.setDowntown(stationDetails.getDowntown());
            station.setLatitude(stationDetails.getLatitude());
            station.setLongitude(stationDetails.getLongitude());
            Station savedStation = stationRepository.save(station);
            trajetService.refreshTrajetsForStation(savedStation);
            return savedStation;
        }
        return null;
    }

    public void deleteStation(Long id, String hostEmail) {
        Station station = getStationByIdForHost(id, hostEmail);
        if (station == null) {
            throw new RuntimeException("Station non trouvée");
        }
        stationRepository.delete(station);
    }

    private User requireHost(String hostEmail) {
        return userRepository.findByEmail(hostEmail)
                .orElseThrow(() -> new RuntimeException("Host introuvable"));
    }

    private void validateTunisiaCoordinates(Station station) {
        if (station == null || station.getLatitude() == null || station.getLongitude() == null) {
            throw new RuntimeException("Les coordonnées de la station sont obligatoires");
        }

        double latitude = station.getLatitude();
        double longitude = station.getLongitude();
        boolean insideTunisia = latitude >= TUNISIA_MIN_LATITUDE
                && latitude <= TUNISIA_MAX_LATITUDE
                && longitude >= TUNISIA_MIN_LONGITUDE
                && longitude <= TUNISIA_MAX_LONGITUDE;

        if (!insideTunisia) {
            throw new RuntimeException("Les stations doivent rester dans le territoire tunisien");
        }
    }
}
