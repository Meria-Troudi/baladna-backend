package tn.esprit.spring.baladna.event.service.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.event.entity.EventReservation;
import tn.esprit.spring.baladna.event.entity.enums.ReservationStatus;
import tn.esprit.spring.baladna.event.repository.EventReservationRepository;

@RestController
@RequestMapping("/api/qr")
@RequiredArgsConstructor
public class QrValidationController {

    private final QrService qrService;
    private final EventReservationRepository reservationRepository;

    @PostMapping("/validate")
    public ResponseEntity<?> validate(@RequestBody QrRequest req) {

        String qr = req.getToken();

        String[] p = qr.split("\\|");
        if (p.length != 4) {
            return ResponseEntity.ok(new QrResponse(false, "INVALID_FORMAT"));
        }

        Long reservationId = Long.parseLong(p[0]);

        EventReservation r = reservationRepository.findById(reservationId)
                .orElse(null);

        if (r == null)
            return ResponseEntity.ok(new QrResponse(false, "NOT_FOUND"));

        if (r.getPaymentStatus() != tn.esprit.spring.baladna.event.entity.enums.PaymentStatus.PAID)
            return ResponseEntity.ok(new QrResponse(false, "NOT_PAID"));

        if (r.getStatus() != ReservationStatus.CONFIRMED)
            return ResponseEntity.ok(new QrResponse(false, "NOT_CONFIRMED"));

        // Check signature and expiry separately for granular feedback
        String payload = p[0] + "|" + p[1] + "|" + p[2];
        String signature = p[3];
        if (!qrService.verify(qr)) {
            // Check if signature is invalid or expired
            if (!qrService.sign(payload).equals(signature)) {
                return ResponseEntity.ok(new QrResponse(false, "INVALID_SIGNATURE"));
            }
            long expiry = Long.parseLong(p[2]);
            if (expiry < java.time.Instant.now().getEpochSecond()) {
                return ResponseEntity.ok(new QrResponse(false, "EXPIRED"));
            }
            return ResponseEntity.ok(new QrResponse(false, "INVALID_SIGNATURE"));
        }

        return ResponseEntity.ok(new QrResponse(true, "VALID"));
    }

    static class QrRequest {
        private String token;
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }

    static class QrResponse {
        public boolean valid;
        public String reason;

        public QrResponse(boolean v, String r) {
            this.valid = v;
            this.reason = r;
        }
    }
}
