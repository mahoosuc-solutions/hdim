package com.healthdata.caregap.controller;

import com.healthdata.caregap.persistence.CareGapEntity;
import com.healthdata.caregap.persistence.CareGapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Care Gap REST API Controller
 *
 * Provides standard CRUD operations for care gaps at /api/v1/care-gaps.
 * Used by demo-seeding-service and clinical portal.
 */
@RestController
@RequestMapping("/api/v1/care-gaps")
@RequiredArgsConstructor
@Slf4j
public class CareGapApiController {

    private final CareGapRepository careGapRepository;

    /**
     * List care gaps with pagination and optional filters
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    public ResponseEntity<Page<CareGapEntity>> listCareGaps(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID patientId) {

        log.info("GET /api/v1/care-gaps - tenant: {}, page: {}, size: {}, priority: {}, status: {}, patientId: {}", 
                tenantId, page, size, priority, status, patientId);

        Pageable pageable = PageRequest.of(page, size);
        Page<CareGapEntity> gaps;

        // Filter by priority if provided
        if (priority != null && !priority.isEmpty()) {
            if (patientId != null) {
                // Filter by tenant, patient, and priority
                gaps = careGapRepository.findByTenantIdAndPatientIdAndPriority(
                    tenantId, patientId, priority.toUpperCase(), pageable);
            } else {
                // Filter by tenant and priority
                gaps = careGapRepository.findByTenantIdAndPriority(
                    tenantId, priority.toUpperCase(), pageable);
            }
        } else if (patientId != null) {
            // Filter by tenant and patient
            gaps = careGapRepository.findByTenantIdAndPatientId(
                tenantId, patientId, pageable);
        } else if (status != null && !status.isEmpty()) {
            // Filter by tenant and status
            gaps = careGapRepository.findByTenantIdAndStatus(
                tenantId, status.toUpperCase(), pageable);
        } else {
            // No filters - get all for tenant
            gaps = careGapRepository.findByTenantId(tenantId, pageable);
        }

        return ResponseEntity.ok(gaps);
    }

    /**
     * Get a single care gap by ID
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission('CARE_GAP_READ')")
    public ResponseEntity<CareGapEntity> getCareGap(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {

        log.info("GET /api/v1/care-gaps/{} - tenant: {}", id, tenantId);

        return careGapRepository.findByIdAndTenantId(id, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new care gap
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasPermission('CARE_GAP_WRITE')")
    public ResponseEntity<CareGapEntity> createCareGap(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody CareGapCreateRequest request) {

        String effectiveMeasureId = request.getEffectiveMeasureId();
        String effectiveMeasureName = request.getMeasureName() != null
                ? request.getMeasureName()
                : effectiveMeasureId;
        log.info("POST /api/v1/care-gaps - tenant: {}, patient: {}, measure: {}",
                tenantId, request.getPatientId(), effectiveMeasureId);

        CareGapEntity gap = CareGapEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .patientId(UUID.fromString(request.getPatientId()))
                .measureId(effectiveMeasureId)
                .measureName(effectiveMeasureName)
                .gapStatus("OPEN")
                .gapType("care-gap")
                .priority(request.getPriority() != null ? request.getPriority() : "medium")
                .gapCategory(request.getCategory() != null ? request.getCategory() : "PREVENTIVE")
                .severity(request.getPriority() != null ? request.getPriority() : "medium")
                .gapDescription(request.getDescription() != null ? request.getDescription() : effectiveMeasureName)
                .identifiedDate(Instant.now())
                .dueDate(request.getDueDate() != null
                        ? LocalDate.parse(request.getDueDate())
                        : LocalDate.now().plusMonths(3))
                .createdBy(request.getCreatedBy() != null ? request.getCreatedBy() : "system")
                .build();

        CareGapEntity saved = careGapRepository.save(gap);
        log.info("Created care gap: {}", saved.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * DTO for care gap creation request
     * Supports both measureId (standard) and measureCode (from demo-seeding) field names
     */
    @lombok.Data
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    public static class CareGapCreateRequest {
        private String patientId;
        private String measureId;
        private String measureCode;  // Alias for measureId (used by demo-seeding-service)
        private String measureName;
        private String description;
        private String priority;
        private String category;
        private String identifiedDate;
        private String dueDate;
        private String createdBy;

        /**
         * Returns measureId, falling back to measureCode if measureId is null
         */
        public String getEffectiveMeasureId() {
            return measureId != null ? measureId : measureCode;
        }
    }
}
