package tn.esprit.spring.baladna.rh.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.spring.baladna.rh.dto.*;
import tn.esprit.spring.baladna.rh.entity.*;
import tn.esprit.spring.baladna.rh.service.RhService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/rh")
@RequiredArgsConstructor
public class RhController {
    private final RhService rhService;

    // ===== PUBLIC - pas besoin d'être connecté =====

    @GetMapping("/interviews")
    public ResponseEntity<List<Interview>> getOpenInterviews() {
        return ResponseEntity.ok(rhService.getAllOpenInterviews());
    }

    @GetMapping("/interviews/{id}")
    public ResponseEntity<Interview> getInterview(@PathVariable Long id) {
        return ResponseEntity.ok(rhService.getInterviewById(id));
    }

    @PostMapping(value = "/apply", consumes = "multipart/form-data")
    public ResponseEntity<Application> apply(
            @RequestPart("data") ApplicationRequest request,
            @RequestPart("cv") MultipartFile cvFile,
            @AuthenticationPrincipal String email  // null si non connecté
    ) throws IOException {
        return ResponseEntity.ok(rhService.apply(request, cvFile, email));
    }

    // ===== ADMIN =====

    @GetMapping("/admin/interviews")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Interview>> getAllInterviews() {
        return ResponseEntity.ok(rhService.getAllInterviews());
    }

    @PostMapping("/admin/interviews")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Interview> createInterview(
            @RequestBody InterviewRequest request) {
        return ResponseEntity.ok(rhService.createInterview(request));
    }

    @PutMapping("/admin/interviews/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Interview> updateStatus(
            @PathVariable Long id,
            @RequestParam InterviewStatus status) {
        return ResponseEntity.ok(rhService.updateInterviewStatus(id, status));
    }

    @DeleteMapping("/admin/interviews/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteInterview(@PathVariable Long id) {
        rhService.deleteInterview(id);
        return ResponseEntity.ok("Interview supprimé");
    }

    @GetMapping("/admin/interviews/{id}/applications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Application>> getApplications(@PathVariable Long id) {
        return ResponseEntity.ok(rhService.getApplicationsByInterview(id));
    }

    @PutMapping("/admin/applications/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Application> updateApplicationStatus(
            @PathVariable Long id,
            @RequestParam ApplicationStatus status) {
        return ResponseEntity.ok(rhService.updateApplicationStatus(id, status));
    }
}
