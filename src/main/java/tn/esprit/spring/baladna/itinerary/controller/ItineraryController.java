package tn.esprit.spring.baladna.itinerary.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.spring.baladna.itinerary.dto.*;
import tn.esprit.spring.baladna.itinerary.entity.enums.CollaboratorRole;
import tn.esprit.spring.baladna.itinerary.service.ItineraryService;
import tn.esprit.spring.baladna.itinerary.util.SecurityUtils;

import java.util.List;
import java.util.UUID;

/**
 * All endpoints require a valid JWT in the Authorization header:
 *   Authorization: Bearer <token>
 *
 * The currently logged-in user's ID is extracted automatically
 * from the security context via SecurityUtils.
 */
@RestController
@RequestMapping("/api/itineraries")
@RequiredArgsConstructor
public class ItineraryController {

    private final ItineraryService itineraryService;
    private final SecurityUtils securityUtils;

    // ─────────────────────────────────────────────
    // ITINERARY CRUD
    // ─────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<ItineraryResponse> create(
            @Valid @RequestBody ItineraryRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itineraryService.createItinerary(request, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItineraryResponse> getById(@PathVariable UUID id) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(itineraryService.getItineraryById(id, userId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ItineraryResponse>> getMyItineraries() {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(itineraryService.getMyItineraries(userId));
    }

    @GetMapping("/public")
    public ResponseEntity<List<ItineraryResponse>> getPublic() {
        return ResponseEntity.ok(itineraryService.getPublicItineraries());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItineraryResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody ItineraryRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(itineraryService.updateItinerary(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        Long userId = securityUtils.getCurrentUserId();
        itineraryService.deleteItinerary(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────
    // COLLABORATOR MANAGEMENT
    // ─────────────────────────────────────────────

    @PostMapping("/{id}/collaborators/invite")
    public ResponseEntity<CollaboratorResponse> invite(
            @PathVariable UUID id,
            @RequestParam Long targetUserId,
            @RequestParam(defaultValue = "EDITOR") CollaboratorRole role) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itineraryService.inviteCollaborator(id, targetUserId, role, userId));
    }

    @PostMapping("/{id}/collaborators/request")
    public ResponseEntity<CollaboratorResponse> requestToJoin(@PathVariable UUID id) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itineraryService.requestToJoin(id, userId));
    }

    @PatchMapping("/{id}/collaborators/{collaboratorId}/approve")
    public ResponseEntity<CollaboratorResponse> approve(
            @PathVariable UUID id,
            @PathVariable UUID collaboratorId) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(itineraryService.approveRequest(id, collaboratorId, userId));
    }

    @PatchMapping("/{id}/collaborators/{collaboratorId}/reject")
    public ResponseEntity<CollaboratorResponse> reject(
            @PathVariable UUID id,
            @PathVariable UUID collaboratorId) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(itineraryService.rejectRequest(id, collaboratorId, userId));
    }

    @DeleteMapping("/{id}/collaborators/{collaboratorId}")
    public ResponseEntity<Void> removeCollaborator(
            @PathVariable UUID id,
            @PathVariable UUID collaboratorId) {
        Long userId = securityUtils.getCurrentUserId();
        itineraryService.removeCollaborator(id, collaboratorId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/collaborators")
    public ResponseEntity<List<CollaboratorResponse>> getCollaborators(
            @PathVariable UUID id) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(itineraryService.getCollaborators(id, userId));
    }

    @GetMapping("/{id}/collaborators/pending")
    public ResponseEntity<List<CollaboratorResponse>> getPending(
            @PathVariable UUID id) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(itineraryService.getPendingRequests(id, userId));
    }

    // ─────────────────────────────────────────────
    // ITINERARY STEPS
    // ─────────────────────────────────────────────

    @PostMapping("/{id}/steps")
    public ResponseEntity<ItineraryStepResponse> addStep(
            @PathVariable UUID id,
            @Valid @RequestBody ItineraryStepRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itineraryService.addStep(id, request, userId));
    }

    @GetMapping("/{id}/steps")
    public ResponseEntity<List<ItineraryStepResponse>> getSteps(
            @PathVariable UUID id) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(itineraryService.getSteps(id, userId));
    }

    @PutMapping("/{id}/steps/{stepId}")
    public ResponseEntity<ItineraryStepResponse> updateStep(
            @PathVariable UUID id,
            @PathVariable UUID stepId,
            @Valid @RequestBody ItineraryStepRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(itineraryService.updateStep(id, stepId, request, userId));
    }

    @DeleteMapping("/{id}/steps/{stepId}")
    public ResponseEntity<Void> deleteStep(
            @PathVariable UUID id,
            @PathVariable UUID stepId) {
        Long userId = securityUtils.getCurrentUserId();
        itineraryService.deleteStep(id, stepId, userId);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────
    // EXPENSES
    // ─────────────────────────────────────────────

    @PostMapping("/{id}/expenses")
    public ResponseEntity<ExpenseResponse> addExpense(
            @PathVariable UUID id,
            @Valid @RequestBody ExpenseRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itineraryService.addExpense(id, request, userId));
    }

    @GetMapping("/{id}/expenses")
    public ResponseEntity<List<ExpenseResponse>> getExpenses(
            @PathVariable UUID id) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(itineraryService.getExpenses(id, userId));
    }

    @PutMapping("/{id}/expenses/{expenseId}")
    public ResponseEntity<ExpenseResponse> updateExpense(
            @PathVariable UUID id,
            @PathVariable UUID expenseId,
            @Valid @RequestBody ExpenseRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(itineraryService.updateExpense(id, expenseId, request, userId));
    }

    @DeleteMapping("/{id}/expenses/{expenseId}")
    public ResponseEntity<Void> deleteExpense(
            @PathVariable UUID id,
            @PathVariable UUID expenseId) {
        Long userId = securityUtils.getCurrentUserId();
        itineraryService.deleteExpense(id, expenseId, userId);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────
    // SETTLEMENT
    // ─────────────────────────────────────────────

    @PostMapping("/{id}/settlement/compute")
    public ResponseEntity<List<SettlementSummaryResponse>> computeSettlement(
            @PathVariable UUID id) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(itineraryService.computeSettlement(id, userId));
    }

    @GetMapping("/{id}/settlement")
    public ResponseEntity<List<SettlementSummaryResponse>> getSettlement(
            @PathVariable UUID id) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(itineraryService.getSettlement(id, userId));
    }

    @PatchMapping("/{id}/settlement/{settlementId}/paid")
    public ResponseEntity<SettlementResponse> markPaid(
            @PathVariable UUID id,
            @PathVariable UUID settlementId) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(itineraryService.markSettlementPaid(id, settlementId, userId));
    }
}