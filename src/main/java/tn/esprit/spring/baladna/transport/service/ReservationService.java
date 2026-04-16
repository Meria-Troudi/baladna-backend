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
        if (user == null) return List.of();
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

        Transport transport = transportRepository.findById(transportId).orElse(null);
        User user = userRepository.findByEmail(userEmail).orElse(null);

        if (transport == null) {
            throw new RuntimeException("Transport non trouvé");
        }

        if (user == null) {
            throw new RuntimeException("Utilisateur non trouvé");
        }

        if (transport.getDepartureDate() == null || !transport.getDepartureDate().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("Impossible de réserver un transport déjà passé ou en cours");
        }

        if (transport.getStatus() == TransportStatus.CANCELLED) {
            throw new RuntimeException("Ce transport est annulé");
        }

        if (transport.getAvailableSeats() == null || transport.getAvailableSeats() < seatsCount) {
            throw new RuntimeException("Pas assez de places disponibles");
        }

        if (!transport.checkWeatherConditions()) {
            transport.setStatus(TransportStatus.CANCELLED);
            transportRepository.save(transport);
            throw new RuntimeException("Départ annulé à cause de la météo");
        }

        int lastSeatsCount = transport.getAvailableSeats();
        double pricePerSeat = transport.calculatePrice(boardingPoint, lastSeatsCount);
        double totalPrice = pricePerSeat * seatsCount;

        Reservation reservation = Reservation.builder()
                .reservedSeats(seatsCount)
                .totalPrice(totalPrice)
                .reservationDate(LocalDateTime.now())
                .boardingPoint(boardingPoint)
                .status(ReservationStatus.CONFIRMED)
                .transport(transport)
                .user(user)
                .build();

        transport.setAvailableSeats(transport.getAvailableSeats() - seatsCount);
        transportRepository.save(transport);

        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation cancelReservation(Long id, String userEmail) {
        Reservation reservation = getReservationById(id);

        if (reservation == null) {
            throw new RuntimeException("Réservation non trouvée");
        }

        if (!reservation.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Vous ne pouvez annuler que vos propres réservations");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new RuntimeException("Cette réservation est déjà annulée");
        }

        reservation.cancel();

        Transport transport = reservation.getTransport();
        transport.setAvailableSeats(transport.getAvailableSeats() + reservation.getReservedSeats());
        transportRepository.save(transport);

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
            transport.setAvailableSeats(transport.getAvailableSeats() + reservation.getReservedSeats());
            transportRepository.save(transport);
        }

        reservationRepository.delete(reservation);
    }
}
