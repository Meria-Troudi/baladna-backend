package tn.esprit.spring.baladna.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.transport.entity.Transport;
import tn.esprit.spring.baladna.transport.entity.TransportStatus;
import tn.esprit.spring.baladna.transport.repository.TransportRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransportService {

    private final TransportRepository transportRepository;

    public List<Transport> getAllTransports() {
        return transportRepository.findAll();
    }

    public Transport getTransportById(Long id) {
        return transportRepository.findById(id).orElse(null);
    }

    public List<Transport> getTransportsByTrajet(Long trajetId) {
        return transportRepository.findByTrajetId(trajetId);
    }

    public List<Transport> getAvailableTransports() {
        return transportRepository.findAvailableTransports(LocalDateTime.now());
    }

    public List<Transport> getTransportsByCities(String departureCity, String arrivalCity) {
        return transportRepository.findTransportsByCities(departureCity, arrivalCity);
    }

    public Transport createTransport(Transport transport) {
        validateTransport(transport);

        if (transport.getAvailableSeats() == null) {
            transport.setAvailableSeats(transport.getTotalCapacity());
        }

        if (transport.getStatus() == null) {
            transport.setStatus(TransportStatus.SCHEDULED);
        }

        if (transport.getTrafficJam() == null) {
            transport.setTrafficJam(false);
        }

        return transportRepository.save(transport);
    }

    public Transport updateTransport(Long id, Transport transportDetails) {
        Transport transport = getTransportById(id);

        if (transport == null) {
            return null;
        }

        validateTransport(transportDetails);

        transport.setDeparturePoint(transportDetails.getDeparturePoint());
        transport.setDepartureDate(transportDetails.getDepartureDate());
        transport.setTotalCapacity(transportDetails.getTotalCapacity());
        transport.setBasePrice(transportDetails.getBasePrice());
        transport.setTrafficJam(transportDetails.getTrafficJam());
        transport.setWeather(transportDetails.getWeather());
        transport.setTrajet(transportDetails.getTrajet());
        transport.setStatus(transportDetails.getStatus());

        if (transportDetails.getAvailableSeats() != null) {
            transport.setAvailableSeats(transportDetails.getAvailableSeats());
        } else if (transport.getAvailableSeats() == null) {
            transport.setAvailableSeats(transportDetails.getTotalCapacity());
        }

        return transportRepository.save(transport);
    }

    public void deleteTransport(Long id) {
        transportRepository.deleteById(id);
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
}