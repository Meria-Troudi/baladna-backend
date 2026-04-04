package tn.esprit.spring.baladna.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.transport.entity.Station;
import tn.esprit.spring.baladna.transport.repository.StationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StationService {

    private final StationRepository stationRepository;

    public List<Station> getAllStations() {
        return stationRepository.findAll();
    }

    public Station getStationById(Long id) {
        return stationRepository.findById(id).orElse(null);
    }

    public List<Station> getStationsByCity(String city) {
        return stationRepository.findByCityContainingIgnoreCase(city);
    }

    public List<Station> getDowntownStations() {
        return stationRepository.findByDowntownTrue();
    }

    public List<Station> searchByName(String name) {
        return stationRepository.searchByName(name);
    }

    public Station createStation(Station station) {
        if (stationRepository.existsByNameAndCity(station.getName(), station.getCity())) {
            throw new RuntimeException("Cette station existe déjà dans cette ville");
        }
        return stationRepository.save(station);
    }

    public Station updateStation(Long id, Station stationDetails) {
        Station station = getStationById(id);
        if (station != null) {
            station.setName(stationDetails.getName());
            station.setCity(stationDetails.getCity());
            station.setSurcharge(stationDetails.getSurcharge());
            station.setDowntown(stationDetails.getDowntown());
            return stationRepository.save(station);
        }
        return null;
    }

    public void deleteStation(Long id) {
        stationRepository.deleteById(id);
    }
}