package tn.esprit.spring.baladna.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.user.dto.ChangePasswordRequest;
import tn.esprit.spring.baladna.user.dto.UpdateProfileRequest;
import tn.esprit.spring.baladna.user.dto.UpdateRoleRequest;
import tn.esprit.spring.baladna.user.dto.UpdateStatusRequest;
import tn.esprit.spring.baladna.user.entity.*;
import tn.esprit.spring.baladna.user.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    // ========== ADMIN ==========

    @GetMapping("/api/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/api/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/api/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(userService.updateStatus(id, request));
    }

    @PutMapping("/api/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> updateRole(
            @PathVariable Long id,
            @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(userService.updateRole(id, request));
    }
    @DeleteMapping("/api/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok("Utilisateur supprimé");
    }

    @DeleteMapping("/api/users/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> hardDeleteUser(@PathVariable Long id) {
        userService.hardDeleteUser(id);
        return ResponseEntity.ok("Utilisateur supprimé définitivement");
    }
    @GetMapping("/api/users/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsersIncludingDeleted() {
        return ResponseEntity.ok(userService.getAllUsersIncludingDeleted());
    }

    // ========== PROFILE (user connecté) ==========

    @GetMapping("/api/profile/me")
    public ResponseEntity<User> getMyProfile(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(userService.getMyProfile(email));
    }

    @PutMapping("/api/profile/me")
    public ResponseEntity<User> updateMyProfile(
            @AuthenticationPrincipal String email,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateMyProfile(email, request));
    }

    @GetMapping("/api/profile/me/activity")
    public ResponseEntity<List<ActivityLog>> getMyActivity(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(userService.getMyActivity(email));
    }

    @GetMapping("/api/users/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable Role role) {
        return ResponseEntity.ok(userService.getUsersByRole(role));
    }

    @GetMapping("/api/users/status/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getUsersByStatus(@PathVariable Status status) {
        return ResponseEntity.ok(userService.getUsersByStatus(status));
    }

    @GetMapping("/api/users/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String keyword) {
        return ResponseEntity.ok(userService.searchUsers(keyword));
    }

    @PutMapping("/api/profile/me/change-password")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal String email,
            @RequestBody ChangePasswordRequest request) {
        userService.changePassword(email, request);
        return ResponseEntity.ok("Mot de passe modifié avec succès");
    }

    @GetMapping("/api/profile/me/sessions")
    public ResponseEntity<List<Session>> getMySessions(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(userService.getMySessions(email));
    }

    @DeleteMapping("/api/profile/me/sessions")
    public ResponseEntity<String> logoutAllSessions(
            @AuthenticationPrincipal String email) {
        userService.logoutAllSessions(email);
        return ResponseEntity.ok("Toutes les sessions fermées");
    }

    @GetMapping("/api/users/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Long>> getUserStats() {
        return ResponseEntity.ok(userService.getUserStats());
    }

}
