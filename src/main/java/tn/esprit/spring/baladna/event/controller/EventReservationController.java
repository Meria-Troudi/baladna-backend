package tn.esprit.spring.baladna.event.controller;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.event.dto.EventReservationDTO;
import tn.esprit.spring.baladna.event.entity.EventReservation;
import tn.esprit.spring.baladna.event.service.IEventReservationService;

import java.util.List;

@AllArgsConstructor
@RestController
@RequestMapping("/api/events/event-reservation")
public class EventReservationController {

    private final IEventReservationService reservationService;

    @GetMapping("/list")
    public List<EventReservation> retrieveEventReservations() {
        return reservationService.retrieveEventReservations();
    }

    @GetMapping("/get/{id}")
    public EventReservation retrieveEventReservation(@PathVariable Long id) {
        return reservationService.retrieveEventReservation(id);
    }

    @PostMapping("/add")
    public EventReservation addEventReservation(@RequestBody EventReservationDTO reservation) {
        return reservationService.addEventReservation(reservation);
    }

    @PutMapping("/update")
    public EventReservation updateEventReservation(@RequestBody EventReservationDTO reservation) {
        return reservationService.updateEventReservation(reservation);
    }

    @DeleteMapping("/delete/{id}")
    public void removeEventReservation(@PathVariable Long id) {
        reservationService.removeEventReservation(id);
    }
}
