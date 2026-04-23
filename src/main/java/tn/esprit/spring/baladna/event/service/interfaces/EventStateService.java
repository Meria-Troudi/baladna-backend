package tn.esprit.spring.baladna.event.service.interfaces;

import tn.esprit.spring.baladna.event.dto.EventUserStateDTO;

public interface EventStateService {
    EventUserStateDTO getUserState(Long eventId, Long userId);
}