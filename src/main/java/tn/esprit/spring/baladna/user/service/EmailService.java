package tn.esprit.spring.baladna.user.service;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username:}}")
    private String fromAddress;

    public void sendResetPasswordEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        String from = (fromAddress != null && !fromAddress.isBlank())
                ? fromAddress
                : null;
        if (from != null) {
            message.setFrom(from);
        }
        message.setTo(to);
        message.setSubject("BALADNA - Réinitialisation de mot de passe");
        message.setText(
                "Bonjour,\n\n" +
                        "Vous avez demandé une réinitialisation de mot de passe.\n\n" +
                        "Cliquez sur ce lien pour réinitialiser votre mot de passe :\n" +
                        "http://localhost:4200/reset-password?token=" + token + "\n\n" +
                        "Ce lien expire dans 15 minutes.\n\n" +
                        "Si vous n'avez pas fait cette demande, ignorez cet email.\n\n" +
                        "L'équipe BALADNA"
        );
        mailSender.send(message);
    }
}
