package tn.esprit.spring.baladna.event.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.baladna.event.dto.ReservationWithEventDTO;
import tn.esprit.spring.baladna.event.entity.Event;
import tn.esprit.spring.baladna.event.entity.EventReservation;
import tn.esprit.spring.baladna.event.entity.enums.PaymentStatus;
import tn.esprit.spring.baladna.event.entity.enums.ReservationStatus;
import tn.esprit.spring.baladna.event.repository.EventRepository;
import tn.esprit.spring.baladna.event.repository.EventReservationRepository;
import tn.esprit.spring.baladna.event.service.interfaces.IEventReservationService;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import tn.esprit.spring.baladna.event.service.payment.QrService;

@AllArgsConstructor
@Service
public class EventReservationServiceImpl implements IEventReservationService {
    private final EventReservationRepository reservationRepository;
    @Override
    public EventReservation findWithEvent(Long reservationId) {
        return reservationRepository.findWithEvent(reservationId)
            .orElseThrow(() -> new RuntimeException("Reservation not found"));
    }
    private final EventRepository eventRepository;
    private final QrService qrService;

    @Override
    public java.util.Optional<EventReservation> findByStripePaymentIntentId(String stripePaymentIntentId) {
        return reservationRepository.findByStripePaymentIntentId(stripePaymentIntentId);
    }

    @Override
    public List<EventReservation> getLastConfirmedOrWaitlistedReservations() {
        return reservationRepository.findTopConfirmedOrWaitlistedReservations();
    }
    @Override
    public EventReservation save(EventReservation reservation) {
        return reservationRepository.save(reservation);
    }

