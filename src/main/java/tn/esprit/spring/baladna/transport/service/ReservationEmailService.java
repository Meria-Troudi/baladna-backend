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
        String subject = "⏳ Reservation Received — " + getRoute(reservation);
        String html = buildPendingHtml(reservation);
        sendHtmlEmail(to, subject, html);
    }

    @Async
    public void sendApprovalEmail(Reservation reservation) {
        String to = reservation.getUser().getEmail();
        String subject = "✅ Reservation Confirmed — " + getRoute(reservation);
        String html = buildApprovalHtml(reservation);
        sendHtmlEmail(to, subject, html);
    }

    @Async
    public void sendRejectionEmail(Reservation reservation) {
        String to = reservation.getUser().getEmail();
        String subject = "❌ Reservation Rejected — " + getRoute(reservation);
        String html = buildRejectionHtml(reservation);
        sendHtmlEmail(to, subject, html);
    }

    @Async
    public void sendCancellationEmail(Reservation reservation) {
        String to = reservation.getUser().getEmail();
        String subject = "🚫 Reservation Cancelled — " + getRoute(reservation);
        String html = buildCancellationHtml(reservation);
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
                "⏳ Reservation Request Received",
                "Your reservation request has been successfully received and is now waiting for host approval.",
                reservation,
                "#1d4ed8",
                statusBox(
                        "Pending Approval ⏳",
                        "You will receive another email as soon as the host approves or rejects your request.",
                        "#f59e0b",
                        "#fff7ed",
                        "#92400e"
                ),
                actionNote(
                        "What happens next?",
                        "The host will review your request. Once a decision is made, you will receive an updated confirmation email."
                )
        );
    }

    private String buildApprovalHtml(Reservation reservation) {
        return buildBaseTemplate(
                "✅ Reservation Confirmed",
                "Great news! Your reservation has been approved by the host and your trip is now confirmed.",
                reservation,
                "#16a34a",
                statusBox(
                        "Confirmed ✅",
                        "Your booking is confirmed. You can now access your QR code and PDF ticket from the application.",
                        "#16a34a",
                        "#f0fdf4",
                        "#166534"
                ),
                actionNote(
                        "Next step",
                        "Open the application to view your digital ticket, download your PDF copy, and get ready for departure."
                )
        );
    }

    private String buildRejectionHtml(Reservation reservation) {
        return buildBaseTemplate(
                "❌ Reservation Rejected",
                "Unfortunately, the host was unable to approve your reservation request.",
                reservation,
                "#dc2626",
                statusBox(
                        "Rejected ❌",
                        "The requested seats have been released automatically. You can try another departure from the application.",
                        "#dc2626",
                        "#fef2f2",
                        "#991b1b"
                ),
                actionNote(
                        "Need another trip?",
                        "You can return to the app, browse available transports, and submit a new reservation request anytime."
                )
        );
    }

    private String buildCancellationHtml(Reservation reservation) {
        return buildBaseTemplate(
                "🚫 Reservation Cancelled",
                "Your reservation has been cancelled successfully.",
                reservation,
                "#b45309",
                statusBox(
                        "Cancelled 🚫",
                        "This booking is no longer active. The reserved seats were released back to availability.",
                        "#b45309",
                        "#fff7ed",
                        "#92400e"
                ),
                actionNote(
                        "Need a new booking?",
                        "You can go back to the application and reserve another available transport whenever you want."
                )
        );
    }

    private String buildBaseTemplate(String title,
                                     String intro,
                                     Reservation reservation,
                                     String headerColor,
                                     String statusSection,
                                     String extraSection) {

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
                <body style="margin:0;padding:24px 12px;background:#f4f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
                  <div style="max-width:760px;margin:0 auto;background:#ffffff;border-radius:22px;overflow:hidden;border:1px solid #e5e7eb;box-shadow:0 12px 32px rgba(15,23,42,0.08);">

                    <div style="background:%s;padding:30px 32px;text-align:center;">
                      <div style="font-size:34px;line-height:1;margin-bottom:10px;">🚌</div>
                      <h1 style="margin:0;color:#ffffff;font-size:30px;font-weight:700;">%s</h1>
                      <p style="margin:10px 0 0;color:#dbeafe;font-size:17px;">Baladna Transport</p>
                    </div>

                    <div style="padding:34px 40px;">
                      <p style="margin:0 0 10px;font-size:16px;">Hello <strong>%s</strong>,</p>
                      <p style="margin:0 0 26px;font-size:15px;line-height:1.7;color:#475569;">%s</p>

                      <div style="border:1px solid #e5e7eb;border-radius:18px;padding:24px 28px;background:#fafafa;">
                        <table style="width:100%%;border-collapse:collapse;">
                          <tr>
                            <td style="padding:14px 0;color:#64748b;font-size:14px;width:34%%;">🛣️ Route</td>
                            <td style="padding:14px 0;font-size:16px;font-weight:700;color:#334155;">%s</td>
                          </tr>
                          <tr><td colspan="2" style="border-top:1px solid #e5e7eb;"></td></tr>
                          <tr>
                            <td style="padding:14px 0;color:#64748b;font-size:14px;">📅 Departure Date</td>
                            <td style="padding:14px 0;font-size:15px;font-weight:600;">%s</td>
                          </tr>
                          <tr><td colspan="2" style="border-top:1px solid #e5e7eb;"></td></tr>
                          <tr>
                            <td style="padding:14px 0;color:#64748b;font-size:14px;">💺 Reserved Seats</td>
                            <td style="padding:14px 0;font-size:15px;font-weight:600;">%d</td>
                          </tr>
                          <tr><td colspan="2" style="border-top:1px solid #e5e7eb;"></td></tr>
                          <tr>
                            <td style="padding:14px 0;color:#64748b;font-size:14px;">💰 Total Price</td>
                            <td style="padding:14px 0;font-size:15px;font-weight:700;">%.2f DT</td>
                          </tr>
                          <tr><td colspan="2" style="border-top:1px solid #e5e7eb;"></td></tr>
                          <tr>
                            <td style="padding:14px 0;color:#64748b;font-size:14px;">📍 Boarding Point</td>
                            <td style="padding:14px 0;font-size:15px;font-weight:600;">%s</td>
                          </tr>
                        </table>
                      </div>

                      %s

                      %s

                      <div style="margin-top:30px;padding-top:18px;border-top:1px solid #e5e7eb;">
                        <p style="margin:0;font-size:13px;color:#94a3b8;text-align:center;">
                          Thank you for travelling with Baladna 🌍
                        </p>
                      </div>
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
                statusSection,
                extraSection
        );
    }

    private String statusBox(String title,
                             String subtitle,
                             String borderColor,
                             String backgroundColor,
                             String textColor) {
        return """
                <div style="margin-top:26px;padding:20px 24px;border:2px solid %s;background:%s;border-radius:16px;text-align:center;">
                  <h3 style="margin:0 0 10px;font-size:18px;color:%s;">%s</h3>
                  <p style="margin:0;font-size:15px;line-height:1.6;color:%s;">%s</p>
                </div>
                """.formatted(borderColor, backgroundColor, textColor, title, textColor, subtitle);
    }

    private String actionNote(String title, String text) {
        return """
                <div style="margin-top:18px;padding:18px 20px;border-radius:14px;background:#f8fafc;border:1px solid #e2e8f0;">
                  <p style="margin:0 0 8px;font-size:14px;font-weight:700;color:#334155;">%s</p>
                  <p style="margin:0;font-size:14px;line-height:1.6;color:#475569;">%s</p>
                </div>
                """.formatted(title, text);
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