package tn.esprit.spring.baladna.event.service.payment;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.event.entity.EventReservation;
import tn.esprit.spring.baladna.event.entity.enums.PaymentStatus;
import tn.esprit.spring.baladna.event.entity.enums.ReservationStatus;
import tn.esprit.spring.baladna.event.service.interfaces.IEventReservationService;
import tn.esprit.spring.baladna.event.service.impl.EventMailService;

@RestController
@RequestMapping("/api/events/payments")
@RequiredArgsConstructor
public class PaymenteventController {

    private final StripeService stripeService;
    private final IEventReservationService reservationService;
    private final QrService qrService;
    private final EventMailService mailService;

    @PostMapping("/create/{reservationId}")
    public String createPayment(@PathVariable Long reservationId) throws StripeException {
        EventReservation res = reservationService.findById(reservationId);
        PaymentIntent intent = stripeService.createPaymentIntent(res.getTotalPrice(), reservationId);
        res.setStripePaymentIntentId(intent.getId());
        reservationService.save(res);
        return intent.getClientSecret();
    }

    @PostMapping("/confirm/{paymentIntentId}")
    public EventReservation confirm(@PathVariable String paymentIntentId) {
        System.out.println("Confirming paymentIntentId = " + paymentIntentId);
        EventReservation res = reservationService.findByStripePaymentIntentId(paymentIntentId)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Reservation not found for paymentIntentId"
            ));
        System.out.println("Reservation found = " + res.getId());

        // Reject waitlisted or cancelled
        if (res.getStatus() == ReservationStatus.WAITLISTED) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Cannot confirm payment for waitlisted reservation"
            );
        }
        if (res.getStatus() == ReservationStatus.CANCELLED) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Cannot confirm payment for cancelled reservation"
            );
        }

        // Idempotency: if already paid/confirmed, return as-is (no duplicate QR/email)
        if (res.getPaymentStatus() == PaymentStatus.PAID && res.getStatus() == ReservationStatus.CONFIRMED && res.getQrToken() != null && res.getQrCodeImageBase64() != null) {
            return res;
        }

        res.setPaymentStatus(PaymentStatus.PAID);
        res.setStatus(ReservationStatus.CONFIRMED);
        String qrToken = qrService.generateToken(res);
        String qrImage = qrService.generateQrImageBase64(qrToken);
        res.setQrToken(qrToken);
        res.setQrCodeImageBase64(qrImage);
        EventReservation saved = reservationService.save(res);
        // Ensure event is loaded before sending email
        EventReservation full = reservationService.findWithEvent(saved.getId());
        mailService.sendReservationConfirmation(full);
        return saved;
    }

    @PostMapping("/fail/{paymentIntentId}")
    public EventReservation fail(@PathVariable String paymentIntentId) {
        EventReservation res = reservationService.findByStripePaymentIntentId(paymentIntentId)
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND,
                "Reservation not found for paymentIntentId"
            ));
        res.setPaymentStatus(PaymentStatus.FAILED);
        res.setStatus(ReservationStatus.CANCELLED);
        return reservationService.save(res);
    }
}