    @Override
    public EventReservation findById(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));
    }



    @Override
    @Transactional
    public EventReservation createReservation(Long eventId, Long userId, int personsCount) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        boolean isFree = event.getPrice() == null || event.getPrice() == 0.0;
        boolean isFull = event.getBookedSeats() >= event.getCapacity();

        EventReservation reservation = new EventReservation();
        reservation.setEvent(event);
        reservation.setUserId(userId);
        reservation.setPersonsCount(personsCount);
        reservation.setTotalPrice(event.getPrice() * personsCount);
        reservation.setCreatedAt(LocalDateTime.now());

        if (isFull) {
            reservation.setStatus(ReservationStatus.WAITLISTED);
            reservation.setPaymentStatus(PaymentStatus.PENDING);
            reservation.setQrToken(null);
            reservation.setQrCodeImageBase64(null);
        } else if (isFree) {
            reservation.setStatus(ReservationStatus.CONFIRMED);
            reservation.setPaymentStatus(PaymentStatus.PAID);
            String qrToken = qrService.generateToken(reservation);
            String qrImage = qrService.generateQrImageBase64(qrToken);
            reservation.setQrToken(qrToken);
            reservation.setQrCodeImageBase64(qrImage);
            // Only increment bookedSeats for confirmed reservations
            event.setBookedSeats(event.getBookedSeats() + personsCount);
            eventRepository.save(event);
        } else {
            reservation.setStatus(ReservationStatus.PENDING);
            reservation.setPaymentStatus(PaymentStatus.PENDING);
            reservation.setQrToken(null);
            reservation.setQrCodeImageBase64(null);
        }

        return reservationRepository.save(reservation);
    }

    // Deprecated: findByStripeId is not used, use findByStripePaymentIntentId instead
    // @Override
    // public EventReservation findByStripeId(String stripeId) {
    //     return reservationRepository.findByStripePaymentIntentId(stripeId)
    //             .orElseThrow(() -> new RuntimeException("Reservation not found for Stripe ID: " + stripeId));
    // }

    @Override
    @Transactional
    public void cancelReservation(Long reservationId) {
        EventReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        Event event = reservation.getEvent();

        boolean wasConfirmed = reservation.getStatus() == ReservationStatus.CONFIRMED;

        // Mark as cancelled
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());

        if (wasConfirmed) {
            event.setBookedSeats(event.getBookedSeats() - reservation.getPersonsCount());
            eventRepository.save(event);
        }

        reservationRepository.save(reservation);

        promoteWaitlist(event);
    }

    @Override
    public List<EventReservation> getUserReservations(Long userId) {
return reservationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<EventReservation> getEventReservations(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        return reservationRepository.findByEvent(event);
    }

    private void promoteWaitlist(Event event) {
        List<EventReservation> waitlist = reservationRepository
                .findByEventAndStatusOrderByCreatedAtAsc(event, ReservationStatus.WAITLISTED);

        int availableSeats = event.getCapacity() - event.getBookedSeats();

        for (EventReservation r : waitlist) {
            if (availableSeats >= r.getPersonsCount()) {
                // Confirm reservation
                r.setStatus(ReservationStatus.CONFIRMED);
                String qrToken = qrService.generateToken(r);
                String qrImage = qrService.generateQrImageBase64(qrToken);
                r.setQrToken(qrToken);
                r.setQrCodeImageBase64(qrImage);

                availableSeats -= r.getPersonsCount();
                event.setBookedSeats(event.getBookedSeats() + r.getPersonsCount());

                reservationRepository.save(r);
                // TODO: notify user (email / notification)
            } else {
                break; // No more seats
            }
        }

        eventRepository.save(event);
    }

    //QR CODE GENERATION 

    private String generateQRCode(Long eventId, Long userId) {
        try {
            String data = "eventId=" + eventId + ";userId=" + userId;

            BitMatrix matrix = new MultiFormatWriter()
                    .encode(data, BarcodeFormat.QR_CODE, 250, 250);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);

            return Base64.getEncoder().encodeToString(baos.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("QR generation failed: " + e.getMessage());
        }
    }

    private ReservationStatus parseReservationStatus(String status) {
        return status == null ? null : ReservationStatus.valueOf(status);
    }

    private PaymentStatus parsePaymentStatus(String status) {
        return status == null ? null : PaymentStatus.valueOf(status);
    }

    @Override
    public List<Long> getReservedEventIdsByUser(Long userId) {
return reservationRepository.findByUserIdAndStatusIn(
            userId, 
            List.of(ReservationStatus.CONFIRMED, ReservationStatus.WAITLISTED)
        ).stream()
         .map(r -> r.getEvent().getId())
         .toList();
    }

    @Override
    @Transactional
    public EventReservation updateReservation(Long eventId, Long reservationId, int personsCount) {
        // Validate persons count
        if (personsCount < 1 || personsCount > 10) {
            throw new IllegalArgumentException("Persons count must be between 1 and 10");
        }

        EventReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        // Block all edits if payment is not pending
        if (reservation.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new IllegalStateException("Reservation cannot be modified after payment");
        }

        // Only allow updating PENDING_PAYMENT reservations
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalStateException("Cannot update a reservation with status: " + reservation.getStatus());
        }

        Event event = reservation.getEvent();
        if (!event.getId().equals(eventId)) {
            throw new IllegalArgumentException("Reservation does not belong to the specified event");
        }

        int oldPersonsCount = reservation.getPersonsCount();
        int personsDiff = personsCount - oldPersonsCount;
        int availableSeats = event.getCapacity() - event.getBookedSeats() + oldPersonsCount;

        // For pending reservations, check seat availability
        if (personsDiff > 0 && availableSeats < personsCount) {
            throw new IllegalStateException("Not enough seats available for " + personsCount + " persons");
        }

        // Update booked seats if needed (for pending, usually not booked yet)
        // event.setBookedSeats(event.getBookedSeats() + personsDiff);
        // eventRepository.save(event);

        // Update reservation
        reservation.setPersonsCount(personsCount);
        reservation.setTotalPrice(event.getPrice() * personsCount);

        return reservationRepository.save(reservation);
    }

    @Override
    public ReservationWithEventDTO getReservationWithEvent(Long reservationId) {
        EventReservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        
        return ReservationWithEventDTO.builder()
                .id(reservation.getId())
                .event(reservation.getEvent())  // This will be serialized now
                .touristUserId(reservation.getUserId())
                .personsCount(reservation.getPersonsCount())
                .totalPrice(reservation.getTotalPrice())
                .status(reservation.getStatus() != null ? reservation.getStatus().name() : null)
                .paymentStatus(reservation.getPaymentStatus() != null ? reservation.getPaymentStatus().name() : null)
                .qrCode(reservation.getQrCodeImageBase64())
                .createdAt(reservation.getCreatedAt() != null ? reservation.getCreatedAt().format(formatter) : null)
                .cancelledAt(reservation.getCancelledAt() != null ? reservation.getCancelledAt().format(formatter) : null)
                .build();
    }

    @Override
    public List<EventReservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    @Override
    public List<ReservationWithEventDTO> getUserReservationsWithEvent(Long userId) {
List<EventReservation> reservations = reservationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return convertToDTOList(reservations);
    }

    @Override
    public List<ReservationWithEventDTO> getEventReservationsWithEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        List<EventReservation> reservations = reservationRepository.findByEvent(event);
        return convertToDTOList(reservations);
    }

    @Override
    public List<ReservationWithEventDTO> getAllReservationsWithEvent() {
        List<EventReservation> reservations = reservationRepository.findAll();
        return convertToDTOList(reservations);
    }

    private List<ReservationWithEventDTO> convertToDTOList(List<EventReservation> reservations) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return reservations.stream()
                .map(r -> ReservationWithEventDTO.builder()
                        .id(r.getId())
                        .event(r.getEvent())
                        .touristUserId(r.getUserId())
                        .personsCount(r.getPersonsCount())
                        .totalPrice(r.getTotalPrice())
                        .status(r.getStatus() != null ? r.getStatus().name() : null)
                        .paymentStatus(r.getPaymentStatus() != null ? r.getPaymentStatus().name() : null)
                        .qrCode(r.getQrCodeImageBase64())
                        .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().format(formatter) : null)
                        .cancelledAt(r.getCancelledAt() != null ? r.getCancelledAt().format(formatter) : null)
                        .build())
                .toList();
    }
}
