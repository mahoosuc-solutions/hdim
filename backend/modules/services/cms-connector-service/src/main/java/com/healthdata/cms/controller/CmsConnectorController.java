package com.healthdata.cms.controller;

import com.healthdata.cms.client.BcdaClient;
import com.healthdata.cms.client.DpcClient;
import com.healthdata.cms.model.*;
import com.healthdata.cms.service.CmsClinicalDataService;
import com.healthdata.cms.service.CmsEobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CMS Connector Controller
 *
 * REST API for interacting with CMS Data APIs:
 * - DPC (Data at Point of Care) - Real-time patient data
 * - BCDA (Beneficiary Claims Data API) - Bulk claims exports
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cms")
@RequiredArgsConstructor
@Tag(name = "CMS Connector", description = "CMS Data Integration APIs")
public class CmsConnectorController {

    private final CmsEobService eobService;
    private final CmsClinicalDataService clinicalDataService;
    private final DpcClient dpcClient;
    private final BcdaClient bcdaClient;

    // ============ DPC Endpoints ============

    @GetMapping("/dpc/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Operation(summary = "Get patient demographics from DPC",
            description = "Retrieves Medicare patient demographics and coverage information")
    public ResponseEntity<String> getPatient(
            @Parameter(description = "DPC Patient ID") @PathVariable String patientId) {
        log.info("Fetching patient from DPC: {}", patientId);
        String patient = dpcClient.getPatient(patientId);
        return ResponseEntity.ok(patient);
    }

    @GetMapping("/dpc/eob/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Operation(summary = "Get ExplanationOfBenefit records",
            description = "Retrieves Medicare claims (EOB) for a patient")
    public ResponseEntity<List<CmsClaim>> getEobForPatient(
            @Parameter(description = "DPC Patient ID") @PathVariable String patientId,
            @Parameter(description = "Tenant ID") @RequestHeader("X-Tenant-ID") UUID tenantId,
            @Parameter(description = "Only fetch records updated after this date (ISO-8601)")
            @RequestParam(required = false) String since) {
        log.info("Fetching EOB from DPC for patient: {} (tenant: {}, since: {})",
                patientId, tenantId, since);

        List<CmsClaim> claims;
        if (since != null && !since.isEmpty()) {
            claims = eobService.fetchEobForPatient(patientId, tenantId, since);
        } else {
            claims = eobService.fetchEobForPatient(patientId, tenantId);
        }

        return ResponseEntity.ok(claims);
    }

    @GetMapping("/dpc/eob/{patientId}/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST')")
    @Operation(summary = "Get EOB summary for a patient",
            description = "Returns aggregated claims summary with totals")
    public ResponseEntity<CmsEobService.EobSummary> getEobSummary(
            @Parameter(description = "DPC Patient ID") @PathVariable String patientId,
            @Parameter(description = "Tenant ID") @RequestHeader("X-Tenant-ID") UUID tenantId) {
        log.info("Fetching EOB summary for patient: {} (tenant: {})", patientId, tenantId);
        CmsEobService.EobSummary summary = eobService.getEobSummary(patientId, tenantId);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/dpc/eob/{patientId}/sync")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sync EOB data for a patient",
            description = "Fetches EOB data from DPC and persists to local database")
    public ResponseEntity<Map<String, Object>> syncEobForPatient(
            @Parameter(description = "DPC Patient ID") @PathVariable String patientId,
            @Parameter(description = "Tenant ID") @RequestHeader("X-Tenant-ID") UUID tenantId) {
        log.info("Syncing EOB data for patient: {} (tenant: {})", patientId, tenantId);
        int count = eobService.syncEobForPatient(patientId, tenantId);
        return ResponseEntity.ok(Map.of(
                "patientId", patientId,
                "tenantId", tenantId,
                "claimsSynced", count,
                "status", "success"
        ));
    }

    @GetMapping("/dpc/conditions/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Operation(summary = "Get conditions from DPC",
            description = "Retrieves diagnosis conditions from Medicare claims")
    public ResponseEntity<String> getConditions(
            @Parameter(description = "DPC Patient ID") @PathVariable String patientId) {
        log.info("Fetching conditions from DPC for patient: {}", patientId);
        String conditions = dpcClient.getConditions(patientId);
        return ResponseEntity.ok(conditions);
    }

