package tn.esprit.spring.baladna.event.service;

import tn.esprit.spring.baladna.event.dto.EventReservationDTO;
import tn.esprit.spring.baladna.event.entity.EventReservation;
import java.util.List;

public interface IEventReservationService {
    List<EventReservation> retrieveEventReservations();
    EventReservation addEventReservation(EventReservationDTO reservation);
    EventReservation updateEventReservation(EventReservationDTO reservation);
    EventReservation retrieveEventReservation(Long id);
    void removeEventReservation(Long id);
}
