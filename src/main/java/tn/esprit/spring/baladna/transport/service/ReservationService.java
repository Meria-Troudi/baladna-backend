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
    private final ReservationEmailService reservationEmailService;

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    public List<Reservation> getReservationsForHost(String hostEmail) {
        return reservationRepository.findByTransportHostEmail(hostEmail);
    }

    public List<Reservation> getPendingReservationsForHost(String hostEmail) {
        return reservationRepository.findByTransportHostEmailAndStatus(
                hostEmail, ReservationStatus.PENDING_APPROVAL);
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
    public Reservation makeReservation(Long transportId, String userEmail,
                                       String boardingPoint, Integer seatsCount) {

        Transport transport = transportRepository.findByIdForUpdate(transportId)
                .orElseThrow(() -> new RuntimeException("Transport not found."));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found."));

        if (boardingPoint == null || boardingPoint.isBlank()) {
            throw new RuntimeException("Boarding point is required.");
        }

        if (seatsCount == null || seatsCount <= 0) {
            throw new RuntimeException("Seats count must be greater than 0.");
        }

        if (transport.getDepartureDate() == null
                || !transport.getDepartureDate().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("You cannot reserve a transport that has already started or passed.");
        }

        if (transport.getStatus() == TransportStatus.CANCELLED) {
            throw new RuntimeException("This transport has been cancelled.");
        }

        if (transport.getStatus() == TransportStatus.COMPLETED) {
            throw new RuntimeException("This transport is already completed.");
        }

        boolean alreadyReserved = reservationRepository
                .existsByUserIdAndTransportIdAndStatusIn(
                        user.getId(),
                        transport.getId(),
                        List.of(
                                ReservationStatus.PENDING_APPROVAL,
                                ReservationStatus.CONFIRMED,
                                ReservationStatus.BOARDED
                        )
                );

        if (alreadyReserved) {
            throw new RuntimeException("You already have an active reservation for this transport.");
        }

        Integer availableSeats = transport.getAvailableSeats();
        if (availableSeats == null) {
            availableSeats = transport.getTotalCapacity();
            transport.setAvailableSeats(availableSeats);
        }

        if (availableSeats < seatsCount) {
            throw new RuntimeException("Not enough seats available. Remaining seats: " + availableSeats);
        }

        if (!transport.checkWeatherConditions()) {
            transport.setStatus(TransportStatus.CANCELLED);
            transportRepository.save(transport);
            throw new RuntimeException("Departure cancelled due to weather conditions.");
        }

        double pricePerSeat = transport.calculatePrice(boardingPoint, seatsCount);
        double totalPrice = Math.round(pricePerSeat * seatsCount * 100.0) / 100.0;

        Reservation reservation = Reservation.builder()
                .reservedSeats(seatsCount)
                .totalPrice(totalPrice)
                .reservationDate(LocalDateTime.now())
                .boardingPoint(boardingPoint)
                .status(ReservationStatus.PENDING_APPROVAL)
                .transport(transport)
                .user(user)
                .build();

        transport.setAvailableSeats(availableSeats - seatsCount);
        refreshTransportStatusAfterSeatChange(transport);
        transportRepository.save(transport);

        Reservation saved = reservationRepository.save(reservation);
        reservationEmailService.sendPendingEmail(saved);

        return saved;
    }

    @Transactional
    public Reservation approveReservation(Long id, String hostEmail) {
        Reservation reservation = reservationRepository
                .findByIdAndTransportHostEmail(id, hostEmail)
                .orElseThrow(() -> new RuntimeException("Reservation not found for this host."));

        if (reservation.getStatus() != ReservationStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Only pending reservations can be approved.");
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        Reservation saved = reservationRepository.save(reservation);
        reservationEmailService.sendApprovalEmail(saved);
        return saved;
    }

    @Transactional
    public Reservation rejectReservation(Long id, String hostEmail) {
        Reservation reservation = reservationRepository
                .findByIdAndTransportHostEmail(id, hostEmail)
                .orElseThrow(() -> new RuntimeException("Reservation not found for this host."));

        if (reservation.getStatus() != ReservationStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Only pending reservations can be rejected.");
        }

        reservation.setStatus(ReservationStatus.REJECTED);

        Transport transport = reservation.getTransport();
        restoreSeatsToTransport(transport, reservation.getReservedSeats());

        Reservation saved = reservationRepository.save(reservation);
        reservationEmailService.sendRejectionEmail(saved);
        return saved;
    }

    @Transactional
    public Reservation markAsBoarded(Long id, String hostEmail) {
        Reservation reservation = reservationRepository
                .findByIdAndTransportHostEmail(id, hostEmail)
                .orElseThrow(() -> new RuntimeException("Reservation not found for this host."));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new RuntimeException("Cancelled reservations cannot be boarded.");
        }
        if (reservation.getStatus() == ReservationStatus.REJECTED) {
            throw new RuntimeException("Rejected reservations cannot be boarded.");
        }
        if (reservation.getStatus() == ReservationStatus.PENDING_APPROVAL) {
            throw new RuntimeException("This reservation has not been approved yet.");
        }
        if (reservation.getStatus() == ReservationStatus.BOARDED) {
            throw new RuntimeException("This passenger is already marked as boarded.");
        }

        reservation.setStatus(ReservationStatus.BOARDED);
        return reservationRepository.save(reservation);
    }

    @Transactional
    public Reservation cancelReservation(Long id, String userEmail) {
        Reservation reservation = getReservationById(id);

        if (reservation == null) {
            throw new RuntimeException("Reservation not found.");
        }

        if (reservation.getUser() == null
                || reservation.getUser().getEmail() == null
                || !reservation.getUser().getEmail().equalsIgnoreCase(userEmail)) {
            throw new RuntimeException("You can only cancel your own reservations.");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new RuntimeException("This reservation is already cancelled.");
        }

        if (reservation.getStatus() == ReservationStatus.REJECTED) {
            throw new RuntimeException("Rejected reservations cannot be cancelled.");
        }

        if (reservation.getStatus() == ReservationStatus.BOARDED) {
            throw new RuntimeException("Boarded reservations cannot be cancelled.");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        Transport transport = reservation.getTransport();
        restoreSeatsToTransport(transport, reservation.getReservedSeats());

        return reservationRepository.save(reservation);
    }

    @Transactional
    public void deleteReservation(Long id, String hostEmail) {
        Reservation reservation = getReservationByIdForHost(id, hostEmail);

        if (reservation == null) {
            throw new RuntimeException("Reservation not found.");
        }

        boolean occupiesSeats = reservation.getStatus() != ReservationStatus.CANCELLED
                && reservation.getStatus() != ReservationStatus.REJECTED;

        if (occupiesSeats) {
            Transport transport = reservation.getTransport();
            restoreSeatsToTransport(transport, reservation.getReservedSeats());
        }

        reservationRepository.delete(reservation);
    }

    @Transactional
    public int expirePendingReservationsOlderThanHours(int hours) {
        LocalDateTime threshold = LocalDateTime.now().minusHours(hours);

        List<Reservation> expiredReservations = reservationRepository
                .findByStatusAndReservationDateBefore(
                        ReservationStatus.PENDING_APPROVAL,
                        threshold
                );

        for (Reservation reservation : expiredReservations) {
            reservation.setStatus(ReservationStatus.CANCELLED);
            restoreSeatsToTransport(reservation.getTransport(), reservation.getReservedSeats());
            reservationRepository.save(reservation);
        }

        return expiredReservations.size();
    }

    private void restoreSeatsToTransport(Transport transport, Integer seatsToRestore) {
        if (transport == null || seatsToRestore == null || seatsToRestore <= 0) {
            return;
        }

        int current = transport.getAvailableSeats() != null ? transport.getAvailableSeats() : 0;
        int max = transport.getTotalCapacity() != null ? transport.getTotalCapacity() : current + seatsToRestore;

        transport.setAvailableSeats(Math.min(max, current + seatsToRestore));
        refreshTransportStatusAfterSeatChange(transport);
        transportRepository.save(transport);
    }

    private void refreshTransportStatusAfterSeatChange(Transport transport) {
        if (transport == null
                || transport.getStatus() == TransportStatus.CANCELLED
                || transport.getStatus() == TransportStatus.COMPLETED) {
            return;
        }

        if (transport.getDepartureDate() != null
                && transport.getDepartureDate().isAfter(LocalDateTime.now())) {
            transport.setStatus(TransportStatus.SCHEDULED);
        }
    }
}