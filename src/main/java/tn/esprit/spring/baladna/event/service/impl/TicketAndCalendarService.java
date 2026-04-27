package tn.esprit.spring.baladna.event.service.impl;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.event.entity.EventReservation;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class TicketAndCalendarService {

    public byte[] generateTicket(EventReservation reservation) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("EVENT TICKET").setBold().setFontSize(18));
            document.add(new Paragraph("Event: " + reservation.getEvent().getTitle()));
            document.add(new Paragraph("Date: " + reservation.getEvent().getStartAt()));
            document.add(new Paragraph("Location: " + reservation.getEvent().getLocation()));
            document.add(new Paragraph("Seats: " + reservation.getPersonsCount()));
            document.add(new Paragraph("Total: " + reservation.getTotalPrice() + " EUR"));

            document.close();

            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed");
        }
    }

    public byte[] generateICS(EventReservation reservation) {
        String content = "BEGIN:VCALENDAR\n" +
                "VERSION:2.0\n" +
                "BEGIN:VEVENT\n" +
                "SUMMARY:" + reservation.getEvent().getTitle() + "\n" +
                "DTSTART:" + formatDate(reservation.getEvent().getStartAt()) + "\n" +
                "LOCATION:" + reservation.getEvent().getLocation() + "\n" +
                "DESCRIPTION:Event Reservation\n" +
                "END:VEVENT\n" +
                "END:VCALENDAR";

        return content.getBytes(StandardCharsets.UTF_8);
    }

    private String formatDate(LocalDateTime date) {
        if (date == null) return "";
        // Format as YYYYMMDDTHHmmssZ
        return date.format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"));
    }
}