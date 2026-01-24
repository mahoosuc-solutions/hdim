package com.healthdata.fhir.rest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CarePlan;
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
import org.springframework.security.access.prepost.PreAuthorize;

import com.healthdata.fhir.service.CarePlanService;

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
 * REST Controller for FHIR CarePlan resource.
 * Provides CRUD operations and search functionality for care coordination plans.
 */
@RestController
@RequestMapping("/CarePlan")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "CarePlan", description = "FHIR CarePlan resource operations")
public class CarePlanController {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);

    private final CarePlanService carePlanService;

    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping(consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Create a new CarePlan resource")
    public ResponseEntity<String> createCarePlan(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @RequestBody String carePlanJson) {

        log.debug("Creating care plan for tenant: {}", tenantId);
        CarePlan carePlan = JSON_PARSER.parseResource(CarePlan.class, carePlanJson);
        CarePlan created = carePlanService.createCarePlan(tenantId, carePlan, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(JSON_PARSER.encodeResourceToString(created));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{id}", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get a CarePlan resource by ID")
    public ResponseEntity<String> getCarePlan(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {

        log.debug("Fetching care plan: tenant={}, id={}", tenantId, id);
        return carePlanService.getCarePlan(tenantId, id)
                .map(carePlan -> ResponseEntity.ok(JSON_PARSER.encodeResourceToString(carePlan)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
    @PutMapping(value = "/{id}", consumes = "application/fhir+json", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Update an existing CarePlan resource")
    public ResponseEntity<String> updateCarePlan(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable UUID id,
            @RequestBody String carePlanJson) {

        log.debug("Updating care plan: tenant={}, id={}", tenantId, id);
        CarePlan carePlan = JSON_PARSER.parseResource(CarePlan.class, carePlanJson);
        CarePlan updated = carePlanService.updateCarePlan(tenantId, id, carePlan, userId);
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(updated));
    }

    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a CarePlan resource")
    public ResponseEntity<Void> deleteCarePlan(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable UUID id) {

        log.debug("Deleting care plan: tenant={}, id={}", tenantId, id);
        carePlanService.deleteCarePlan(tenantId, id, userId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Search Endpoints ====================

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Search CarePlan resources")
    public ResponseEntity<String> searchCarePlans(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient reference (e.g., Patient/uuid)")
            @RequestParam(required = false) String patient,
            @Parameter(description = "Encounter reference (e.g., Encounter/uuid)")
            @RequestParam(required = false) String encounter,
            @Parameter(description = "CarePlan status")
            @RequestParam(required = false) String status,
            @Parameter(description = "CarePlan intent")
            @RequestParam(required = false) String intent,
            @Parameter(description = "Category code")
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int _page,
            @RequestParam(defaultValue = "20") int _count) {

        log.debug("Searching care plans: tenant={}, patient={}", tenantId, patient);

        UUID patientId = extractUuidFromReference(patient, "Patient/");
        UUID encounterId = extractUuidFromReference(encounter, "Encounter/");

        Page<CarePlan> carePlans = carePlanService.searchCarePlans(
                tenantId, patientId, encounterId, status, intent, category,
                PageRequest.of(_page, Math.min(_count, 100)));

        Bundle bundle = toSearchBundle(carePlans.getContent(), carePlans.getTotalElements());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get all care plans for a patient")
    public ResponseEntity<String> getCarePlansByPatient(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {

        log.debug("Fetching care plans for patient: tenant={}, patient={}", tenantId, patientId);
        List<CarePlan> carePlans = carePlanService.getCarePlansByPatient(tenantId, patientId);
        Bundle bundle = toSearchBundle(carePlans, carePlans.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}/active", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get active care plans for a patient")
    public ResponseEntity<String> getActiveCarePlans(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {

        log.debug("Fetching active care plans: tenant={}, patient={}", tenantId, patientId);
        List<CarePlan> carePlans = carePlanService.getActiveCarePlans(tenantId, patientId);
        Bundle bundle = toSearchBundle(carePlans, carePlans.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}/primary", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get primary (top-level) care plans for a patient")
    public ResponseEntity<String> getPrimaryCarePlans(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {

        log.debug("Fetching primary care plans: tenant={}, patient={}", tenantId, patientId);
        List<CarePlan> carePlans = carePlanService.getPrimaryCarePlans(tenantId, patientId);
        Bundle bundle = toSearchBundle(carePlans, carePlans.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}/with-activities", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get care plans with activities for a patient")
    public ResponseEntity<String> getCarePlansWithActivities(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {

        log.debug("Fetching care plans with activities: tenant={}, patient={}", tenantId, patientId);
        List<CarePlan> carePlans = carePlanService.getCarePlansWithActivities(tenantId, patientId);
        Bundle bundle = toSearchBundle(carePlans, carePlans.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/encounter/{encounterId}", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get care plans for an encounter")
    public ResponseEntity<String> getCarePlansByEncounter(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID encounterId) {

        log.debug("Fetching care plans for encounter: tenant={}, encounter={}", tenantId, encounterId);
        List<CarePlan> carePlans = carePlanService.getCarePlansByEncounter(tenantId, encounterId);
        Bundle bundle = toSearchBundle(carePlans, carePlans.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/condition/{conditionId}", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get care plans addressing a specific condition")
    public ResponseEntity<String> getCarePlansByCondition(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID conditionId) {

        log.debug("Fetching care plans for condition: tenant={}, condition={}", tenantId, conditionId);
        List<CarePlan> carePlans = carePlanService.getCarePlansByCondition(tenantId, conditionId);
        Bundle bundle = toSearchBundle(carePlans, carePlans.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/goal/{goalId}", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get care plans with a specific goal")
    public ResponseEntity<String> getCarePlansByGoal(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID goalId) {

        log.debug("Fetching care plans for goal: tenant={}, goal={}", tenantId, goalId);
        List<CarePlan> carePlans = carePlanService.getCarePlansByGoal(tenantId, goalId);
        Bundle bundle = toSearchBundle(carePlans, carePlans.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{carePlanId}/children", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get child care plans (part of a parent plan)")
    public ResponseEntity<String> getChildCarePlans(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID carePlanId) {

        log.debug("Fetching child care plans: tenant={}, parent={}", tenantId, carePlanId);
        List<CarePlan> carePlans = carePlanService.getChildCarePlans(tenantId, carePlanId);
        Bundle bundle = toSearchBundle(carePlans, carePlans.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/expiring", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Get care plans expiring within a date range")
    public ResponseEntity<String> getExpiringCarePlans(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Start date (ISO-8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant start,
            @Parameter(description = "End date (ISO-8601)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant end) {

        log.debug("Fetching expiring care plans: tenant={}", tenantId);
        List<CarePlan> carePlans = carePlanService.getExpiringCarePlans(tenantId, start, end);
        Bundle bundle = toSearchBundle(carePlans, carePlans.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/patient/{patientId}/search", produces = {"application/fhir+json", "application/json"})
    @Operation(summary = "Search care plans by text (title/description)")
    public ResponseEntity<String> searchByText(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            @Parameter(description = "Search term")
            @RequestParam String q) {

        log.debug("Searching care plans by text: tenant={}, patient={}, term={}", tenantId, patientId, q);
        List<CarePlan> carePlans = carePlanService.searchByText(tenantId, patientId, q);
        Bundle bundle = toSearchBundle(carePlans, carePlans.size());
        return ResponseEntity.ok(JSON_PARSER.encodeResourceToString(bundle));
    }

    // ==================== Helper Methods ====================

    private UUID extractUuidFromReference(String reference, String prefix) {
        if (reference != null && reference.startsWith(prefix)) {
            try {
                return UUID.fromString(reference.substring(prefix.length()));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid UUID in reference: {}", reference);
            }
        }
        return null;
    }

    private Bundle toSearchBundle(List<CarePlan> carePlans, long total) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        bundle.setTotal((int) total);

        for (CarePlan carePlan : carePlans) {
            Bundle.BundleEntryComponent entry = bundle.addEntry();
            entry.setResource(carePlan);
            entry.setFullUrl("CarePlan/" + carePlan.getIdElement().getIdPart());
        }

        return bundle;
    }
}
