package tn.esprit.spring.baladna.transport.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.baladna.transport.entity.Reservation;
import tn.esprit.spring.baladna.transport.entity.ReservationStatus;
import tn.esprit.spring.baladna.transport.entity.Transport;
import tn.esprit.spring.baladna.transport.entity.TransportStatus;
import tn.esprit.spring.baladna.transport.repository.ReservationRepository;
import tn.esprit.spring.baladna.transport.repository.TransportRepository;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final TransportRepository transportRepository;
    private final UserRepository userRepository;

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    public List<Reservation> getReservationsForHost(String hostEmail) {
        return reservationRepository.findByTransportHostEmail(hostEmail);
    }

    public Reservation getReservationById(Long id) {
        return reservationRepository.findById(id).orElse(null);
    }

    public Reservation getReservationByIdForHost(Long id, String hostEmail) {
        return reservationRepository.findByIdAndTransportHostEmail(id, hostEmail).orElse(null);
    }

    public List<Reservation> getReservationsByUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return List.of();
        }
        return reservationRepository.findByUser(user);
    }

    public List<Reservation> getReservationsByUserEmail(String email) {
        return reservationRepository.findByUserEmail(email);
    }

    public List<Reservation> getReservationsByTransport(Long transportId) {
        return reservationRepository.findByTransportId(transportId);
    }

    public List<Reservation> getReservationsByTransportForHost(Long transportId, String hostEmail) {
        return reservationRepository.findByTransportIdAndTransportHostEmail(transportId, hostEmail);
    }

    @Transactional
    public Reservation makeReservation(Long transportId, String userEmail, String boardingPoint, Integer seatsCount) {
        Transport transport = transportRepository.findById(transportId)
                .orElseThrow(() -> new RuntimeException("Transport non trouvé"));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        if (boardingPoint == null || boardingPoint.isBlank()) {
            throw new RuntimeException("Le point d'embarquement est obligatoire");
        }

        if (seatsCount == null || seatsCount <= 0) {
            throw new RuntimeException("Le nombre de places doit être supérieur à 0");
        }

        if (transport.getDepartureDate() == null || !transport.getDepartureDate().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Impossible de réserver un transport déjà passé ou en cours");
        }

        if (transport.getStatus() == TransportStatus.CANCELLED) {
            throw new RuntimeException("Ce transport est annulé");
        }

        if (transport.getStatus() == TransportStatus.COMPLETED) {
            throw new RuntimeException("Ce transport est déjà terminé");
        }

        Integer availableSeats = transport.getAvailableSeats();
        if (availableSeats == null) {
            availableSeats = transport.getTotalCapacity();
            transport.setAvailableSeats(availableSeats);
        }

        if (availableSeats < seatsCount) {
            throw new RuntimeException("Pas assez de places disponibles. Places restantes : " + availableSeats);
        }

        if (!transport.checkWeatherConditions()) {
            transport.setStatus(TransportStatus.CANCELLED);
            transportRepository.save(transport);
            throw new RuntimeException("Départ annulé à cause de la météo");
        }

        int lastSeatsCount = availableSeats;
        double pricePerSeat = transport.calculatePrice(boardingPoint, lastSeatsCount);
        double totalPrice = Math.round(pricePerSeat * seatsCount * 100.0) / 100.0;

        Reservation reservation = Reservation.builder()
                .reservedSeats(seatsCount)
                .totalPrice(totalPrice)
                .reservationDate(LocalDateTime.now())
                .boardingPoint(boardingPoint)
                .status(ReservationStatus.CONFIRMED)
                .transport(transport)
                .user(user)
                .build();

        transport.setAvailableSeats(availableSeats - seatsCount);
        refreshTransportStatusAfterSeatChange(transport);
        transportRepository.save(transport);

        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation cancelReservation(Long id, String userEmail) {
        Reservation reservation = getReservationById(id);

        if (reservation == null) {
            throw new RuntimeException("Réservation non trouvée");
        }

        if (reservation.getUser() == null || !reservation.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Vous ne pouvez annuler que vos propres réservations");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new RuntimeException("Cette réservation est déjà annulée");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        Transport transport = reservation.getTransport();
        if (transport != null) {
            int currentAvailable = transport.getAvailableSeats() != null ? transport.getAvailableSeats() : 0;
            int reservedSeats = reservation.getReservedSeats() != null ? reservation.getReservedSeats() : 0;
            int totalCapacity = transport.getTotalCapacity() != null ? transport.getTotalCapacity() : currentAvailable + reservedSeats;

            transport.setAvailableSeats(Math.min(totalCapacity, currentAvailable + reservedSeats));
            refreshTransportStatusAfterSeatChange(transport);
            transportRepository.save(transport);
        }

        return reservationRepository.save(reservation);
    }

    @Transactional
    public void deleteReservation(Long id, String hostEmail) {
        Reservation reservation = getReservationByIdForHost(id, hostEmail);

        if (reservation == null) {
            throw new RuntimeException("Réservation non trouvée");
        }

        if (reservation.getStatus() != ReservationStatus.CANCELLED) {
            Transport transport = reservation.getTransport();
            if (transport != null) {
                int currentAvailable = transport.getAvailableSeats() != null ? transport.getAvailableSeats() : 0;
                int reservedSeats = reservation.getReservedSeats() != null ? reservation.getReservedSeats() : 0;
                int totalCapacity = transport.getTotalCapacity() != null ? transport.getTotalCapacity() : currentAvailable + reservedSeats;

                transport.setAvailableSeats(Math.min(totalCapacity, currentAvailable + reservedSeats));
                refreshTransportStatusAfterSeatChange(transport);
                transportRepository.save(transport);
            }
        }

        reservationRepository.delete(reservation);
    }

    private void refreshTransportStatusAfterSeatChange(Transport transport) {
        if (transport == null || transport.getStatus() == TransportStatus.CANCELLED || transport.getStatus() == TransportStatus.COMPLETED) {
            return;
        }

        Integer availableSeats = transport.getAvailableSeats();
        if (availableSeats != null && availableSeats <= 0) {
            // Si ton enum n'a pas FULL, on laisse SCHEDULED
            transport.setStatus(TransportStatus.SCHEDULED);
            return;
        }

        if (transport.getDepartureDate() != null && transport.getDepartureDate().isAfter(LocalDateTime.now())) {
            transport.setStatus(TransportStatus.SCHEDULED);
        }
    }
}