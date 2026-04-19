package tn.esprit.spring.baladna.transport.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.transport.entity.Reservation;
import tn.esprit.spring.baladna.transport.entity.Transport;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ReservationEmailService {

    private final JavaMailSender mailSender;

    @Value("${transport.mail.from}")
    private String fromEmail;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Async
    public void sendPendingEmail(Reservation reservation) {
        String to = reservation.getUser().getEmail();
        String subject = "Reservation Request Submitted — " + getRoute(reservation);
        String html = buildPendingHtml(reservation);
        sendHtmlEmail(to, subject, html);
    }

    @Async
    public void sendApprovalEmail(Reservation reservation) {
        String to = reservation.getUser().getEmail();
        String subject = "Reservation Confirmed — " + getRoute(reservation);
        String html = buildApprovalHtml(reservation);
        sendHtmlEmail(to, subject, html);
    }

    @Async
    public void sendRejectionEmail(Reservation reservation) {
        String to = reservation.getUser().getEmail();
        String subject = "Reservation Rejected — " + getRoute(reservation);
        String html = buildRejectionHtml(reservation);
        sendHtmlEmail(to, subject, html);
    }

    private void sendHtmlEmail(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            System.err.println("[ReservationEmailService] Email sending failed: " + e.getMessage());
        }
    }

    private String buildPendingHtml(Reservation reservation) {
        return buildBaseTemplate(
                "Reservation Request Submitted",
                "Your reservation request has been received and is currently pending host approval.",
                reservation,
                "#1d4ed8",
                statusBox("Pending Approval", "You will receive another email as soon as the host responds.", "#f59e0b", "#fff7ed")
        );
    }

    private String buildApprovalHtml(Reservation reservation) {
        return buildBaseTemplate(
                "Reservation Confirmed",
                "Great news! Your reservation has been approved by the host.",
                reservation,
                "#16a34a",
                statusBox("Confirmed", "Your booking is now confirmed. You can access your QR code and PDF ticket from the application.", "#16a34a", "#f0fdf4")
        );
    }

    private String buildRejectionHtml(Reservation reservation) {
        return buildBaseTemplate(
                "Reservation Rejected",
                "Your reservation request has been rejected by the host.",
                reservation,
                "#dc2626",
                statusBox("Rejected", "The reserved seats were released automatically. You may choose another transport from the application.", "#dc2626", "#fef2f2")
        );
    }

    private String buildBaseTemplate(String title,
                                     String intro,
                                     Reservation reservation,
                                     String headerColor,
                                     String statusSection) {

        String fullName = getSafeFullName(reservation);
        String route = getRoute(reservation);
        String departureDate = reservation.getTransport().getDepartureDate() != null
                ? reservation.getTransport().getDepartureDate().format(DATE_FORMAT)
                : "N/A";
        String boardingPoint = reservation.getBoardingPoint() != null
                ? reservation.getBoardingPoint()
                : "N/A";

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>%s</title>
                </head>
                <body style="margin:0;padding:0;background:#f4f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                  <div style="max-width:760px;margin:30px auto;background:#ffffff;border-radius:18px;overflow:hidden;border:1px solid #e5e7eb;">
                    
                    <div style="background:%s;padding:28px 32px;text-align:center;">
                      <h1 style="margin:0;color:#ffffff;font-size:30px;font-weight:700;">%s</h1>
                      <p style="margin:10px 0 0;color:#dbeafe;font-size:18px;">Baladna Transport</p>
                    </div>

                    <div style="padding:34px 40px;">
                      <p style="margin:0 0 10px;font-size:16px;">Hello <strong>%s</strong>,</p>
                      <p style="margin:0 0 28px;font-size:15px;line-height:1.7;color:#475569;">%s</p>

                      <div style="border:1px solid #e5e7eb;border-radius:16px;padding:24px 28px;background:#fafafa;">
                        <table style="width:100%%;border-collapse:collapse;">
                          <tr>
                            <td style="padding:14px 0;color:#64748b;font-size:14px;width:34%%;">Route</td>
                            <td style="padding:14px 0;font-size:16px;font-weight:700;color:#334155;">%s</td>
                          </tr>
                          <tr><td colspan="2" style="border-top:1px solid #e5e7eb;"></td></tr>
                          <tr>
                            <td style="padding:14px 0;color:#64748b;font-size:14px;">Departure Date</td>
                            <td style="padding:14px 0;font-size:15px;font-weight:600;">%s</td>
                          </tr>
                          <tr><td colspan="2" style="border-top:1px solid #e5e7eb;"></td></tr>
                          <tr>
                            <td style="padding:14px 0;color:#64748b;font-size:14px;">Requested Seats</td>
                            <td style="padding:14px 0;font-size:15px;font-weight:600;">%d</td>
                          </tr>
                          <tr><td colspan="2" style="border-top:1px solid #e5e7eb;"></td></tr>
                          <tr>
                            <td style="padding:14px 0;color:#64748b;font-size:14px;">Total Price</td>
                            <td style="padding:14px 0;font-size:15px;font-weight:700;">%.2f DT</td>
                          </tr>
                          <tr><td colspan="2" style="border-top:1px solid #e5e7eb;"></td></tr>
                          <tr>
                            <td style="padding:14px 0;color:#64748b;font-size:14px;">Boarding Point</td>
                            <td style="padding:14px 0;font-size:15px;font-weight:600;">%s</td>
                          </tr>
                        </table>
                      </div>

                      %s

                      <p style="margin:28px 0 0;font-size:13px;color:#94a3b8;text-align:center;">
                        Thank you for travelling with Baladna.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                title,
                headerColor,
                title,
                fullName,
                intro,
                route,
                departureDate,
                reservation.getReservedSeats(),
                reservation.getTotalPrice(),
                boardingPoint,
                statusSection
        );
    }

    private String statusBox(String title, String subtitle, String borderColor, String backgroundColor) {
        return """
                <div style="margin-top:26px;padding:20px 24px;border:2px solid %s;background:%s;border-radius:16px;text-align:center;">
                  <h3 style="margin:0 0 10px;font-size:18px;color:%s;">%s</h3>
                  <p style="margin:0;font-size:15px;line-height:1.6;color:#7c2d12;">%s</p>
                </div>
                """.formatted(borderColor, backgroundColor, borderColor, title, subtitle);
    }

    private String getSafeFullName(Reservation reservation) {
        String firstName = reservation.getUser().getFirstName() != null ? reservation.getUser().getFirstName().trim() : "";
        String lastName = reservation.getUser().getLastName() != null ? reservation.getUser().getLastName().trim() : "";
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isBlank() ? reservation.getUser().getEmail() : fullName;
    }

    private String getRoute(Reservation reservation) {
        Transport transport = reservation.getTransport();
        return transport.getTrajet().getDepartureStation().getName()
                + " → " +
                transport.getTrajet().getArrivalStation().getName();
    }
}