    @GetMapping("/dpc/procedures/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Operation(summary = "Get procedures from DPC",
            description = "Retrieves procedures from Medicare claims")
    public ResponseEntity<String> getProcedures(
            @Parameter(description = "DPC Patient ID") @PathVariable String patientId) {
        log.info("Fetching procedures from DPC for patient: {}", patientId);
        String procedures = dpcClient.getProcedures(patientId);
        return ResponseEntity.ok(procedures);
    }

    @GetMapping("/dpc/medications/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Operation(summary = "Get medications from DPC",
            description = "Retrieves medication requests from Part D claims")
    public ResponseEntity<String> getMedications(
            @Parameter(description = "DPC Patient ID") @PathVariable String patientId) {
        log.info("Fetching medications from DPC for patient: {}", patientId);
        String medications = dpcClient.getMedicationRequests(patientId);
        return ResponseEntity.ok(medications);
    }

    @GetMapping("/dpc/observations/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Operation(summary = "Get observations from DPC",
            description = "Retrieves lab results and observations")
    public ResponseEntity<String> getObservations(
            @Parameter(description = "DPC Patient ID") @PathVariable String patientId) {
        log.info("Fetching observations from DPC for patient: {}", patientId);
        String observations = dpcClient.getObservations(patientId);
        return ResponseEntity.ok(observations);
    }

    @GetMapping("/dpc/coverage/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Operation(summary = "Get coverage information from DPC",
            description = "Retrieves Medicare coverage details")
    public ResponseEntity<String> getCoverage(
            @Parameter(description = "DPC Patient ID") @PathVariable String patientId) {
        log.info("Fetching coverage from DPC for patient: {}", patientId);
        String coverage = dpcClient.getCoverage(patientId);
        return ResponseEntity.ok(coverage);
    }

    @GetMapping("/dpc/patient/{patientId}/everything")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Operation(summary = "Get all patient data from DPC",
            description = "Retrieves complete patient record using $everything operation")
    public ResponseEntity<String> getPatientEverything(
            @Parameter(description = "DPC Patient ID") @PathVariable String patientId) {
        log.info("Fetching all data from DPC for patient: {}", patientId);
        String everything = dpcClient.getPatientEverything(patientId);
        return ResponseEntity.ok(everything);
    }

    // ============ Clinical Data Parsed Endpoints ============

    @GetMapping("/dpc/conditions/{patientId}/parsed")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Operation(summary = "Get parsed conditions from DPC",
            description = "Retrieves and parses diagnosis conditions into structured entities")
    public ResponseEntity<List<CmsCondition>> getConditionsParsed(
            @Parameter(description = "DPC Patient ID") @PathVariable String patientId,
            @Parameter(description = "Tenant ID") @RequestHeader("X-Tenant-ID") UUID tenantId) {
        log.info("Fetching parsed conditions for patient: {} (tenant: {})", patientId, tenantId);
        List<CmsCondition> conditions = clinicalDataService.fetchConditions(patientId, tenantId);
        return ResponseEntity.ok(conditions);
    }

    @GetMapping("/dpc/procedures/{patientId}/parsed")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Operation(summary = "Get parsed procedures from DPC",
            description = "Retrieves and parses procedures into structured entities")
    public ResponseEntity<List<CmsProcedure>> getProceduresParsed(
            @Parameter(description = "DPC Patient ID") @PathVariable String patientId,
            @Parameter(description = "Tenant ID") @RequestHeader("X-Tenant-ID") UUID tenantId) {
        log.info("Fetching parsed procedures for patient: {} (tenant: {})", patientId, tenantId);
        List<CmsProcedure> procedures = clinicalDataService.fetchProcedures(patientId, tenantId);
        return ResponseEntity.ok(procedures);
    }

    @GetMapping("/dpc/medications/{patientId}/parsed")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Operation(summary = "Get parsed medication requests from DPC",
            description = "Retrieves and parses Part D medication requests into structured entities")
    public ResponseEntity<List<CmsMedicationRequest>> getMedicationsParsed(
            @Parameter(description = "DPC Patient ID") @PathVariable String patientId,
            @Parameter(description = "Tenant ID") @RequestHeader("X-Tenant-ID") UUID tenantId) {
        log.info("Fetching parsed medications for patient: {} (tenant: {})", patientId, tenantId);
        List<CmsMedicationRequest> medications = clinicalDataService.fetchMedicationRequests(patientId, tenantId);
        return ResponseEntity.ok(medications);
    }

