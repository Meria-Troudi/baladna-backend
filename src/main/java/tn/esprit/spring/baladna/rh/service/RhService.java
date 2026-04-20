package tn.esprit.spring.baladna.rh.service;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.spring.baladna.rh.dto.*;
import tn.esprit.spring.baladna.rh.entity.*;
import tn.esprit.spring.baladna.rh.repository.*;
import tn.esprit.spring.baladna.user.entity.User;
import tn.esprit.spring.baladna.user.repository.UserRepository;
import org.apache.pdfbox.Loader;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RhService {
    private final InterviewRepository interviewRepo;
    private final ApplicationRepository applicationRepo;
    private final AtsService atsService;
    private final UserRepository userRepo;

    private final String uploadDir = "uploads/cvs/";

    // ===== INTERVIEWS =====

    public List<Interview> getAllOpenInterviews() {
        return interviewRepo.findByStatus(InterviewStatus.OPEN);
    }

    public List<Interview> getAllInterviews() {
        return interviewRepo.findAll();
    }

    public Interview getInterviewById(Long id) {
        return interviewRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Interview not found"));
    }

    public Interview createInterview(InterviewRequest request) {
        Interview interview = Interview.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .department(request.getDepartment())
                .contractType(request.getContractType())
                .scheduledAt(request.getScheduledAt())
                .maxCandidates(request.getMaxCandidates())
                .requiredSkills(request.getRequiredSkills())
                .experienceYears(request.getExperienceYears())
                .status(InterviewStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();
        return interviewRepo.save(interview);
    }

    public Interview updateInterviewStatus(Long id, InterviewStatus status) {
        Interview interview = getInterviewById(id);
        interview.setStatus(status);
        return interviewRepo.save(interview);
    }

    public void deleteInterview(Long id) {
        interviewRepo.deleteById(id);
    }

    // ===== APPLICATIONS =====

    @Transactional
    public Application apply(
            ApplicationRequest request,
            MultipartFile cvFile,
            String userEmail  // null si non connecté
    ) throws IOException {
        if (request.getCin() == null ||
                !request.getCin().matches("\\d{8}")) {
            throw new RuntimeException("CIN invalide — doit contenir exactement 8 chiffres");
        }

        // Vérifier si déjà candidaté
        if (applicationRepo.existsByEmailAndInterviewId(
                request.getEmail(), request.getInterviewId())) {
            throw new RuntimeException("Vous avez déjà candidaté à cet entretien");
        }

        Interview interview = getInterviewById(request.getInterviewId());

        // ✅ Uploader le CV
        String cvPath = uploadCV(cvFile);

        // ✅ Extraire le texte du CV pour ATS
        String cvText = extractTextFromCV(cvFile);

        // ✅ Analyser avec ATS
        AtsResult atsResult = atsService.analyzeCV(cvText, interview);

        // ✅ Récupérer le user si connecté
        User user = null;
        if (userEmail != null) {
            user = userRepo.findByEmail(userEmail).orElse(null);
        }

        Application application = Application.builder()
                .interview(interview)
                .user(user)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .cin(request.getCin())
                .coverLetter(request.getCoverLetter())
                .cvPath(cvPath)
                .status(ApplicationStatus.PENDING)
                .atsScore(atsResult.getScore())
                .atsFeedback(atsResult.getFeedback())
                .appliedAt(LocalDateTime.now())
                .build();

        return applicationRepo.save(application);
    }

    public List<Application> getApplicationsByInterview(Long interviewId) {
        return applicationRepo.findByInterviewIdOrderByAtsScoreDesc(interviewId);
    }

    public Application updateApplicationStatus(Long id, ApplicationStatus status) {
        Application app = applicationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        app.setStatus(status);
        app.setUpdatedAt(LocalDateTime.now());
        return applicationRepo.save(app);
    }

    private String uploadCV(MultipartFile file) throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path path = Paths.get(uploadDir + fileName);
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    private String extractTextFromCV(MultipartFile file) {
        try {
            if (file.getOriginalFilename() != null &&
                    file.getOriginalFilename().endsWith(".pdf")) {

                // ✅ PDFBox 3.x utilise Loader.loadPDF() au lieu de PDDocument.load()
                PDDocument document = Loader.loadPDF(file.getBytes());
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                document.close();
                return text;
            }
            // Pour les fichiers Word (.doc, .docx)
            return new String(file.getBytes());
        } catch (Exception e) {
            return "";
        }
    }

}
