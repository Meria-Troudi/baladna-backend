package tn.esprit.spring.baladna.itinerary.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.spring.baladna.itinerary.dto.*;
import tn.esprit.spring.baladna.itinerary.entity.*;
import tn.esprit.spring.baladna.itinerary.entity.enums.*;
import tn.esprit.spring.baladna.itinerary.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItineraryService {

    private final ItineraryRepository itineraryRepository;
    private final ItineraryCollaboratorRepository collaboratorRepository;
    private final ItineraryStepRepository stepRepository;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementSummaryRepository summaryRepository;

    // ─────────────────────────────────────────────
    // ITINERARY CRUD
    // ─────────────────────────────────────────────

    @Transactional
    public ItineraryResponse createItinerary(ItineraryRequest request, Long ownerId) {
        Itinerary itinerary = Itinerary.builder()
                .ownerId(ownerId)
                .title(request.getTitle())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .destinationRegion(request.getDestinationRegion())
                .estimatedBudget(request.getEstimatedBudget())
                .status(request.getStatus() != null ? request.getStatus() : ItineraryStatus.DRAFT)
                .visibility(request.getVisibility() != null ? request.getVisibility() : Visibility.PRIVATE)
                .build();

        itinerary = itineraryRepository.save(itinerary);

        // Owner is automatically an ACTIVE collaborator with OWNER role
        ItineraryCollaborator ownerCollab = ItineraryCollaborator.builder()
                .itinerary(itinerary)
                .userId(ownerId)
                .role(CollaboratorRole.OWNER)
                .status(CollaboratorStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();
        collaboratorRepository.save(ownerCollab);

        return mapToResponse(itinerary);
    }

    public ItineraryResponse getItineraryById(UUID id, Long requestingUserId) {
        Itinerary itinerary = findItineraryOrThrow(id);
        assertCanView(itinerary, requestingUserId);
        return mapToResponse(itinerary);
    }

    public List<ItineraryResponse> getMyItineraries(Long userId) {
        return itineraryRepository.findAllAccessibleByUser(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ItineraryResponse> getPublicItineraries() {
        return itineraryRepository.findByVisibility(Visibility.PUBLIC)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ItineraryResponse updateItinerary(UUID id, ItineraryRequest request, Long requestingUserId) {
        Itinerary itinerary = findItineraryOrThrow(id);
        assertIsOwner(itinerary, requestingUserId);

        itinerary.setTitle(request.getTitle());
        itinerary.setDescription(request.getDescription());
        itinerary.setStartDate(request.getStartDate());
        itinerary.setEndDate(request.getEndDate());
        itinerary.setDestinationRegion(request.getDestinationRegion());
        itinerary.setEstimatedBudget(request.getEstimatedBudget());
        if (request.getStatus() != null) itinerary.setStatus(request.getStatus());
        if (request.getVisibility() != null) itinerary.setVisibility(request.getVisibility());

        return mapToResponse(itineraryRepository.save(itinerary));
    }

    @Transactional
    public void deleteItinerary(UUID id, Long requestingUserId) {
        Itinerary itinerary = findItineraryOrThrow(id);
        assertIsOwner(itinerary, requestingUserId);
        itineraryRepository.delete(itinerary);
    }

    // ─────────────────────────────────────────────
    // COLLABORATOR MANAGEMENT
    // ─────────────────────────────────────────────

    // Owner invites a user directly → ACTIVE immediately
    @Transactional
    public CollaboratorResponse inviteCollaborator(UUID itineraryId, Long targetUserId,
                                                   CollaboratorRole role, Long requestingUserId) {
        Itinerary itinerary = findItineraryOrThrow(itineraryId);
        assertIsOwner(itinerary, requestingUserId);

        // Check not already a collaborator
        collaboratorRepository.findByItineraryIdAndUserId(itineraryId, targetUserId)
                .ifPresent(c -> { throw new IllegalStateException("User is already a collaborator"); });

        ItineraryCollaborator collab = ItineraryCollaborator.builder()
                .itinerary(itinerary)
                .userId(targetUserId)
                .role(role != null ? role : CollaboratorRole.EDITOR)
                .status(CollaboratorStatus.ACTIVE)
                .joinedAt(LocalDateTime.now())
                .build();

        return mapToCollaboratorResponse(collaboratorRepository.save(collab));
    }

    // User requests to join a PUBLIC itinerary → PENDING
    @Transactional
    public CollaboratorResponse requestToJoin(UUID itineraryId, Long requestingUserId) {
        Itinerary itinerary = findItineraryOrThrow(itineraryId);

        if (itinerary.getVisibility() != Visibility.PUBLIC) {
            throw new IllegalStateException("This itinerary is private");
        }

        // Check not already a collaborator
        collaboratorRepository.findByItineraryIdAndUserId(itineraryId, requestingUserId)
                .ifPresent(c -> { throw new IllegalStateException("You already have a pending or active request"); });

        ItineraryCollaborator collab = ItineraryCollaborator.builder()
                .itinerary(itinerary)
                .userId(requestingUserId)
                .role(CollaboratorRole.VIEWER)
                .status(CollaboratorStatus.PENDING)
                .build();

        return mapToCollaboratorResponse(collaboratorRepository.save(collab));
    }

    // Owner approves a PENDING request
    @Transactional
    public CollaboratorResponse approveRequest(UUID itineraryId, UUID collaboratorId, Long requestingUserId) {
        Itinerary itinerary = findItineraryOrThrow(itineraryId);
        assertIsOwner(itinerary, requestingUserId);

        ItineraryCollaborator collab = collaboratorRepository.findById(collaboratorId)
                .orElseThrow(() -> new NoSuchElementException("Collaborator request not found"));

        if (collab.getStatus() != CollaboratorStatus.PENDING) {
            throw new IllegalStateException("Request is not in PENDING state");
        }

        collab.setStatus(CollaboratorStatus.ACTIVE);
        collab.setRole(CollaboratorRole.EDITOR);
        collab.setJoinedAt(LocalDateTime.now());

        return mapToCollaboratorResponse(collaboratorRepository.save(collab));
    }

    // Owner rejects a PENDING request
    @Transactional
    public CollaboratorResponse rejectRequest(UUID itineraryId, UUID collaboratorId, Long requestingUserId) {
        Itinerary itinerary = findItineraryOrThrow(itineraryId);
        assertIsOwner(itinerary, requestingUserId);

        ItineraryCollaborator collab = collaboratorRepository.findById(collaboratorId)
                .orElseThrow(() -> new NoSuchElementException("Collaborator request not found"));

        collab.setStatus(CollaboratorStatus.REJECTED);
        return mapToCollaboratorResponse(collaboratorRepository.save(collab));
    }

    // Owner removes an ACTIVE collaborator
    @Transactional
    public void removeCollaborator(UUID itineraryId, UUID collaboratorId, Long requestingUserId) {
        Itinerary itinerary = findItineraryOrThrow(itineraryId);
        assertIsOwner(itinerary, requestingUserId);

        ItineraryCollaborator collab = collaboratorRepository.findById(collaboratorId)
                .orElseThrow(() -> new NoSuchElementException("Collaborator not found"));

        if (collab.getRole() == CollaboratorRole.OWNER) {
            throw new IllegalStateException("Cannot remove the owner");
        }

        collab.setStatus(CollaboratorStatus.REMOVED);
        collaboratorRepository.save(collab);
    }

    public List<CollaboratorResponse> getCollaborators(UUID itineraryId, Long requestingUserId) {
        Itinerary itinerary = findItineraryOrThrow(itineraryId);
        assertCanView(itinerary, requestingUserId);
        return collaboratorRepository.findByItineraryId(itineraryId)
                .stream()
                .map(this::mapToCollaboratorResponse)
                .collect(Collectors.toList());
    }

    public List<CollaboratorResponse> getPendingRequests(UUID itineraryId, Long requestingUserId) {
        Itinerary itinerary = findItineraryOrThrow(itineraryId);
        assertIsOwner(itinerary, requestingUserId);
        return collaboratorRepository.findByItineraryIdAndStatus(itineraryId, CollaboratorStatus.PENDING)
                .stream()
                .map(this::mapToCollaboratorResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // ITINERARY STEPS CRUD
    // ─────────────────────────────────────────────

    @Transactional
    public ItineraryStepResponse addStep(UUID itineraryId, ItineraryStepRequest request, Long requestingUserId) {
        Itinerary itinerary = findItineraryOrThrow(itineraryId);
        assertIsActiveCollaborator(itineraryId, requestingUserId);

        // Auto-set position at end if not provided
        int position = request.getPosition() != null
                ? request.getPosition()
                : stepRepository.findMaxPositionByItineraryId(itineraryId) + 1;

        ItineraryStep step = ItineraryStep.builder()
                .itinerary(itinerary)
                .addedByUserId(requestingUserId)
                .serviceType(request.getServiceType())
                .serviceRefId(request.getServiceRefId())
                .title(request.getTitle())
                .notes(request.getNotes())
                .plannedDate(request.getPlannedDate())
                .position(position)
                .estimatedCost(request.getEstimatedCost())
                .actualCost(request.getActualCost())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        return mapToStepResponse(stepRepository.save(step));
    }

    public List<ItineraryStepResponse> getSteps(UUID itineraryId, Long requestingUserId) {
        Itinerary itinerary = findItineraryOrThrow(itineraryId);
        assertCanView(itinerary, requestingUserId);
        return stepRepository.findByItineraryIdOrderByPositionAsc(itineraryId)
                .stream()
                .map(this::mapToStepResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ItineraryStepResponse updateStep(UUID itineraryId, UUID stepId,
                                            ItineraryStepRequest request, Long requestingUserId) {
        findItineraryOrThrow(itineraryId);
        assertIsActiveCollaborator(itineraryId, requestingUserId);

        ItineraryStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new NoSuchElementException("Step not found"));

        step.setServiceType(request.getServiceType());
        step.setServiceRefId(request.getServiceRefId());
        step.setTitle(request.getTitle());
        step.setNotes(request.getNotes());
        step.setPlannedDate(request.getPlannedDate());
        if (request.getPosition() != null) step.setPosition(request.getPosition());
        step.setEstimatedCost(request.getEstimatedCost());
        step.setActualCost(request.getActualCost());
        step.setLatitude(request.getLatitude());
        step.setLongitude(request.getLongitude());

        return mapToStepResponse(stepRepository.save(step));
    }

    @Transactional
    public void deleteStep(UUID itineraryId, UUID stepId, Long requestingUserId) {
        findItineraryOrThrow(itineraryId);
        assertIsActiveCollaborator(itineraryId, requestingUserId);
        ItineraryStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new NoSuchElementException("Step not found"));
        stepRepository.delete(step);
    }

    // ─────────────────────────────────────────────
    // EXPENSE CRUD
    // ─────────────────────────────────────────────

    @Transactional
    public ExpenseResponse addExpense(UUID itineraryId, ExpenseRequest request, Long requestingUserId) {
        Itinerary itinerary = findItineraryOrThrow(itineraryId);
        assertIsActiveCollaborator(itineraryId, requestingUserId);

        Expense expense = Expense.builder()
                .itinerary(itinerary)
                .paidByUserId(requestingUserId)
                .category(request.getCategory())
                .amount(request.getAmount())
                .description(request.getDescription())
                .expenseDate(request.getExpenseDate())
                .build();

        return mapToExpenseResponse(expenseRepository.save(expense));
    }

    public List<ExpenseResponse> getExpenses(UUID itineraryId, Long requestingUserId) {
        Itinerary itinerary = findItineraryOrThrow(itineraryId);
        assertCanView(itinerary, requestingUserId);
        return expenseRepository.findByItineraryId(itineraryId)
                .stream()
                .map(this::mapToExpenseResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ExpenseResponse updateExpense(UUID itineraryId, UUID expenseId,
                                         ExpenseRequest request, Long requestingUserId) {
        findItineraryOrThrow(itineraryId);
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new NoSuchElementException("Expense not found"));

        // Only the person who logged the expense can edit it
        if (!expense.getPaidByUserId().equals(requestingUserId)) {
            throw new IllegalStateException("You can only edit your own expenses");
        }

        expense.setCategory(request.getCategory());
        expense.setAmount(request.getAmount());
        expense.setDescription(request.getDescription());
        expense.setExpenseDate(request.getExpenseDate());

        return mapToExpenseResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void deleteExpense(UUID itineraryId, UUID expenseId, Long requestingUserId) {
        findItineraryOrThrow(itineraryId);
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new NoSuchElementException("Expense not found"));

        if (!expense.getPaidByUserId().equals(requestingUserId)) {
            throw new IllegalStateException("You can only delete your own expenses");
        }

        expenseRepository.delete(expense);
    }
    /**
     * Clean up duplicate PAID settlements only
     * Keep all PAID as historical record, only recreate PENDING
     */
    private void cleanupDuplicateSettlements(UUID itineraryId) {
        List<Settlement> allSettlements = settlementRepository.findByItineraryId(itineraryId);

        // Only clean up duplicate PAID settlements (historical cleanup)
        // Keep all PAID as they are
        Map<String, List<Settlement>> paidGroups = new HashMap<>();
        for (Settlement s : allSettlements) {
            if (s.getStatus() == SettlementStatus.PAID) {
                String key = s.getDebtorUserId() + "-" + s.getCreditorUserId();
                paidGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
            }
        }

        // For each (debtor, creditor) pair with multiple PAID, delete all but the first
        List<UUID> toDelete = new ArrayList<>();
        for (List<Settlement> group : paidGroups.values()) {
            if (group.size() > 1) {
                // Keep first PAID (oldest), delete the rest
                for (int i = 1; i < group.size(); i++) {
                    toDelete.add(group.get(i).getId());
                }
            }
        }

        // Delete duplicate PAID only
        if (!toDelete.isEmpty()) {
            settlementRepository.deleteAllById(toDelete);
        }
    }

    // ─────────────────────────────────────────────
    // SETTLEMENT CALCULATION
    // ─────────────────────────────────────────────

    @Transactional
    public List<SettlementSummaryResponse> computeSettlement(UUID itineraryId, Long requestingUserId) {
        Itinerary itinerary = findItineraryOrThrow(itineraryId);
        assertIsActiveCollaborator(itineraryId, requestingUserId);

        // 1. Get all ACTIVE collaborators
        List<ItineraryCollaborator> activeCollabs = collaboratorRepository
                .findByItineraryIdAndStatus(itineraryId, CollaboratorStatus.ACTIVE);

        if (activeCollabs.isEmpty()) {
            throw new IllegalStateException("No active collaborators found");
        }

        // 2. Compute total expenses and equal share
        BigDecimal totalExpenses = expenseRepository.sumTotalByItineraryId(itineraryId);
        int memberCount = activeCollabs.size();
        BigDecimal equalShare = totalExpenses.divide(
                BigDecimal.valueOf(memberCount), 2, RoundingMode.HALF_UP);

        // 3. Compute net balance per user
        // netBalance = totalPaid - equalShare
        // positive → owed money; negative → owes money
        Map<Long, BigDecimal> balances = new HashMap<>();
        for (ItineraryCollaborator collab : activeCollabs) {
            BigDecimal paid = expenseRepository.sumAmountByItineraryIdAndPaidByUserId(
                    itineraryId, collab.getUserId());
            BigDecimal net = paid.subtract(equalShare);
            balances.put(collab.getUserId(), net);

            // Save/update SettlementSummary per user
            SettlementSummary summary = summaryRepository
                    .findByItineraryIdAndUserId(itineraryId, collab.getUserId())
                    .orElse(SettlementSummary.builder()
                            .itinerary(itinerary)
                            .userId(collab.getUserId())
                            .build());
            summary.setTotalPaid(paid);
            summary.setEqualShare(equalShare);
            summary.setNetBalance(net);
            summary.setComputedAt(LocalDateTime.now());
            summaryRepository.save(summary);
        }

        // 3.5. ✅ Clean up duplicate PAID settlements FIRST (before creating new ones)
// This prevents findByItineraryIdAndDebtorUserIdAndCreditorUserId from finding multiple duplicates
        cleanupDuplicateSettlements(itineraryId);

// 4. Debt simplification algorithm
// Separate creditors (net > 0) and debtors (net < 0)
// Greedily match debtors to creditors to minimize transactions
        settlementRepository.deleteByItineraryIdAndStatus(itineraryId, SettlementStatus.PENDING);

        List<Map.Entry<Long, BigDecimal>> creditors = balances.entrySet().stream()
                .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .collect(Collectors.toList());

        List<Map.Entry<Long, BigDecimal>> debtors = balances.entrySet().stream()
                .filter(e -> e.getValue().compareTo(BigDecimal.ZERO) < 0)
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toList());

        // Mutable balances for matching
        Map<Long, BigDecimal> creditorMap = new LinkedHashMap<>();
        creditors.forEach(e -> creditorMap.put(e.getKey(), e.getValue()));
        Map<Long, BigDecimal> debtorMap = new LinkedHashMap<>();
        debtors.forEach(e -> debtorMap.put(e.getKey(), e.getValue().abs()));

        List<Settlement> settlements = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> debtor : debtorMap.entrySet()) {
            BigDecimal remaining = debtor.getValue();
            for (Map.Entry<Long, BigDecimal> creditor : creditorMap.entrySet()) {
                if (remaining.compareTo(BigDecimal.ZERO) == 0) break;
                BigDecimal available = creditor.getValue();
                if (available.compareTo(BigDecimal.ZERO) == 0) continue;

                BigDecimal payment = remaining.min(available);

                // ✅ CHECK IF SETTLEMENT ALREADY EXISTS BEFORE CREATING
                Settlement existing = settlementRepository
                        .findByItineraryIdAndDebtorUserIdAndCreditorUserId(
                                itinerary.getId(), debtor.getKey(), creditor.getKey())
                        .orElse(null);

                if (existing == null || existing.getStatus() == SettlementStatus.PAID) {
                    settlements.add(Settlement.builder()
                            .itinerary(itinerary)
                            .debtorUserId(debtor.getKey())
                            .creditorUserId(creditor.getKey())
                            .amount(payment)
                            .status(SettlementStatus.PENDING)
                            .build());
                }

                creditorMap.put(creditor.getKey(), available.subtract(payment));
                remaining = remaining.subtract(payment);
            }
        }

        settlementRepository.saveAll(settlements);



// 5. Build response
        return buildSummaryResponses(itineraryId);
    }

    public List<SettlementSummaryResponse> getSettlement(UUID itineraryId, Long requestingUserId) {
        Itinerary itinerary = findItineraryOrThrow(itineraryId);
        assertCanView(itinerary, requestingUserId);
        return buildSummaryResponses(itineraryId);
    }

    @Transactional
    public SettlementResponse markSettlementPaid(UUID itineraryId, UUID settlementId, Long requestingUserId) {
        findItineraryOrThrow(itineraryId);
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new NoSuchElementException("Settlement not found"));

        // Only the debtor can mark their own debt as paid
        if (!settlement.getDebtorUserId().equals(requestingUserId)) {
            throw new IllegalStateException("Only the debtor can mark this as paid");
        }

        settlement.setStatus(SettlementStatus.PAID);
        settlement.setSettledAt(LocalDateTime.now());
        return mapToSettlementResponse(settlementRepository.save(settlement));
    }

    // ─────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────

    private Itinerary findItineraryOrThrow(UUID id) {
        return itineraryRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Itinerary not found with id: " + id));
    }

    private void assertIsOwner(Itinerary itinerary, Long userId) {
        if (!itinerary.getOwnerId().equals(userId)) {
            throw new IllegalStateException("Only the owner can perform this action");
        }
    }

    private void assertCanView(Itinerary itinerary, Long userId) {
        if (itinerary.getVisibility() == Visibility.PUBLIC) return;
        boolean hasAccess = itinerary.getOwnerId().equals(userId) ||
                collaboratorRepository.findByItineraryIdAndUserId(itinerary.getId(), userId)
                        .map(c -> c.getStatus() == CollaboratorStatus.ACTIVE)
                        .orElse(false);
        if (!hasAccess) {
            throw new IllegalStateException("You do not have access to this itinerary");
        }
    }

    private void assertIsActiveCollaborator(UUID itineraryId, Long userId) {
        ItineraryCollaborator collab = collaboratorRepository
                .findByItineraryIdAndUserId(itineraryId, userId)
                .orElseThrow(() -> new IllegalStateException("You are not a collaborator of this itinerary"));
        if (collab.getStatus() != CollaboratorStatus.ACTIVE) {
            throw new IllegalStateException("Your collaborator status is not active");
        }
    }

    private List<SettlementSummaryResponse> buildSummaryResponses(UUID itineraryId) {
        List<SettlementSummary> summaries = summaryRepository.findByItineraryId(itineraryId);
        List<Settlement> allSettlements = settlementRepository.findByItineraryId(itineraryId);

        return summaries.stream().map(s -> {
            List<SettlementResponse> owes = allSettlements.stream()
                    .filter(st -> st.getDebtorUserId().equals(s.getUserId()))
                    .map(this::mapToSettlementResponse)
                    .collect(Collectors.toList());
            List<SettlementResponse> owedBy = allSettlements.stream()
                    .filter(st -> st.getCreditorUserId().equals(s.getUserId()))
                    .map(this::mapToSettlementResponse)
                    .collect(Collectors.toList());

            return SettlementSummaryResponse.builder()
                    .userId(s.getUserId())
                    .totalPaid(s.getTotalPaid())
                    .equalShare(s.getEqualShare())
                    .netBalance(s.getNetBalance())
                    .computedAt(s.getComputedAt())
                    .owes(owes)
                    .owedBy(owedBy)
                    .build();
        }).collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // MAPPERS
    // ─────────────────────────────────────────────

    private ItineraryResponse mapToResponse(Itinerary i) {
        return ItineraryResponse.builder()
                .id(i.getId())
                .ownerId(i.getOwnerId())
                .title(i.getTitle())
                .description(i.getDescription())
                .startDate(i.getStartDate())
                .endDate(i.getEndDate())
                .destinationRegion(i.getDestinationRegion())
                .estimatedBudget(i.getEstimatedBudget())
                .actualBudget(i.getActualBudget())
                .status(i.getStatus())
                .visibility(i.getVisibility())
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .steps(i.getSteps().stream().map(this::mapToStepResponse).collect(Collectors.toList()))
                .collaborators(i.getCollaborators().stream().map(this::mapToCollaboratorResponse).collect(Collectors.toList()))
                .build();
    }

    private ItineraryStepResponse mapToStepResponse(ItineraryStep s) {
        return ItineraryStepResponse.builder()
                .id(s.getId())
                .addedByUserId(s.getAddedByUserId())
                .serviceType(s.getServiceType())
                .serviceRefId(s.getServiceRefId())
                .title(s.getTitle())
                .notes(s.getNotes())
                .plannedDate(s.getPlannedDate())
                .position(s.getPosition())
                .estimatedCost(s.getEstimatedCost())
                .actualCost(s.getActualCost())
                .latitude(s.getLatitude())
                .longitude(s.getLongitude())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private CollaboratorResponse mapToCollaboratorResponse(ItineraryCollaborator c) {
        return CollaboratorResponse.builder()
                .id(c.getId())
                .userId(c.getUserId())
                .role(c.getRole())
                .status(c.getStatus())
                .requestedAt(c.getRequestedAt())
                .joinedAt(c.getJoinedAt())
                .build();
    }

    private ExpenseResponse mapToExpenseResponse(Expense e) {
        return ExpenseResponse.builder()
                .id(e.getId())
                .itineraryId(e.getItinerary().getId())
                .paidByUserId(e.getPaidByUserId())
                .category(e.getCategory())
                .amount(e.getAmount())
                .description(e.getDescription())
                .expenseDate(e.getExpenseDate())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private SettlementResponse mapToSettlementResponse(Settlement s) {
        return SettlementResponse.builder()
                .id(s.getId())
                .debtorUserId(s.getDebtorUserId())
                .creditorUserId(s.getCreditorUserId())
                .amount(s.getAmount())
                .status(s.getStatus())
                .settledAt(s.getSettledAt())
                .build();
    }
}