    @GetMapping("/dpc/observations/{patientId}/parsed")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Operation(summary = "Get parsed observations from DPC",
            description = "Retrieves and parses lab results and observations into structured entities")
    public ResponseEntity<List<CmsObservation>> getObservationsParsed(
            @Parameter(description = "DPC Patient ID") @PathVariable String patientId,
            @Parameter(description = "Tenant ID") @RequestHeader("X-Tenant-ID") UUID tenantId) {
        log.info("Fetching parsed observations for patient: {} (tenant: {})", patientId, tenantId);
        List<CmsObservation> observations = clinicalDataService.fetchObservations(patientId, tenantId);
        return ResponseEntity.ok(observations);
    }

    // ============ Clinical Data Sync Endpoints ============

    @PostMapping("/dpc/conditions/{patientId}/sync")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sync conditions for a patient",
            description = "Fetches conditions from DPC and persists to local database")
    public ResponseEntity<Map<String, Object>> syncConditions(
            @Parameter(description = "DPC Patient ID") @PathVariable String patientId,
            @Parameter(description = "Tenant ID") @RequestHeader("X-Tenant-ID") UUID tenantId) {
        log.info("Syncing conditions for patient: {} (tenant: {})", patientId, tenantId);
        int count = clinicalDataService.syncConditions(patientId, tenantId);
        return ResponseEntity.ok(Map.of(
                "patientId", patientId,
                "tenantId", tenantId,
                "conditionsSynced", count,
                "status", "success"
        ));
    }

    @PostMapping("/dpc/procedures/{patientId}/sync")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sync procedures for a patient",
            description = "Fetches procedures from DPC and persists to local database")
    public ResponseEntity<Map<String, Object>> syncProcedures(
            @Parameter(description = "DPC Patient ID") @PathVariable String patientId,
            @Parameter(description = "Tenant ID") @RequestHeader("X-Tenant-ID") UUID tenantId) {
        log.info("Syncing procedures for patient: {} (tenant: {})", patientId, tenantId);
        int count = clinicalDataService.syncProcedures(patientId, tenantId);
        return ResponseEntity.ok(Map.of(
                "patientId", patientId,
                "tenantId", tenantId,
                "proceduresSynced", count,
                "status", "success"
        ));
    }

    @PostMapping("/dpc/medications/{patientId}/sync")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sync medication requests for a patient",
            description = "Fetches medication requests from DPC and persists to local database")
    public ResponseEntity<Map<String, Object>> syncMedications(
            @Parameter(description = "DPC Patient ID") @PathVariable String patientId,
            @Parameter(description = "Tenant ID") @RequestHeader("X-Tenant-ID") UUID tenantId) {
        log.info("Syncing medications for patient: {} (tenant: {})", patientId, tenantId);
        int count = clinicalDataService.syncMedicationRequests(patientId, tenantId);
        return ResponseEntity.ok(Map.of(
                "patientId", patientId,
                "tenantId", tenantId,
                "medicationsSynced", count,
                "status", "success"
        ));
    }

    @PostMapping("/dpc/observations/{patientId}/sync")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sync observations for a patient",
            description = "Fetches observations from DPC and persists to local database")
    public ResponseEntity<Map<String, Object>> syncObservations(
            @Parameter(description = "DPC Patient ID") @PathVariable String patientId,
            @Parameter(description = "Tenant ID") @RequestHeader("X-Tenant-ID") UUID tenantId) {
        log.info("Syncing observations for patient: {} (tenant: {})", patientId, tenantId);
        int count = clinicalDataService.syncObservations(patientId, tenantId);
        return ResponseEntity.ok(Map.of(
                "patientId", patientId,
                "tenantId", tenantId,
                "observationsSynced", count,
                "status", "success"
        ));
    }

    @PostMapping("/dpc/clinical/{patientId}/sync")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Sync all clinical data for a patient",
            description = "Fetches all clinical data (conditions, procedures, medications, observations) from DPC and persists to local database")
    public ResponseEntity<CmsClinicalDataService.ClinicalDataSyncResult> syncAllClinicalData(
            @Parameter(description = "DPC Patient ID") @PathVariable String patientId,
            @Parameter(description = "Tenant ID") @RequestHeader("X-Tenant-ID") UUID tenantId) {
        log.info("Syncing all clinical data for patient: {} (tenant: {})", patientId, tenantId);
        CmsClinicalDataService.ClinicalDataSyncResult result = clinicalDataService.syncAllClinicalData(patientId, tenantId);
        return ResponseEntity.ok(result);
    }

    // ============ BCDA Endpoints ============

    @PostMapping("/bcda/export/patient")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Initiate BCDA patient bulk export",
            description = "Starts async bulk export job for Patient resources")
    public ResponseEntity<BcdaClient.BulkDataExportResponse> initiatePatientExport(
            @Parameter(description = "Only export records updated after this date")
            @RequestParam(required = false) String since) {
        log.info("Initiating BCDA patient export (since: {})", since);
        BcdaClient.BulkDataExportResponse response = bcdaClient.requestPatientExport(since);
        return ResponseEntity.accepted().body(response);
    }

    @PostMapping("/bcda/export/group")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Initiate BCDA group bulk export",
            description = "Starts async bulk export job for all resources")
    public ResponseEntity<BcdaClient.BulkDataExportResponse> initiateGroupExport(
            @Parameter(description = "Only export records updated after this date")
            @RequestParam(required = false) String since) {
        log.info("Initiating BCDA group export (since: {})", since);
        BcdaClient.BulkDataExportResponse response = bcdaClient.requestGroupExport(since);
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/bcda/export/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Check BCDA export status",
            description = "Returns status of an ongoing bulk export job")
    public ResponseEntity<BcdaClient.BulkDataExportStatus> getExportStatus(
            @Parameter(description = "Export job URL from Content-Location header")
            @RequestParam String jobUrl) {
        log.info("Checking BCDA export status: {}", jobUrl);
        BcdaClient.BulkDataExportStatus status = bcdaClient.getExportStatus(jobUrl);
        return ResponseEntity.ok(status);
    }

    @DeleteMapping("/bcda/export")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel BCDA export",
            description = "Cancels an in-progress bulk export job")
    public ResponseEntity<Void> cancelExport(
            @Parameter(description = "Export job URL") @RequestParam String jobUrl) {
        log.info("Cancelling BCDA export: {}", jobUrl);
        bcdaClient.cancelExport(jobUrl);
        return ResponseEntity.noContent().build();
    }

    // ============ Health Endpoints ============

    @GetMapping("/health/dpc")
    @Operation(summary = "Check DPC API health",
            description = "Returns health status of DPC API connection")
    public ResponseEntity<DpcClient.DpcHealthStatus> getDpcHealth() {
        log.info("Checking DPC API health");
        DpcClient.DpcHealthStatus health = dpcClient.getHealthStatus();
        return ResponseEntity.ok(health);
    }

    @GetMapping("/health/bcda")
    @Operation(summary = "Check BCDA API health",
            description = "Returns health status of BCDA API connection")
    public ResponseEntity<BcdaClient.BcdaHealthStatus> getBcdaHealth() {
        log.info("Checking BCDA API health");
        BcdaClient.BcdaHealthStatus health = bcdaClient.getHealthStatus();
        return ResponseEntity.ok(health);
    }

    @GetMapping("/health")
    @Operation(summary = "Check CMS connector health",
            description = "Returns overall health status of CMS integrations")
    public ResponseEntity<Map<String, Object>> getOverallHealth() {
        log.info("Checking CMS connector overall health");
        DpcClient.DpcHealthStatus dpcHealth = dpcClient.getHealthStatus();
        BcdaClient.BcdaHealthStatus bcdaHealth = bcdaClient.getHealthStatus();

        String overallStatus = (dpcHealth.isHealthy() || bcdaHealth.isHealthy())
                ? "UP" : "DOWN";

        return ResponseEntity.ok(Map.of(
                "status", overallStatus,
                "dpc", Map.of(
                        "status", dpcHealth.getStatus(),
                        "endpoint", dpcHealth.getEndpoint()
                ),
                "bcda", Map.of(
                        "status", bcdaHealth.getStatus(),
                        "endpoint", bcdaHealth.getEndpoint()
                )
        ));
    }
}
