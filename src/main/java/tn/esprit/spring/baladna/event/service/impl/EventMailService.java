package tn.esprit.spring.baladna.event.service.impl;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.event.entity.EventReservation;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class EventMailService {

    private final UserRepository userRepository;
    private static final Logger log = LoggerFactory.getLogger(EventMailService.class);

    private final JavaMailSender mailSender;
    private final TicketAndCalendarService ticketAndCalendarService;

    @Value("${app.mail.from:${spring.mail.username:}}")
    private String fromAddress;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    public void sendReservationConfirmation(EventReservation reservation) {
        if (fromAddress == null || fromAddress.isBlank()
                || smtpUsername == null || smtpUsername.isBlank()
                || smtpPassword == null || smtpPassword.isBlank()) {
            log.warn("Reservation email skipped (SMTP not configured: app.mail.from, username, password)");
            return;
        }
        try {
            User user = userRepository.findById(reservation.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found for reservation"));

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            StringBuilder body = new StringBuilder();
            body.append("<div style='max-width:400px;margin:24px auto;padding:24px;border-radius:12px;background:#f9f9f9;border:1px solid #e5e7eb;font-family:sans-serif;'>");
            body.append("<h2 style='color:#1e88e5;text-align:center;'>Reservation Confirmed!</h2>");
            String qrBase64 = reservation.getQrCodeImageBase64();
            if ((qrBase64 == null || qrBase64.isBlank()) && reservation.getQrCode() != null && !reservation.getQrCode().isBlank()) {
                qrBase64 = reservation.getQrCode();
            }
            if (qrBase64 != null && !qrBase64.isBlank()) {
                body.append("<div style='text-align:center;margin:20px 0;'>")
                    .append("<img src='data:image/png;base64,")
                    .append(qrBase64)
                    .append("' alt='QR Code' style='width:180px;height:180px;border-radius:8px;border:2px solid #E5E7EB;background:white;padding:8px;' />")
                    .append("<p style='color:#555;font-size:14px;'>Show this QR code at the event</p>")
                    .append("</div>");
            }
            body.append("<div style='margin:18px 0 10px 0;padding:12px 18px;background:#fff;border-radius:8px;border:1px solid #e0e0e0;'>");
            body.append("<p style='margin:0 0 6px 0;'><strong>Event:</strong> ").append(reservation.getEvent().getTitle()).append("</p>");
            body.append("<p style='margin:0 0 6px 0;'><strong>Date:</strong> ").append(reservation.getEvent().getStartAt()).append("</p>");
            body.append("<p style='margin:0 0 6px 0;'><strong>Location:</strong> ").append(reservation.getEvent().getLocation()).append("</p>");
            body.append("<p style='margin:0 0 6px 0;'><strong>Seats:</strong> ").append(reservation.getPersonsCount()).append("</p>");
            body.append("<p style='margin:0 0 6px 0;'><strong>Total:</strong> ").append(reservation.getTotalPrice()).append(" EUR</p>");
        
            body.append("<div style='margin-top:18px;color:#888;font-size:13px;text-align:center;'>Thank you for your reservation.</div>");
            body.append("</div>");

            helper.setTo(user.getEmail());
            helper.setFrom(fromAddress);
            helper.setSubject("🎟️ Event Confirmation - " + reservation.getEvent().getTitle());
            helper.setText(body.toString(), true);
            // === PDF ATTACHMENT ===
            byte[] pdf = ticketAndCalendarService.generateTicket(reservation);
            helper.addAttachment("ticket.pdf", new ByteArrayResource(pdf));
            // === CALENDAR ATTACHMENT ===
            byte[] ics = ticketAndCalendarService.generateICS(reservation);
            helper.addAttachment("event.ics", new ByteArrayResource(ics));
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Email sending failed: {}", e.getMessage());
        }
    }
}
