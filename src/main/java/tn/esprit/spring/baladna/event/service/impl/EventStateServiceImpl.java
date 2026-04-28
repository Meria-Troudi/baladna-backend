package tn.esprit.spring.baladna.event.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.event.dto.EventUserStateDTO;
import tn.esprit.spring.baladna.event.entity.Event;
import tn.esprit.spring.baladna.event.entity.EventReservation;
import tn.esprit.spring.baladna.event.entity.enums.PaymentStatus;
import tn.esprit.spring.baladna.event.entity.enums.ReservationStatus;
import tn.esprit.spring.baladna.event.repository.EventRepository;
import tn.esprit.spring.baladna.event.repository.EventReservationRepository;
import tn.esprit.spring.baladna.event.service.interfaces.EventStateService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EventStateServiceImpl implements EventStateService {

    private final EventRepository eventRepository;
    private final EventReservationRepository reservationRepository;

    @Override
    public EventUserStateDTO getUserState(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        int capacity = event.getCapacity() == null ? 0 : event.getCapacity();
        int bookedSeats = event.getBookedSeats() == null ? 0 : event.getBookedSeats();
        boolean full = bookedSeats >= capacity;
        boolean past = event.getStartAt() != null && event.getStartAt().isBefore(LocalDateTime.now());

        EventReservation reservation = reservationRepository
                .findByEventIdAndUserId(eventId, userId)
                .orElse(null);

        String state;
        boolean canBook = false, canPay = false, canCancel = false, canEdit = false;
        if (reservation == null) {
            state = full ? "FULL" : "NONE";
            canBook = !full && !past;
        } else if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            state = "CANCELLED";
            canBook = !full && !past;
        } else if (reservation.getStatus() == ReservationStatus.WAITLISTED) {
            state = "WAITLISTED";
            canCancel = !past;
        } else if (reservation.getStatus() == ReservationStatus.CONFIRMED
                && reservation.getPaymentStatus() == PaymentStatus.PAID) {
            state = "CONFIRMED";
            canCancel = !past;
        } else if (reservation.getStatus() == ReservationStatus.PENDING
                && reservation.getPaymentStatus() == PaymentStatus.PENDING) {
            state = "PENDING_PAYMENT";
            canPay = !past;
            canEdit = !past;
            canCancel = !past;
        } else {
            state = "NONE";
            canBook = !full && !past;
        }

        return EventUserStateDTO.builder()
                .eventId(eventId)
                .state(state)
                .full(full)
                .past(past)
                .canBook(canBook)
                .canPay(canPay)
                .canCancel(canCancel)
                .canEdit(canEdit)
                .build();
    }
}