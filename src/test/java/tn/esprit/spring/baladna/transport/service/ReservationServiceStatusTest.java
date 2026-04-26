package tn.esprit.spring.baladna.transport.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.spring.baladna.transport.entity.Reservation;
import tn.esprit.spring.baladna.transport.entity.ReservationStatus;
import tn.esprit.spring.baladna.transport.entity.Transport;
import tn.esprit.spring.baladna.transport.entity.TransportStatus;
import tn.esprit.spring.baladna.transport.repository.ReservationRepository;
import tn.esprit.spring.baladna.transport.repository.TransportRepository;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceStatusTest {

    private static final String HOST_EMAIL = "host@baladna.tn";
    private static final String TOURIST_EMAIL = "tourist@baladna.tn";

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private TransportRepository transportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationEmailService reservationEmailService;

    @Mock
    private TransportAiDatasetService transportAiDatasetService;

    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        reservationService = new ReservationService(
                reservationRepository,
                transportRepository,
                userRepository,
                reservationEmailService,
                transportAiDatasetService
        );
    }

    @Test
    void approveReservation_movesPendingToConfirmed() {
        Reservation reservation = buildReservation(ReservationStatus.PENDING_APPROVAL);

        when(reservationRepository.findByIdAndTransportHostEmail(reservation.getId(), HOST_EMAIL))
                .thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reservation updatedReservation = reservationService.approveReservation(reservation.getId(), HOST_EMAIL);

        assertEquals(ReservationStatus.CONFIRMED, updatedReservation.getStatus());
        verify(reservationEmailService).sendApprovalEmail(updatedReservation);
    }

    @Test
    void markAsBoarded_rejectsPendingReservations() {
        Reservation reservation = buildReservation(ReservationStatus.PENDING_APPROVAL);

        when(reservationRepository.findByIdAndTransportHostEmail(reservation.getId(), HOST_EMAIL))
                .thenReturn(Optional.of(reservation));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> reservationService.markAsBoarded(reservation.getId(), HOST_EMAIL)
        );

        assertEquals("This reservation has not been approved yet.", exception.getMessage());
    }

    @Test
    void markAsBoarded_movesConfirmedToBoarded() {
        Reservation reservation = buildReservation(ReservationStatus.CONFIRMED);

        when(reservationRepository.findByIdAndTransportHostEmail(reservation.getId(), HOST_EMAIL))
                .thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reservation updatedReservation = reservationService.markAsBoarded(reservation.getId(), HOST_EMAIL);

        assertEquals(ReservationStatus.BOARDED, updatedReservation.getStatus());
    }

    @Test
    void cancelReservation_movesConfirmedToCancelledAndRestoresSeats() {
        Reservation reservation = buildReservation(ReservationStatus.CONFIRMED);
        reservation.getTransport().setAvailableSeats(3);
        reservation.getTransport().setTotalCapacity(10);
        reservation.getTransport().setStatus(TransportStatus.FULL);
        reservation.getTransport().setDepartureDate(LocalDateTime.now().plusHours(4));

        when(reservationRepository.findById(reservation.getId()))
                .thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transportRepository.save(any(Transport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Reservation updatedReservation = reservationService.cancelReservation(reservation.getId(), TOURIST_EMAIL);

        assertEquals(ReservationStatus.CANCELLED, updatedReservation.getStatus());
        assertEquals(5, reservation.getTransport().getAvailableSeats());
        assertEquals(TransportStatus.SCHEDULED, reservation.getTransport().getStatus());
        verify(reservationEmailService).sendCancellationEmail(updatedReservation);
    }

    private Reservation buildReservation(ReservationStatus status) {
        User host = User.builder()
                .id(100L)
                .email(HOST_EMAIL)
                .build();

        Transport transport = Transport.builder()
                .id(45L)
                .availableSeats(8)
                .totalCapacity(10)
                .status(TransportStatus.SCHEDULED)
                .departureDate(LocalDateTime.now().plusDays(1))
                .host(host)
                .build();

        User tourist = User.builder()
                .id(200L)
                .email(TOURIST_EMAIL)
                .build();

        return Reservation.builder()
                .id(12L)
                .reservedSeats(2)
                .totalPrice(24.0)
                .reservationDate(LocalDateTime.of(2026, 4, 20, 10, 15))
                .boardingPoint("Bousaada")
                .status(status)
                .transport(transport)
                .user(tourist)
                .build();
    }
}
