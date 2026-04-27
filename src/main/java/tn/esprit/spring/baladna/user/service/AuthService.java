package tn.esprit.spring.baladna.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.esprit.spring.baladna.user.dto.AuthResponse;
import tn.esprit.spring.baladna.user.dto.FaceLoginRequest;
import tn.esprit.spring.baladna.user.dto.LoginRequest;
import tn.esprit.spring.baladna.user.dto.RefreshRequest;
import tn.esprit.spring.baladna.user.dto.RegisterRequest;
import tn.esprit.spring.baladna.user.entity.Role;
import tn.esprit.spring.baladna.user.entity.Session;
import tn.esprit.spring.baladna.user.entity.Status;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.SessionRepository;
import tn.esprit.spring.baladna.user.repository.UserRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final SessionRepository sessionRepo;
    private final JwtService jwtService;
    private final PasswordEncoder encoder;
    private final ActivityLogService logService;

    public AuthResponse register(RegisterRequest request) {

        // ✅ vérifier si email déjà utilisé
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(encoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : Role.TOURIST)
                .preferredLanguage(request.getPreferredLanguage())
                .status(Status.ACTIVE)
                .build();

        userRepo.save(user);
        logService.log("REGISTER", user);
        return generateTokens(user);
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!encoder.matches(request.getPassword(), user.getPassword()))
            throw new RuntimeException("Wrong password");

        logService.log("LOGIN", user);
        return generateTokens(user);
    }

    public AuthResponse faceLogin(FaceLoginRequest request) {
        String email = request.getEmail();
        String capturedImage = request.getImage();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé: " + email));

        if (user.getProfilePhoto() == null) {
            throw new RuntimeException("Cet utilisateur n'a pas de photo de profil. Veuillez en ajouter une d'abord.");
        }

        String uploadDir = "C:\\Users\\msi\\Desktop\\complet\\baladna-backend\\uploads\\photos\\";
        String profilePhotoPath = uploadDir + user.getProfilePhoto();

        if (!Files.exists(Paths.get(profilePhotoPath))) {
            throw new RuntimeException("Photo de profil introuvable. Veuillez recharger votre photo.");
        }

        try {
            byte[] profilePhotoBytes = Files.readAllBytes(Paths.get(profilePhotoPath));
            String profilePhotoBase64 = Base64.getEncoder().encodeToString(profilePhotoBytes);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = Map.of(
                "image1", capturedImage,
                "image2", profilePhotoBase64
            );
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

            Map<String, Object> response = restTemplate.postForObject(
                "http://localhost:8000/compare",
                entity,
                Map.class
            );

            System.out.println("=== FACE LOGIN DEBUG ===");
            System.out.println("Response: " + response);
            System.out.println("========================");

            Boolean match = (Boolean) response.get("match");
            Double similarity = response.containsKey("similarity") 
                ? ((Number) response.get("similarity")).doubleValue() 
                : 0;

            System.out.println("Match: " + match + ", Similarity: " + similarity + "%");

            if (match == null || !match) {
                throw new RuntimeException("Visage non correspondant. Similarité: " + similarity + "%");
            }

            logService.log("FACE_LOGIN", user);
            return generateTokens(user);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erreur reconnaissance faciale: " + e.getMessage());
        }
    }

    // ✅ REFRESH TOKEN
    public AuthResponse refreshToken(RefreshRequest request) {

        Session session = sessionRepo.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Session invalide"));

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            sessionRepo.delete(session);
            throw new RuntimeException("Session expirée, reconnectez-vous");
        }

        String newAccessToken = jwtService.generateToken(session.getUser());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(session.getToken())
                .role(session.getUser().getRole())
                .id(session.getUser().getId())
                .build();
    }

    // ✅ LOGOUT
    public void logout(RefreshRequest request) {
        sessionRepo.findByToken(request.getRefreshToken())
                .ifPresent(session -> {
                    logService.log("LOGOUT", session.getUser());
                    sessionRepo.delete(session);
                });
    }

    // ✅ une seule méthode generateTokens — la version dupliquée supprimée
    private AuthResponse generateTokens(User user) {

        String accessToken = jwtService.generateToken(user);
        String refreshToken = UUID.randomUUID().toString();

        Session session = Session.builder()
                .token(refreshToken)
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        sessionRepo.save(session);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .id(user.getId())
                .build();
    }
}