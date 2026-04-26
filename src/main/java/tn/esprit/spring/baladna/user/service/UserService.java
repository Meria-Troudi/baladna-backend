package tn.esprit.spring.baladna.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.spring.baladna.user.dto.ChangePasswordRequest;
import tn.esprit.spring.baladna.user.dto.UpdateProfileRequest;
import tn.esprit.spring.baladna.user.dto.UpdateRoleRequest;
import tn.esprit.spring.baladna.user.dto.UpdateStatusRequest;
import tn.esprit.spring.baladna.user.entity.*;
import tn.esprit.spring.baladna.user.repository.ActivityLogRepository;
import tn.esprit.spring.baladna.user.repository.SessionRepository;
import tn.esprit.spring.baladna.user.repository.UserRepository;
import java.nio.file.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;
    private final ActivityLogRepository logRepo;
    private final ActivityLogService logService;
    private final PasswordEncoder encoder;
    private final SessionRepository sessionRepo;
    private final String uploadDir = "uploads/photos/";
    private final String pythonProjectPath = "C:\\Users\\msi\\Desktop\\PI\\Face_recognition_python-main";
    private final ObjectMapper objectMapper = new ObjectMapper();
    // ✅ ADMIN - Liste tous les users
    //public List<User> getAllUsers() {
        //return userRepo.findAll();
   // }
    public List<User> getAllUsers() {
        return userRepo.findByStatusNot(Status.DELETED);
    }

    // ✅ ADMIN - Détail d'un user
    public User getUserById(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ✅ ADMIN - Changer le status (ACTIVE / SUSPENDED / DELETED)
    public User updateStatus(Long id, UpdateStatusRequest request) {
        User user = getUserById(id);
        user.setStatus(request.getStatus());
        logService.log("STATUS_CHANGED_TO_" + request.getStatus(), user);
        return userRepo.save(user);
    }

    // ✅ ADMIN - Changer le rôle
    public User updateRole(Long id, UpdateRoleRequest request) {
        User user = getUserById(id);
        user.setRole(request.getRole());
        logService.log("ROLE_CHANGED_TO_" + request.getRole(), user);
        return userRepo.save(user);
    }

    // ✅ ADMIN - Soft delete
    public void deleteUser(Long id) {
        User user = getUserById(id);
        user.setStatus(Status.DELETED);
        logService.log("DELETED", user);
        userRepo.save(user);
    }
    @Transactional
    public void hardDeleteUser(Long id) {
        User user = getUserById(id);

        // ✅ supprimer d'abord les données liées
        sessionRepo.deleteAllByUser(user);
        logRepo.deleteAllByUser(user);

        // ✅ ensuite supprimer le user
        userRepo.delete(user);
    }

    public List<User> getAllUsersIncludingDeleted() {
        return userRepo.findAll();
    }

    // ✅ USER - Voir son profil
    public User getMyProfile(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ✅ USER - Modifier son profil
    public User updateMyProfile(String email, UpdateProfileRequest request) {
        User user = getMyProfile(email);

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPreferredLanguage() != null) user.setPreferredLanguage(request.getPreferredLanguage());
        if (request.getProfilePhoto() != null) user.setProfilePhoto(request.getProfilePhoto());

        logService.log("PROFILE_UPDATED", user);
        return userRepo.save(user);
    }

    // ✅ USER - Voir son activity log
    public List<ActivityLog> getMyActivity(String email) {
        User user = getMyProfile(email);
        return logRepo.findByUserOrderByTimestampDesc(user);
    }

    // Filtrer par rôle
    public List<User> getUsersByRole(Role role) {
        return userRepo.findByRole(role);
    }

    // Filtrer par status
    public List<User> getUsersByStatus(Status status) {
        return userRepo.findByStatus(status);
    }

    // Recherche par nom
    public List<User> searchUsers(String keyword) {
        return userRepo.findByFirstNameContainingOrLastNameContaining(keyword, keyword);
    }

    //chnager mdp
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = getMyProfile(email);

        if (!encoder.matches(request.getOldPassword(), user.getPassword()))
            throw new RuntimeException("Ancien mot de passe incorrect");

        user.setPassword(encoder.encode(request.getNewPassword()));
        logService.log("PASSWORD_CHANGED", user);
        userRepo.save(user);
    }

    public List<Session> getMySessions(String email) {
        User user = getMyProfile(email);
        return sessionRepo.findByUserAndExpiresAtAfter(user, LocalDateTime.now());
    }

    @Transactional
    public void logoutAllSessions(String email) {
        User user = getMyProfile(email);
        sessionRepo.deleteAllByUser(user);
        logService.log("LOGOUT_ALL_SESSIONS", user);

    }

    public Map<String, Long> getUserStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", userRepo.count());
        stats.put("tourists", userRepo.countByRole(Role.TOURIST));
        stats.put("hosts", userRepo.countByRole(Role.HOST));
        stats.put("admins", userRepo.countByRole(Role.ADMIN));
        stats.put("artisans", userRepo.countByRole(Role.ARTISAN));
        stats.put("active", userRepo.countByStatus(Status.ACTIVE));
        stats.put("suspended", userRepo.countByStatus(Status.SUSPENDED));
        stats.put("deleted", userRepo.countByStatus(Status.DELETED));
        return stats;
    }

    public String uploadPhoto(String email, MultipartFile photo) throws IOException {

        // ✅ Vérifier le type de fichier
        String contentType = photo.getContentType();
        if (contentType == null ||
                (!contentType.equals("image/jpeg") &&
                        !contentType.equals("image/png") &&
                        !contentType.equals("image/jpg"))) {
            throw new RuntimeException("Format invalide — JPG ou PNG uniquement");
        }

        // ✅ Vérifier la taille (max 5MB)
        if (photo.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("Photo trop grande — maximum 5MB");
        }

        User user = getMyProfile(email);

        // ✅ Supprimer l'ancienne photo si elle existe
        if (user.getProfilePhoto() != null) {
            Path oldPath = Paths.get(uploadDir + user.getProfilePhoto());
            Files.deleteIfExists(oldPath);
        }

        // ✅ Sauvegarder la nouvelle photo
        Files.createDirectories(Paths.get(uploadDir));
        String fileName = UUID.randomUUID() + "_" +
                photo.getOriginalFilename().replaceAll("\\s+", "_");
        Path path = Paths.get(uploadDir + fileName);

        // ✅ Lire les bytes une seule fois pour éviter problème de stream consommé
        byte[] imageBytes = photo.getBytes();
        Files.write(path, imageBytes);

        // ✅ Copier vers le dossier Image/ du projet Python pour face recognition
        Path pythonImagePath = Paths.get(pythonProjectPath + "\\Image\\" + fileName);
        Files.createDirectories(pythonImagePath.getParent());
        Files.write(pythonImagePath, imageBytes);

        // ✅ Mettre à jour faces.json avec l'email de l'utilisateur
        Path facesJsonPath = Paths.get(pythonProjectPath + "\\faces.json");
        Map<String, String> faces = new HashMap<>();
        if (Files.exists(facesJsonPath)) {
            try {
                faces = objectMapper.readValue(facesJsonPath.toFile(), Map.class);
            } catch (IOException e) {
                System.err.println("Erreur lecture faces.json: " + e.getMessage());
            }
        }
        faces.put(email, "Image/" + fileName);
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(facesJsonPath.toFile(), faces);
            System.out.println("faces.json mis à jour pour: " + email);
        } catch (IOException e) {
            System.err.println("Erreur écriture faces.json: " + e.getMessage());
        }

        // ✅ Mettre à jour en base
        user.setProfilePhoto(fileName);
        userRepo.save(user);

        logService.log("PHOTO_UPDATED", user);
        return fileName;
    }

    public void deletePhoto(String email) {
        User user = getMyProfile(email);

        if (user.getProfilePhoto() != null) {
            String photoFileName = user.getProfilePhoto();
            try {
                // ✅ Supprimer du dossier local
                Path path = Paths.get(uploadDir + photoFileName);
                Files.deleteIfExists(path);

                // ✅ Supprimer du dossier Image/ du projet Python
                Path pythonImagePath = Paths.get(pythonProjectPath + "\\Image\\" + photoFileName);
                Files.deleteIfExists(pythonImagePath);

                // ✅ Supprimer l'entrée de faces.json
                Path facesJsonPath = Paths.get(pythonProjectPath + "\\faces.json");
                if (Files.exists(facesJsonPath)) {
                    Map<String, String> faces = objectMapper.readValue(facesJsonPath.toFile(), Map.class);
                    faces.remove(email);
                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(facesJsonPath.toFile(), faces);
                    System.out.println("Entrée supprimée de faces.json pour: " + email);
                }
            } catch (IOException e) {
                System.err.println("Erreur suppression photo: " + e.getMessage());
            }
            user.setProfilePhoto(null);
            userRepo.save(user);
            logService.log("PHOTO_DELETED", user);
        }
    }
}
