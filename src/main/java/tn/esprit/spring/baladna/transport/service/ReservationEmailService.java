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
            DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

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
                        : "Départ";
                String arr = reservation.getTransport().getTrajet().getArrivalStation() != null
                        ? reservation.getTransport().getTrajet().getArrivalStation().getName()
                        : "Arrivée";
                route = dep + " → " + arr;
                departureStation = dep;
            }
            if (reservation.getTransport().getDepartureDate() != null) {
                departureDate = reservation.getTransport().getDepartureDate().format(DATE_FORMAT);
            }
        }

        String subject = "✅ Réservation confirmée — " + route;
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
                    : "Départ";
            String arr = reservation.getTransport().getTrajet().getArrivalStation() != null
                    ? reservation.getTransport().getTrajet().getArrivalStation().getName()
                    : "Arrivée";
            route = dep + " → " + arr;
        }

        String subject = "❌ Réservation refusée — " + route;
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
            log.info("[ReservationEmailService] ✅ Email envoyé à {}", to);
        } catch (MessagingException e) {
            log.error("[ReservationEmailService] ❌ Échec envoi à {}: {}", to, e.getMessage());
        }
    }

    private String buildApprovalHtml(String name, String ticketCode, String route,
                                     String departureDate, String departureStation,
                                     Integer seats, Double totalPrice, String boardingPoint) {
        return """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;background:#f9fafb;border-radius:12px;overflow:hidden;border:1px solid #e5e7eb">
              <div style="background:#1a3a5c;padding:32px;text-align:center">
                <h1 style="color:#fff;margin:0;font-size:26px">🎉 Réservation Confirmée</h1>
                <p style="color:#93c5fd;margin:8px 0 0;font-size:14px">Baladna Transport</p>
              </div>
              <div style="padding:32px">
                <p style="font-size:16px;margin-top:0">Bonjour <strong>%s</strong>,</p>
                <p style="color:#374151">Votre réservation a été <strong style="color:#16a34a">approuvée</strong> par le host. Vous pouvez maintenant monter à bord !</p>
                <div style="background:#fff;border:1px solid #e5e7eb;border-radius:8px;padding:20px;margin:20px 0">
                  <table style="width:100%%;border-collapse:collapse;font-size:15px">
                    <tr style="border-bottom:1px solid #f3f4f6">
                      <td style="padding:10px 0;color:#6b7280">Trajet</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #f3f4f6">
                      <td style="padding:10px 0;color:#6b7280">Date de départ</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #f3f4f6">
                      <td style="padding:10px 0;color:#6b7280">Station de départ</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #f3f4f6">
                      <td style="padding:10px 0;color:#6b7280">Point d'embarquement</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #f3f4f6">
                      <td style="padding:10px 0;color:#6b7280">Places réservées</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%d</td>
                    </tr>
                    <tr>
                      <td style="padding:10px 0;color:#6b7280">Prix total</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%.2f DT</td>
                    </tr>
                  </table>
                </div>
                <div style="background:#f0fdf4;border:2px solid #16a34a;border-radius:10px;padding:20px;text-align:center;margin:24px 0">
                  <p style="color:#6b7280;margin:0 0 8px;font-size:13px;text-transform:uppercase;letter-spacing:1px">Votre code ticket</p>
                  <p style="font-size:26px;font-weight:bold;color:#1a3a5c;letter-spacing:4px;margin:0;font-family:monospace">%s</p>
                  <p style="color:#6b7280;margin:10px 0 0;font-size:12px">Présentez ce code au host le jour du départ pour validation</p>
                </div>
                <p style="color:#9ca3af;font-size:12px;margin-top:32px;text-align:center">Merci de voyager avec Baladna 🌍</p>
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
                <h1 style="color:#fff;margin:0;font-size:26px">❌ Réservation Refusée</h1>
                <p style="color:#fca5a5;margin:8px 0 0;font-size:14px">Baladna Transport</p>
              </div>
              <div style="padding:32px">
                <p style="font-size:16px;margin-top:0">Bonjour <strong>%s</strong>,</p>
                <p style="color:#374151">Nous sommes désolés, votre demande pour le trajet <strong>%s</strong> a été <strong style="color:#dc2626">refusée</strong> par le host.</p>
                <div style="background:#fff;border:1px solid #e5e7eb;border-radius:8px;padding:20px;margin:20px 0">
                  <table style="width:100%%;border-collapse:collapse;font-size:15px">
                    <tr style="border-bottom:1px solid #f3f4f6">
                      <td style="padding:10px 0;color:#6b7280">Trajet</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%s</td>
                    </tr>
                    <tr style="border-bottom:1px solid #f3f4f6">
                      <td style="padding:10px 0;color:#6b7280">Places demandées</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%d</td>
                    </tr>
                    <tr>
                      <td style="padding:10px 0;color:#6b7280">Point d'embarquement</td>
                      <td style="padding:10px 0;font-weight:bold;text-align:right">%s</td>
                    </tr>
                  </table>
                </div>
                <p style="color:#374151">Les places ont été automatiquement libérées. Vous pouvez rechercher un autre transport disponible sur Baladna.</p>
                <p style="color:#9ca3af;font-size:12px;margin-top:32px;text-align:center">L'équipe Baladna 🌍</p>
              </div>
            </div>
            """.formatted(name, route, route,
                seats != null ? seats : 0,
                boardingPoint != null ? boardingPoint : "N/A");
    }
}