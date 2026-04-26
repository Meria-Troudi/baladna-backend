package tn.esprit.spring.baladna.event.service.impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.event.dto.EventReservationDTO;
import tn.esprit.spring.baladna.event.entity.Event;
import tn.esprit.spring.baladna.event.entity.EventReservation;
import tn.esprit.spring.baladna.event.entity.enums.PaymentStatus;
import tn.esprit.spring.baladna.event.entity.enums.ReservationStatus;
import tn.esprit.spring.baladna.event.repository.EventRepository;
import tn.esprit.spring.baladna.event.repository.EventReservationRepository;
import tn.esprit.spring.baladna.event.service.IEventReservationService;

import java.util.List;

@AllArgsConstructor
@Service
public class EventReservationServiceImpl implements IEventReservationService {

    private final EventReservationRepository reservationRepository;
    private final EventRepository eventRepository;

    @Override
    public List<EventReservation> retrieveEventReservations() {
        return reservationRepository.findAll();
    }

    @Override
    public EventReservation addEventReservation(EventReservationDTO reservation) {
        Event event = eventRepository.findById(reservation.getEventId()).orElse(null);
        EventReservation entity = EventReservation.builder()
                .event(event)
                .touristUserId(reservation.getUserId())
                .personsCount(reservation.getPersonsCount())
                .totalPrice(reservation.getTotalPrice())
                .status(parseReservationStatus(reservation.getStatus()))
                .paymentStatus(parsePaymentStatus(reservation.getPaymentStatus()))
                .build();
        return reservationRepository.save(entity);
    }

    @Override
    public EventReservation updateEventReservation(EventReservationDTO reservation) {
        EventReservation entity = reservationRepository.findById(reservation.getId()).orElse(null);
        if (entity == null) {
            return null;
        }
        if (reservation.getEventId() != null) {
            entity.setEvent(eventRepository.findById(reservation.getEventId()).orElse(null));
        }
        entity.setTouristUserId(reservation.getUserId());
        entity.setPersonsCount(reservation.getPersonsCount());
        entity.setTotalPrice(reservation.getTotalPrice());
        entity.setStatus(parseReservationStatus(reservation.getStatus()));
        entity.setPaymentStatus(parsePaymentStatus(reservation.getPaymentStatus()));
        return reservationRepository.save(entity);
    }

    @Override
    public EventReservation retrieveEventReservation(Long id) {
        return reservationRepository.findById(id).orElse(null);
    }

    @Override
    public void removeEventReservation(Long id) {
        reservationRepository.deleteById(id);
    }

    private ReservationStatus parseReservationStatus(String status) {
        return status == null ? null : ReservationStatus.valueOf(status);
    }

    private PaymentStatus parsePaymentStatus(String status) {
        return status == null ? null : PaymentStatus.valueOf(status);
    }
}
