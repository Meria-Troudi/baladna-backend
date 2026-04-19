package tn.esprit.spring.baladna.transport.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationMaintenanceService {

    private final ReservationService reservationService;

    @Scheduled(cron = "0 0 * * * *")
    public void expireOldPendingReservations() {
        int expiredCount = reservationService.expirePendingReservationsOlderThanHours(24);

        if (expiredCount > 0) {
            log.info("[ReservationMaintenanceService] Expired {} pending reservation(s).", expiredCount);
        }
    }
}