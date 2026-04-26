package tn.esprit.spring.baladna.transport.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.baladna.transport.dto.ReservationTicketValidationResponseDTO;
import tn.esprit.spring.baladna.transport.entity.Reservation;
import tn.esprit.spring.baladna.transport.entity.ReservationStatus;
import tn.esprit.spring.baladna.transport.repository.ReservationRepository;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

@Service
public class ReservationTicketService {

    private final ReservationRepository reservationRepository;
    private final String ticketSecret;

    public ReservationTicketService(
            ReservationRepository reservationRepository,
            @Value("${transport.ticket.secret:baladna-dev-transport-ticket-key}") String ticketSecret
    ) {
        this.reservationRepository = reservationRepository;
        this.ticketSecret = ticketSecret;
    }

    public String generateTicketCode(Reservation reservation) {
        if (reservation == null || reservation.getId() == null) {
            return null;
        }

        String payload = buildTicketPayload(reservation, null, false);
        String signature = hmacHex(payload).substring(0, 6).toUpperCase(Locale.ROOT);
        return String.format(Locale.ROOT, "BLD-%04d-%s", reservation.getId(), signature);
    }

    @Transactional
    public ReservationTicketValidationResponseDTO validateTicketCode(String rawTicketCode, String hostEmail) {
        String ticketCode = normalize(rawTicketCode);
        if (ticketCode == null) {
            return invalid("Ticket code is required.");
        }

        Long reservationId = extractReservationId(ticketCode);
        if (reservationId == null) {
            return invalid("Ticket format is invalid.");
        }

        Optional<Reservation> optionalReservation = reservationRepository.findByIdAndTransportHostEmail(reservationId, hostEmail);
        if (optionalReservation.isEmpty()) {
            return invalid("Reservation not found for this host.");
        }

        Reservation reservation = optionalReservation.get();
        String expectedTicketCode = generateTicketCode(reservation);

        if (!matchesKnownTicketCode(reservation, ticketCode)) {
            return invalid("Ticket signature does not match the reservation.");
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return buildResponse(reservation, expectedTicketCode, false, "Ticket found but the reservation is cancelled.");
        }

        if (reservation.getStatus() == ReservationStatus.REJECTED) {
            return buildResponse(reservation, expectedTicketCode, false, "Ticket found but the reservation was rejected.");
        }

        if (reservation.getStatus() == ReservationStatus.PENDING_APPROVAL) {
            return buildResponse(reservation, expectedTicketCode, false, "Ticket found but the reservation is still pending host approval.");
        }

        if (reservation.getStatus() == ReservationStatus.BOARDED) {
            return buildResponse(reservation, expectedTicketCode, true, "Passenger already boarded. Ticket previously validated.");
        }

        return buildResponse(reservation, expectedTicketCode, true, "Ticket valid. Reservation is confirmed for boarding.");
    }

    private ReservationTicketValidationResponseDTO buildResponse(
            Reservation reservation,
            String ticketCode,
            boolean valid,
            String message
    ) {
        return ReservationTicketValidationResponseDTO.builder()
                .valid(valid)
                .message(message)
                .reservationId(reservation.getId())
                .transportId(reservation.getTransport() != null ? reservation.getTransport().getId() : null)
                .ticketCode(ticketCode)
                .passengerName(reservation.getUser() != null
                        ? reservation.getUser().getFirstName() + " " + reservation.getUser().getLastName()
                        : null)
                .passengerEmail(reservation.getUser() != null ? reservation.getUser().getEmail() : null)
                .transportRoute(buildTransportRoute(reservation))
                .boardingPoint(reservation.getBoardingPoint())
                .reservedSeats(reservation.getReservedSeats())
                .totalPrice(reservation.getTotalPrice())
                .reservationDate(reservation.getReservationDate())
                .status(reservation.getStatus())
                .build();
    }

    private ReservationTicketValidationResponseDTO invalid(String message) {
        return ReservationTicketValidationResponseDTO.builder()
                .valid(false)
                .message(message)
                .build();
    }

    private Long extractReservationId(String ticketCode) {
        String[] parts = ticketCode.split("-");
        if (parts.length != 3 || !"BLD".equalsIgnoreCase(parts[0])) {
            return null;
        }

        try {
            return Long.parseLong(parts[1]);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String buildTransportRoute(Reservation reservation) {
        if (reservation == null || reservation.getTransport() == null || reservation.getTransport().getTrajet() == null) {
            return "Transport reservation";
        }

        String departure = reservation.getTransport().getTrajet().getDepartureStation() != null
                ? reservation.getTransport().getTrajet().getDepartureStation().getName()
                : "Departure";
        String arrival = reservation.getTransport().getTrajet().getArrivalStation() != null
                ? reservation.getTransport().getTrajet().getArrivalStation().getName()
                : "Arrival";

        return departure + " -> " + arrival;
    }

    private String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    private boolean matchesKnownTicketCode(Reservation reservation, String ticketCode) {
        if (ticketCode == null) {
            return false;
        }

        String canonicalTicketCode = generateTicketCode(reservation);
        if (canonicalTicketCode != null && canonicalTicketCode.equalsIgnoreCase(ticketCode)) {
            return true;
        }

        // Compatibility path for tickets generated before the code was stabilised.
        for (ReservationStatus status : ReservationStatus.values()) {
            String legacyTicketCode = generateLegacyTicketCode(reservation, status);
            if (legacyTicketCode != null && legacyTicketCode.equalsIgnoreCase(ticketCode)) {
                return true;
            }
        }

        return false;
    }

    private String generateLegacyTicketCode(Reservation reservation, ReservationStatus status) {
        if (reservation == null || reservation.getId() == null) {
            return null;
        }

        String payload = buildTicketPayload(reservation, status, true);
        String signature = hmacHex(payload).substring(0, 6).toUpperCase(Locale.ROOT);
        return String.format(Locale.ROOT, "BLD-%04d-%s", reservation.getId(), signature);
    }

    private String buildTicketPayload(
            Reservation reservation,
            ReservationStatus statusOverride,
            boolean includeStatus
    ) {
        StringBuilder payload = new StringBuilder()
                .append(String.valueOf(reservation.getId()))
                .append('|')
                .append(String.valueOf(reservation.getTransport() != null ? reservation.getTransport().getId() : null))
                .append('|')
                .append(String.valueOf(reservation.getReservedSeats()))
                .append('|')
                .append(String.valueOf(reservation.getTotalPrice()))
                .append('|')
                .append(String.valueOf(reservation.getReservationDate()))
                .append('|')
                .append(String.valueOf(reservation.getBoardingPoint()));

        if (includeStatus) {
            payload.append('|').append(String.valueOf(statusOverride != null ? statusOverride : reservation.getStatus()));
        }

        return payload.toString();
    }

    private String hmacHex(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(ticketSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte part : digest) {
                builder.append(String.format(Locale.ROOT, "%02x", part));
            }
            return builder.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate ticket signature", exception);
        }
    }
}
