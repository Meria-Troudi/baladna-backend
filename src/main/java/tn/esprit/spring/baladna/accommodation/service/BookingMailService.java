package tn.esprit.spring.baladna.accommodation.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class BookingMailService {

    private static final Logger log = LoggerFactory.getLogger(BookingMailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    @Value("${spring.mail.password:}")
    private String smtpPassword;

    /** Shown as From: on outgoing mail (defaults to app.mail.from in application.properties). */
    @Value("${app.mail.from:${spring.mail.username:}}")
    private String fromAddress;

    public boolean sendBookingConfirmation(
            String toEmail,
            String guestName,
            String invoiceNumber,
            String confirmationCode,
            String qrDataUrl) {
        if (mailSender == null
                || smtpUsername == null || smtpUsername.isBlank()
                || smtpPassword == null || smtpPassword.isBlank()) {
            log.info("Email skipped (SMTP not configured: set MAIL_USERNAME / MAIL_PASSWORD or spring.mail.*). Guest: {}, invoice: {}",
                    toEmail, invoiceNumber);
            return false;
        }
        String from = (fromAddress != null && !fromAddress.isBlank()) ? fromAddress : smtpUsername;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setFrom(from);
            helper.setSubject("Baladna — Confirmation de réservation " + invoiceNumber);
            String body = """
                    <p>Bonjour %s,</p>
                    <p>Votre paiement est confirmé.</p>
                    <ul>
                      <li><strong>Facture :</strong> %s</li>
                      <li><strong>Code de confirmation :</strong> %s</li>
                    </ul>
                    <p>Présentez ce QR à l’hébergement :</p>
                    <p><img src="%s" alt="QR" width="220" height="220"/></p>
                    <p>Merci d’avoir choisi Baladna.</p>
                    """.formatted(
                    escape(guestName),
                    escape(invoiceNumber),
                    escape(confirmationCode),
                    qrDataUrl);
            helper.setText(body, true);
            mailSender.send(message);
            return true;
        } catch (Exception e) {
            log.warn("Could not send booking email: {}", e.getMessage());
            return false;
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
