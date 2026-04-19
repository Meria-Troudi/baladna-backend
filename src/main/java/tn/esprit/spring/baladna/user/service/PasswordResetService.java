package tn.esprit.spring.baladna.user.service;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.spring.baladna.user.dto.ForgotPasswordRequest;
import tn.esprit.spring.baladna.user.dto.ResetPasswordRequest;
import tn.esprit.spring.baladna.user.entity.PasswordResetToken;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.PasswordResetTokenRepository;
import tn.esprit.spring.baladna.user.repository.UserRepository;


import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final UserRepository userRepo;
    private final PasswordResetTokenRepository tokenRepo;
    private final EmailService emailService;
    private final PasswordEncoder encoder;
    private final ActivityLogService logService;

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email non trouvé"));

        // supprimer les anciens tokens
        tokenRepo.deleteAllByUser_Id(user.getId());

        // créer un nouveau token
        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        tokenRepo.save(resetToken);

        // envoyer l'email
        emailService.sendResetPasswordEmail(user.getEmail(), token);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = tokenRepo.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Token invalide"));

        if (resetToken.isUsed())
            throw new RuntimeException("Token déjà utilisé");

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new RuntimeException("Token expiré");

        User user = resetToken.getUser();
        user.setPassword(encoder.encode(request.getNewPassword()));
        userRepo.save(user);

        resetToken.setUsed(true);
        tokenRepo.save(resetToken);

        logService.log("PASSWORD_RESET", user);
    }
}
