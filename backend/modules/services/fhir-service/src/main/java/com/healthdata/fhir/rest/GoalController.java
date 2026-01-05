package com.healthdata.fhir.rest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Goal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthdata.fhir.service.GoalService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;

/**
 * REST Controller for FHIR Goal resource.
 * Provides CRUD operations and search functionality for patient health goals.
 */
@RestController
@RequestMapping("/fhir/Goal")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Goal", description = "FHIR Goal resource operations")
public class GoalController {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);

    private final GoalService goalService;

    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(consumes = "application/fhir+json", produces = "application/fhir+json")
    @Operation(summary = "Create a new Goal resource")
    public ResponseEntity<String> createGoal(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @RequestBody String goalJson) {

        log.debug("Creating goal for tenant: {}", tenantId);
        Goal goal = JSON_PARSER.parseResource(Goal.class, goalJson);
        Goal created = goalService.createGoal(tenantId, goal, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(JSON_PARSER.encodeResourceToString(created));
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{id}", produces = "application/fhir+json")
    @Operation(summary = "Get a Goal resource by ID")
    public ResponseEntity<String> getGoal(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {

        log.debug("Fetching goal: tenant={}, id={}", tenantId, id);
        return goalService.getGoal(tenantId, id)
                .map(goal -> ResponseEntity.ok(JSON_PARSER.encodeResourceToString(goal)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PutMapping(value = "/{id}", consumes = "application/fhir+json", produces = "application/fhir+json")
    @Operation(summary = "Update an existing Goal resource")
    public ResponseEntity<String> updateGoal(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable UUID id,
            @RequestBody String goalJson) {

        log.debug("Updating goal: tenant={}, id={}", tenantId, id);
        Goal goal = JSON_PARSER.parseResource(Goal.class, goalJson);
        Goal updated = goalService.updateGoal(tenantId, id, goal, userId);
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(updated));
    }

    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a Goal resource")
    public ResponseEntity<Void> deleteGoal(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable UUID id) {

        log.debug("Deleting goal: tenant={}, id={}", tenantId, id);
        goalService.deleteGoal(tenantId, id, userId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Search Endpoints ====================

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(produces = "application/fhir+json")
    @Operation(summary = "Search Goal resources")
    public ResponseEntity<String> searchGoals(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient reference (e.g., Patient/uuid)")
            @RequestParam(required = false) String patient,
            @Parameter(description = "Lifecycle status")
            @RequestParam(name = "lifecycle-status", required = false) String lifecycleStatus,
            @Parameter(description = "Achievement status")
            @RequestParam(name = "achievement-status", required = false) String achievementStatus,
            @Parameter(description = "Category code")
            @RequestParam(required = false) String category,
            @Parameter(description = "Priority")
            @RequestParam(required = false) String priority,
            @RequestParam(defaultValue = "0") int _page,
            @RequestParam(defaultValue = "20") int _count) {

        log.debug("Searching goals: tenant={}, patient={}", tenantId, patient);

        UUID patientId = null;
        if (patient != null && patient.startsWith("Patient/")) {
            patientId = UUID.fromString(patient.substring(8));
        }

        Page<Goal> goals = goalService.searchGoals(
                tenantId, patientId, lifecycleStatus, achievementStatus, category, priority,
                PageRequest.of(_page, Math.min(_count, 100)));

        Bundle bundle = toSearchBundle(goals.getContent(), goals.getTotalElements());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}", produces = "application/fhir+json")
    @Operation(summary = "Get all goals for a patient")
    public ResponseEntity<String> getGoalsByPatient(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {

        log.debug("Fetching goals for patient: tenant={}, patient={}", tenantId, patientId);
        List<Goal> goals = goalService.getGoalsByPatient(tenantId, patientId);
        Bundle bundle = toSearchBundle(goals, goals.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}/active", produces = "application/fhir+json")
    @Operation(summary = "Get active goals for a patient")
    public ResponseEntity<String> getActiveGoals(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {

        log.debug("Fetching active goals: tenant={}, patient={}", tenantId, patientId);
        List<Goal> goals = goalService.getActiveGoals(tenantId, patientId);
        Bundle bundle = toSearchBundle(goals, goals.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}/overdue", produces = "application/fhir+json")
    @Operation(summary = "Get overdue goals for a patient")
    public ResponseEntity<String> getOverdueGoals(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {

        log.debug("Fetching overdue goals: tenant={}, patient={}", tenantId, patientId);
        List<Goal> goals = goalService.getOverdueGoals(tenantId, patientId);
        Bundle bundle = toSearchBundle(goals, goals.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}/high-priority", produces = "application/fhir+json")
    @Operation(summary = "Get high priority goals for a patient")
    public ResponseEntity<String> getHighPriorityGoals(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {

        log.debug("Fetching high priority goals: tenant={}, patient={}", tenantId, patientId);
        List<Goal> goals = goalService.getHighPriorityGoals(tenantId, patientId);
        Bundle bundle = toSearchBundle(goals, goals.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/condition/{conditionId}", produces = "application/fhir+json")
    @Operation(summary = "Get goals addressing a specific condition")
    public ResponseEntity<String> getGoalsByCondition(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID conditionId) {

        log.debug("Fetching goals for condition: tenant={}, condition={}", tenantId, conditionId);
        List<Goal> goals = goalService.getGoalsByCondition(tenantId, conditionId);
        Bundle bundle = toSearchBundle(goals, goals.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/due", produces = "application/fhir+json")
    @Operation(summary = "Get goals due within a date range")
    public ResponseEntity<String> getGoalsDueInRange(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Start date (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "End date (yyyy-MM-dd)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {

        log.debug("Fetching goals due between {} and {}: tenant={}", start, end, tenantId);
        List<Goal> goals = goalService.getGoalsDueInRange(tenantId, start, end);
        Bundle bundle = toSearchBundle(goals, goals.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    // ==================== Helper Methods ====================

    private Bundle toSearchBundle(List<Goal> goals, long total) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal((int) total);

        for (Goal goal : goals) {
            Bundle.BundleEntryComponent entry = bundle.addEntry();
            entry.setResource(goal);
            entry.setFullUrl("Goal/" + goal.getIdElement().getIdPart());
        }

        return bundle;
    }
}
