package tn.esprit.spring.baladna.transport.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.spring.baladna.transport.dto.ReservationTicketValidationResponseDTO;
import tn.esprit.spring.baladna.transport.entity.Reservation;
import tn.esprit.spring.baladna.transport.entity.ReservationStatus;
import tn.esprit.spring.baladna.transport.entity.Transport;
import tn.esprit.spring.baladna.transport.repository.ReservationRepository;
import tn.esprit.spring.baladna.user.entity.User;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationTicketServiceTest {

    private static final String HOST_EMAIL = "host@baladna.tn";
    private static final String TICKET_SECRET = "test-transport-ticket-secret";

    @Mock
    private ReservationRepository reservationRepository;

    private ReservationTicketService reservationTicketService;

    @BeforeEach
    void setUp() {
        reservationTicketService = new ReservationTicketService(reservationRepository, TICKET_SECRET);
    }

    @Test
    void generateTicketCode_staysStableWhenReservationStatusChanges() {
        Reservation reservation = buildReservation(ReservationStatus.CONFIRMED);

        String confirmedTicketCode = reservationTicketService.generateTicketCode(reservation);
        reservation.setStatus(ReservationStatus.BOARDED);
        String boardedTicketCode = reservationTicketService.generateTicketCode(reservation);

        assertEquals(confirmedTicketCode, boardedTicketCode);
    }

    @Test
    void validateTicketCode_keepsConfirmedReservationConfirmed() {
        Reservation reservation = buildReservation(ReservationStatus.CONFIRMED);
        String ticketCode = reservationTicketService.generateTicketCode(reservation);

        when(reservationRepository.findByIdAndTransportHostEmail(reservation.getId(), HOST_EMAIL))
                .thenReturn(Optional.of(reservation));

        ReservationTicketValidationResponseDTO response =
                reservationTicketService.validateTicketCode(ticketCode, HOST_EMAIL);

        assertTrue(response.isValid());
        assertEquals(ReservationStatus.CONFIRMED, response.getStatus());
        assertEquals("Ticket valid. Reservation is confirmed for boarding.", response.getMessage());
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void validateTicketCode_acceptsLegacyStatusSignedCodesDuringTransition() {
        Reservation reservation = buildReservation(ReservationStatus.BOARDED);
        String legacyConfirmedTicketCode = generateLegacyTicketCode(reservation, ReservationStatus.CONFIRMED);

        when(reservationRepository.findByIdAndTransportHostEmail(reservation.getId(), HOST_EMAIL))
                .thenReturn(Optional.of(reservation));

        ReservationTicketValidationResponseDTO response =
                reservationTicketService.validateTicketCode(legacyConfirmedTicketCode, HOST_EMAIL);

        assertTrue(response.isValid());
        assertEquals(ReservationStatus.BOARDED, response.getStatus());
        assertEquals("Passenger already boarded. Ticket previously validated.", response.getMessage());
    }

    @Test
    void validateTicketCode_rejectsPendingReservations() {
        Reservation reservation = buildReservation(ReservationStatus.PENDING_APPROVAL);
        String ticketCode = reservationTicketService.generateTicketCode(reservation);

        when(reservationRepository.findByIdAndTransportHostEmail(reservation.getId(), HOST_EMAIL))
                .thenReturn(Optional.of(reservation));

        ReservationTicketValidationResponseDTO response =
                reservationTicketService.validateTicketCode(ticketCode, HOST_EMAIL);

        assertFalse(response.isValid());
        assertEquals(ReservationStatus.PENDING_APPROVAL, response.getStatus());
        assertEquals("Ticket found but the reservation is still pending host approval.", response.getMessage());
    }

    private Reservation buildReservation(ReservationStatus status) {
        Transport transport = Transport.builder()
                .id(45L)
                .build();

        User user = User.builder()
                .id(9L)
                .firstName("Fatma")
                .lastName("Esprit")
                .email("fatma@baladna.tn")
                .build();

        return Reservation.builder()
                .id(12L)
                .reservedSeats(2)
                .totalPrice(24.0)
                .reservationDate(LocalDateTime.of(2026, 4, 20, 10, 15))
                .boardingPoint("Bousaada")
                .status(status)
                .transport(transport)
                .user(user)
                .build();
    }

    private String generateLegacyTicketCode(Reservation reservation, ReservationStatus status) {
        String payload = String.join("|",
                String.valueOf(reservation.getId()),
                String.valueOf(reservation.getTransport() != null ? reservation.getTransport().getId() : null),
                String.valueOf(reservation.getReservedSeats()),
                String.valueOf(reservation.getTotalPrice()),
                String.valueOf(reservation.getReservationDate()),
                String.valueOf(reservation.getBoardingPoint()),
                String.valueOf(status)
        );

        String signature = hmacHex(payload).substring(0, 6).toUpperCase(Locale.ROOT);
        return String.format(Locale.ROOT, "BLD-%04d-%s", reservation.getId(), signature);
    }

    private String hmacHex(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(TICKET_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte part : digest) {
                builder.append(String.format(Locale.ROOT, "%02x", part));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
