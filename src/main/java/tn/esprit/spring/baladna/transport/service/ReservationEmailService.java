package tn.esprit.spring.baladna.transport.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.transport.entity.Reservation;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationEmailService {

    private final JavaMailSender mailSender;
    private final ReservationTicketService reservationTicketService;

    @Value("${transport.mail.from:noreply@baladna.tn}")
    private String fromEmail;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Async
    public void sendPendingEmail(Reservation reservation) {
        if (reservation.getUser() == null || reservation.getUser().getEmail() == null) return;

        String to = reservation.getUser().getEmail();
        String passengerName = reservation.getUser().getFirstName()
                + " " + reservation.getUser().getLastName();

        String route = "N/A";
        String departureDate = "N/A";

        if (reservation.getTransport() != null) {
            if (reservation.getTransport().getTrajet() != null) {
                String dep = reservation.getTransport().getTrajet().getDepartureStation() != null
                        ? reservation.getTransport().getTrajet().getDepartureStation().getName()
                        : "Departure";
                String arr = reservation.getTransport().getTrajet().getArrivalStation() != null
                        ? reservation.getTransport().getTrajet().getArrivalStation().getName()
                        : "Arrival";
                route = dep + " → " + arr;
            }
            if (reservation.getTransport().getDepartureDate() != null) {
                departureDate = reservation.getTransport().getDepartureDate().format(DATE_FORMAT);
            }
        }

        String subject = "⏳ Reservation request received — " + route;
        String html = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;background:#f9fafb;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb">
              <div style="background:#1a3a5c;padding:32px;text-align:center">
                <h1 style="color:#fff;margin:0;font-size:26px">⏳ Request Received</h1>
                <p style="color:#93c5fd;margin:8px 0 0;font-size:14px">Baladna Transport</p>
              </div>
              <div style="padding:32px">
                <p style="font-size:16px;margin-top:0">Hello <strong>%s</strong>,</p>
                <p>Your reservation request has been received and is currently pending approval.</p>
                <div style="background:#fff;border:1px solid #e5e7eb;border-radius:8px;padding:20px;margin:20px 0">
                  <table style="width:100%%;border-collapse:collapse;font-size:15px">
                    <tr style="border-bottom:1px solid #f3f4f6">
                      <td style="padding:10px 0;color:#6b7280">Route</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #f3f4f6">
                      <td style="padding:10px 0;color:#6b7280">Departure Date</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #f3f4f6">
                      <td style="padding:10px 0;color:#6b7280">Requested Seats</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%d</td>
                    </tr>
                    <tr>
                      <td style="padding:10px 0;color:#6b7280">Total Price</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%.2f DT</td>
                    </tr>
                  </table>
                </div>
                <div style="background:#fef3c7;border:2px solid #f59e0b;border-radius:10px;padding:16px;text-align:center;margin:20px 0">
                  <p style="color:#92400e;margin:0;font-weight:bold">Pending Approval</p>
                  <p style="color:#92400e;margin:8px 0 0;font-size:13px">You will receive an email as soon as the host responds.</p>
                </div>
                <p style="color:#9ca3af;font-size:12px;margin-top:32px;text-align:center">Thank you for traveling with Baladna 🌍</p>
              </div>
            </div>
            """.formatted(
                passengerName, route, departureDate,
                reservation.getReservedSeats() != null ? reservation.getReservedSeats() : 0,
                reservation.getTotalPrice() != null ? reservation.getTotalPrice() : 0.0
        );

        sendHtml(to, subject, html);
    }

    @Async
    public void sendApprovalEmail(Reservation reservation) {
        if (reservation.getUser() == null || reservation.getUser().getEmail() == null) return;

        String to = reservation.getUser().getEmail();
        String passengerName = reservation.getUser().getFirstName()
                + " " + reservation.getUser().getLastName();
        String ticketCode = reservationTicketService.generateTicketCode(reservation);

        String route = "N/A";
        String departureDate = "N/A";
        String departureStation = "N/A";

        if (reservation.getTransport() != null) {
            if (reservation.getTransport().getTrajet() != null) {
                String dep = reservation.getTransport().getTrajet().getDepartureStation() != null
                        ? reservation.getTransport().getTrajet().getDepartureStation().getName()
                        : "Departure";
                String arr = reservation.getTransport().getTrajet().getArrivalStation() != null
                        ? reservation.getTransport().getTrajet().getArrivalStation().getName()
                        : "Arrival";
                route = dep + " → " + arr;
                departureStation = dep;
            }
            if (reservation.getTransport().getDepartureDate() != null) {
                departureDate = reservation.getTransport().getDepartureDate().format(DATE_FORMAT);
            }
        }

        String subject = "✅ Reservation confirmed — " + route;
        String html = buildApprovalHtml(
                passengerName, ticketCode, route, departureDate, departureStation,
                reservation.getReservedSeats(), reservation.getTotalPrice(),
                reservation.getBoardingPoint()
        );

        sendHtml(to, subject, html);
    }

    @Async
    public void sendRejectionEmail(Reservation reservation) {
        if (reservation.getUser() == null || reservation.getUser().getEmail() == null) return;

        String to = reservation.getUser().getEmail();
        String passengerName = reservation.getUser().getFirstName()
                + " " + reservation.getUser().getLastName();

        String route = "N/A";
        if (reservation.getTransport() != null
                && reservation.getTransport().getTrajet() != null) {
            String dep = reservation.getTransport().getTrajet().getDepartureStation() != null
                    ? reservation.getTransport().getTrajet().getDepartureStation().getName()
                    : "Departure";
            String arr = reservation.getTransport().getTrajet().getArrivalStation() != null
                    ? reservation.getTransport().getTrajet().getArrivalStation().getName()
                    : "Arrival";
            route = dep + " → " + arr;
        }

        String subject = "❌ Reservation rejected — " + route;
        String html = buildRejectionHtml(
                passengerName, route,
                reservation.getReservedSeats(), reservation.getBoardingPoint()
        );

        sendHtml(to, subject, html);
    }

    private void sendHtml(String to, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("[ReservationEmailService] Email sent to {}", to);
        } catch (MessagingException e) {
            log.error("[ReservationEmailService] Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    private String buildApprovalHtml(String name, String ticketCode, String route,
                                     String departureDate, String departureStation,
                                     Integer seats, Double totalPrice, String boardingPoint) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;background:#f9fafb;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb">
              <div style="background:#1a3a5c;padding:32px;text-align:center">
                <h1 style="color:#fff;margin:0;font-size:26px">🎉 Reservation Confirmed</h1>
                <p style="color:#93c5fd;margin:8px 0 0;font-size:14px">Baladna Transport</p>
              </div>
              <div style="padding:32px">
                <p style="font-size:16px;margin-top:0">Hello <strong>%s</strong>,</p>
                <p style="color:#374151">Your reservation has been <strong style="color:#16a34a">approved</strong> by the host. You can now board the transport.</p>
                <div style="background:#fff;border:1px solid #e5e7eb;border-radius:8px;padding:20px;margin:20px 0">
                  <table style="width:100%%;border-collapse:collapse;font-size:15px">
                    <tr style="border-bottom:1px solid #f3f4f6">
                      <td style="padding:10px 0;color:#6b7280">Route</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #f3f4f6">
                      <td style="padding:10px 0;color:#6b7280">Departure Date</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #f3f4f6">
                      <td style="padding:10px 0;color:#6b7280">Departure Station</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #f3f4f6">
                      <td style="padding:10px 0;color:#6b7280">Boarding Point</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #f3f4f6">
                      <td style="padding:10px 0;color:#6b7280">Reserved Seats</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%d</td>
                    </tr>
                    <tr>
                      <td style="padding:10px 0;color:#6b7280">Total Price</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%.2f DT</td>
                    </tr>
                  </table>
                </div>
                <div style="background:#f0fdf4;border:2px solid #16a34a;border-radius:10px;padding:20px;text-align:center;margin:24px 0">
                  <p style="color:#6b7280;margin:0 0 8px;font-size:13px;text-transform:uppercase;letter-spacing:1px">Your Ticket Code</p>
                  <p style="font-size:26px;font-weight:bold;color:#1a3a5c;letter-spacing:4px;margin:0;font-family:monospace">%s</p>
                  <p style="color:#6b7280;margin:10px 0 0;font-size:12px">Present this code to the host on departure day for validation.</p>
                </div>
                <p style="color:#9ca3af;font-size:12px;margin-top:32px;text-align:center">Thank you for traveling with Baladna 🌍</p>
              </div>
            </div>
            """.formatted(name, route, departureDate, departureStation,
                boardingPoint != null ? boardingPoint : "N/A",
                seats != null ? seats : 0,
                totalPrice != null ? totalPrice : 0.0,
                ticketCode);
    }

    private String buildRejectionHtml(String name, String route,
                                      Integer seats, String boardingPoint) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;background:#f9fafb;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb">
              <div style="background:#7f1d1d;padding:32px;text-align:center">
                <h1 style="color:#fff;margin:0;font-size:26px">❌ Reservation Rejected</h1>
                <p style="color:#fca5a5;margin:8px 0 0;font-size:14px">Baladna Transport</p>
              </div>
              <div style="padding:32px">
                <p style="font-size:16px;margin-top:0">Hello <strong>%s</strong>,</p>
                <p style="color:#374151">We are sorry, but your request for route <strong>%s</strong> has been <strong style="color:#dc2626">rejected</strong> by the host.</p>
                <div style="background:#fff;border:1px solid #e5e7eb;border-radius:8px;padding:20px;margin:20px 0">
                  <table style="width:100%%;border-collapse:collapse;font-size:15px">
                    <tr style="border-bottom:1px solid #f3f4f6">
                      <td style="padding:10px 0;color:#6b7280">Route</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #f3f4f6">
                      <td style="padding:10px 0;color:#6b7280">Requested Seats</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%d</td>
                    </tr>
                    <tr>
                      <td style="padding:10px 0;color:#6b7280">Boarding Point</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%s</td>
                    </tr>
                  </table>
                </div>
                <p style="color:#374151">The seats have been released automatically. You can search for another available transport in Baladna.</p>
                <p style="color:#9ca3af;font-size:12px;margin-top:32px;text-align:center">The Baladna Team 🌍</p>
              </div>
            </div>
            """.formatted(name, route, route,
                seats != null ? seats : 0,
                boardingPoint != null ? boardingPoint : "N/A");
    }
}