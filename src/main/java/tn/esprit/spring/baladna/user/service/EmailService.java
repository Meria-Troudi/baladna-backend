package tn.esprit.spring.baladna.user.service;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendResetPasswordEmail(String to, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("BALADNA — Reset Your Password");
        message.setText(
                "Hello,\n\n" +
                        "You have requested a password reset for your BALADNA account.\n\n" +
                        "Click the link below to reset your password:\n" +
                        "http://localhost:4200/reset-password?token=" + token + "\n\n" +
                        "This link expires in 15 minutes.\n\n" +
                        "If you did not request this, please ignore this email.\n\n" +
                        "The BALADNA Team"
        );
        mailSender.send(message);
    }

    public void sendWelcomeEmail(String to, String firstName, String tempPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("BALADNA — Your Account Has Been Created");
        message.setText(
                "Hello " + firstName + ",\n\n" +
                        "Your application has been received on BALADNA.\n\n" +
                        "An account has been automatically created for you:\n" +
                        "Email    : " + to + "\n" +
                        "Temporary Password : " + tempPassword + "\n\n" +
                        "Log in at: http://localhost:4200/login\n" +
                        "Please change your password after logging in.\n\n" +
                        "The BALADNA Team"
        );
        mailSender.send(message);
    }
}
