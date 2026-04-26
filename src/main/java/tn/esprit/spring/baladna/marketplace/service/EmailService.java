package tn.esprit.spring.baladna.marketplace.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.admin.email}")
    private String adminEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendFactureEmail(String to, String subject, String body, byte[] pdf, String factureNumber) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);
            helper.addAttachment("Facture_" + factureNumber + ".pdf", new ByteArrayResource(pdf));
            mailSender.send(message);

            // Copie admin
            MimeMessage adminMsg = mailSender.createMimeMessage();
            MimeMessageHelper adminHelper = new MimeMessageHelper(adminMsg, true);
            adminHelper.setFrom(fromEmail);
            adminHelper.setTo(adminEmail);
            adminHelper.setSubject("Copie Facture " + factureNumber);
            adminHelper.setText("Facture " + factureNumber + " envoyée au client.");
            adminHelper.addAttachment("Facture_" + factureNumber + ".pdf", new ByteArrayResource(pdf));
            mailSender.send(adminMsg);

        } catch (MessagingException e) {
            throw new RuntimeException("Erreur envoi email", e);
        }
    }
}