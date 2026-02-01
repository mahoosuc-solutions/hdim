package com.healthdata.patientevent.api;

import com.healthdata.patientevent.projection.PatientProjection;
import com.healthdata.patientevent.repository.PatientProjectionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Patient Projection Query API
 *
 * Provides fast read-only queries on denormalized patient data.
 * Part of CQRS pattern - this service contains the read model.
 */
@RestController
@RequestMapping("/api/v1/patient-projections")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Patient Projections", description = "CQRS Read Model - Patient Projections")
public class PatientProjectionController {

    private final PatientProjectionRepository patientProjectionRepository;

    /**
     * Get patient projection by ID
     */
    @GetMapping("/{patientId}")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Operation(summary = "Get patient projection", description = "Retrieve denormalized patient view")
    public ResponseEntity<PatientProjection> getPatientProjection(
            @PathVariable UUID patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching patient projection for patient {} in tenant {}", patientId, tenantId);

        return patientProjectionRepository.findByTenantIdAndPatientId(tenantId, patientId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * List all patient projections for tenant
     */
    @GetMapping
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Operation(summary = "List patient projections", description = "Retrieve all patient projections for tenant")
    public ResponseEntity<Page<PatientProjection>> listPatients(
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {
        log.debug("Listing patient projections for tenant {}", tenantId);

        Page<PatientProjection> patients = patientProjectionRepository
            .findByTenantIdOrderByLastNameAsc(tenantId, pageable);
        return ResponseEntity.ok(patients);
    }

    /**
     * Search patients by last name
     */
    @GetMapping("/search")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Operation(summary = "Search patients", description = "Search patients by last name")
    public ResponseEntity<Page<PatientProjection>> searchPatients(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam String lastName,
            Pageable pageable) {
        log.debug("Searching patients for tenant {} with lastName containing: {}", tenantId, lastName);

        Page<PatientProjection> patients = patientProjectionRepository
            .findByTenantIdAndLastNameContainingIgnoreCaseOrderByLastName(tenantId, lastName, pageable);
        return ResponseEntity.ok(patients);
    }

    /**
     * Get high-risk patients
     */
    @GetMapping("/high-risk")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Operation(summary = "Get high-risk patients", description = "Retrieve all high-risk patients for tenant")
    public ResponseEntity<List<PatientProjection>> getHighRiskPatients(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching high-risk patients for tenant {}", tenantId);

        List<PatientProjection> patients = patientProjectionRepository.findHighRiskPatients(tenantId);
        return ResponseEntity.ok(patients);
    }

    /**
     * Get patients with urgent care gaps
     */
    @GetMapping("/urgent-gaps")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Operation(summary = "Get patients with urgent gaps", description = "Retrieve patients with urgent care gaps")
    public ResponseEntity<List<PatientProjection>> getPatientsWithUrgentGaps(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching patients with urgent gaps for tenant {}", tenantId);

        List<PatientProjection> patients = patientProjectionRepository.findPatientsWithUrgentGaps(tenantId);
        return ResponseEntity.ok(patients);
    }

    /**
     * Get patients with critical alerts
     */
    @GetMapping("/critical-alerts")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Operation(summary = "Get patients with critical alerts", description = "Retrieve patients with critical alerts")
    public ResponseEntity<List<PatientProjection>> getPatientsWithCriticalAlerts(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching patients with critical alerts for tenant {}", tenantId);

        List<PatientProjection> patients = patientProjectionRepository.findPatientsWithCriticalAlerts(tenantId);
        return ResponseEntity.ok(patients);
    }

    /**
     * Get patients with mental health flags
     */
    @GetMapping("/mental-health")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Operation(summary = "Get patients with mental health flags", description = "Retrieve patients with mental health assessments")
    public ResponseEntity<List<PatientProjection>> getPatientsWithMentalHealthFlags(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Fetching patients with mental health flags for tenant {}", tenantId);

        List<PatientProjection> patients = patientProjectionRepository.findPatientsWithMentalHealthFlags(tenantId);
        return ResponseEntity.ok(patients);
    }

    /**
     * Get statistics for tenant
     */
    @GetMapping("/stats")
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @Operation(summary = "Get patient statistics", description = "Retrieve aggregated statistics for tenant")
    public ResponseEntity<PatientStatistics> getStatistics(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.debug("Calculating patient statistics for tenant {}", tenantId);

        long totalPatients = patientProjectionRepository.countByTenantId(tenantId);
        long highRiskCount = patientProjectionRepository.countHighRiskPatients(tenantId);
        long urgentGapsCount = patientProjectionRepository.countPatientsWithUrgentGaps(tenantId);
        long activeAlertsCount = patientProjectionRepository.countPatientsWithActiveAlerts(tenantId);

        PatientStatistics stats = PatientStatistics.builder()
            .totalPatients(totalPatients)
            .highRiskCount(highRiskCount)
            .patientsWithUrgentGaps(urgentGapsCount)
            .patientsWithActiveAlerts(activeAlertsCount)
            .build();

        return ResponseEntity.ok(stats);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Service health status")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Patient event service is healthy");
    }
}
