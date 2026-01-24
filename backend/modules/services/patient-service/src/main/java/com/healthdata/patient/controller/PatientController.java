package com.healthdata.patient.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.healthdata.patient.service.PatientAggregationService;
import com.healthdata.patient.service.PatientHealthStatusService;
import com.healthdata.patient.service.PatientTimelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Patient Controller
 *
 * REST API for patient data aggregation, timeline views, and health status dashboards.
 *
 * Endpoints (via gateway at /patient/patient/* or /api/patients/patient/*):
 * - GET /patient/health-record - Comprehensive patient health record
 * - GET /patient/allergies - Patient allergies
 * - GET /patient/immunizations - Patient immunizations
 * - GET /patient/medications - Patient medications
 * - GET /patient/conditions - Patient conditions
 * - GET /patient/procedures - Patient procedures
 * - GET /patient/vitals - Patient vital signs
 * - GET /patient/labs - Patient lab results
 * - GET /patient/encounters - Patient encounters
 * - GET /patient/care-plans - Patient care plans
 * - GET /patient/timeline - Patient timeline
 * - GET /patient/timeline/by-date - Timeline filtered by date range
 * - GET /patient/timeline/by-type - Timeline filtered by resource type
 * - GET /patient/timeline/summary - Monthly timeline summary
 * - GET /patient/health-status - Health status dashboard
 * - GET /patient/medication-summary - Medication summary
 * - GET /patient/allergy-summary - Allergy summary
 * - GET /patient/condition-summary - Condition summary
 * - GET /patient/immunization-summary - Immunization summary
 * - GET /patient/_health - Health check endpoint
 */
@RestController
@RequestMapping("/patient")
@RequiredArgsConstructor
@Slf4j
public class PatientController {

    private final PatientAggregationService aggregationService;
    private final PatientTimelineService timelineService;
    private final PatientHealthStatusService healthStatusService;
    private final FhirContext fhirContext = FhirContext.forR4();
    private final IParser jsonParser = fhirContext.newJsonParser();

    // ==================== Patient Aggregation Endpoints ====================

    /**
     * Get comprehensive patient health record
     *
     * @param tenantId Tenant ID (from header)
     * @param patientId Patient ID (query parameter)
     * @return FHIR Bundle with all patient resources
     */
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/health-record", produces = "application/fhir+json")
    public ResponseEntity<String> getComprehensiveHealthRecord(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId
    ) {
        log.info("GET /patient/health-record - tenant: {}, patient: {}", tenantId, patientId);

        Bundle bundle = aggregationService.getComprehensiveHealthRecord(tenantId, patientId);
        String json = jsonParser.encodeResourceToString(bundle);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/fhir+json"))
                .body(json);
    }

    /**
     * Get patient allergies
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param onlyCritical Return only critical allergies
     * @return FHIR Bundle with allergies
     */
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/allergies", produces = "application/fhir+json")
    public ResponseEntity<String> getAllergies(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId,
            @RequestParam(value = "onlyCritical", defaultValue = "false") boolean onlyCritical
    ) {
        log.info("GET /patient/allergies - patient: {}, onlyCritical: {}", patientId, onlyCritical);

        Bundle bundle = aggregationService.getAllergies(tenantId, patientId, onlyCritical);
        String json = jsonParser.encodeResourceToString(bundle);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/fhir+json"))
                .body(json);
    }

    /**
     * Get patient immunizations
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param onlyCompleted Return only completed immunizations
     * @return FHIR Bundle with immunizations
     */
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/immunizations", produces = "application/fhir+json")
    public ResponseEntity<String> getImmunizations(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId,
            @RequestParam(value = "onlyCompleted", defaultValue = "false") boolean onlyCompleted
    ) {
        log.info("GET /patient/immunizations - patient: {}, onlyCompleted: {}", patientId, onlyCompleted);

        Bundle bundle = aggregationService.getImmunizations(tenantId, patientId, onlyCompleted);
        String json = jsonParser.encodeResourceToString(bundle);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/fhir+json"))
                .body(json);
    }

    /**
     * Get patient medications
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param onlyActive Return only active medications
     * @return FHIR Bundle with medications
     */
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/medications", produces = "application/fhir+json")
    public ResponseEntity<String> getMedications(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId,
            @RequestParam(value = "onlyActive", defaultValue = "true") boolean onlyActive
    ) {
        log.info("GET /patient/medications - patient: {}, onlyActive: {}", patientId, onlyActive);

        Bundle bundle = aggregationService.getMedications(tenantId, patientId, onlyActive);
        String json = jsonParser.encodeResourceToString(bundle);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/fhir+json"))
                .body(json);
    }

    /**
     * Get patient conditions
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param onlyActive Return only active conditions
     * @return FHIR Bundle with conditions
     */
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/conditions", produces = "application/fhir+json")
    public ResponseEntity<String> getConditions(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId,
            @RequestParam(value = "onlyActive", defaultValue = "true") boolean onlyActive
    ) {
        log.info("GET /patient/conditions - patient: {}, onlyActive: {}", patientId, onlyActive);

        Bundle bundle = aggregationService.getConditions(tenantId, patientId, onlyActive);
        String json = jsonParser.encodeResourceToString(bundle);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/fhir+json"))
                .body(json);
    }

    /**
     * Get patient procedures
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return FHIR Bundle with procedures
     */
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/procedures", produces = "application/fhir+json")
    public ResponseEntity<String> getProcedures(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId
    ) {
        log.info("GET /patient/procedures - patient: {}", patientId);

        Bundle bundle = aggregationService.getProcedures(tenantId, patientId);
        String json = jsonParser.encodeResourceToString(bundle);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/fhir+json"))
                .body(json);
    }

    /**
     * Get patient vital signs
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return FHIR Bundle with vital signs
     */
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/vitals", produces = "application/fhir+json")
    public ResponseEntity<String> getVitalSigns(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId
    ) {
        log.info("GET /patient/vitals - patient: {}", patientId);

        Bundle bundle = aggregationService.getVitalSigns(tenantId, patientId);
        String json = jsonParser.encodeResourceToString(bundle);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/fhir+json"))
                .body(json);
    }

    /**
     * Get patient lab results
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return FHIR Bundle with lab results
     */
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/labs", produces = "application/fhir+json")
    public ResponseEntity<String> getLabResults(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId
    ) {
        log.info("GET /patient/labs - patient: {}", patientId);

        Bundle bundle = aggregationService.getLabResults(tenantId, patientId);
        String json = jsonParser.encodeResourceToString(bundle);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/fhir+json"))
                .body(json);
    }

    /**
     * Get patient encounters
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param onlyActive Return only active encounters
     * @return FHIR Bundle with encounters
     */
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/encounters", produces = "application/fhir+json")
    public ResponseEntity<String> getEncounters(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId,
            @RequestParam(value = "onlyActive", defaultValue = "false") boolean onlyActive
    ) {
        log.info("GET /patient/encounters - patient: {}, onlyActive: {}", patientId, onlyActive);

        Bundle bundle = aggregationService.getEncounters(tenantId, patientId, onlyActive);
        String json = jsonParser.encodeResourceToString(bundle);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/fhir+json"))
                .body(json);
    }

    /**
     * Get patient care plans
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param onlyActive Return only active care plans
     * @return FHIR Bundle with care plans
     */
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/care-plans", produces = "application/fhir+json")
    public ResponseEntity<String> getCarePlans(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId,
            @RequestParam(value = "onlyActive", defaultValue = "true") boolean onlyActive
    ) {
        log.info("GET /patient/care-plans - patient: {}, onlyActive: {}", patientId, onlyActive);

        Bundle bundle = aggregationService.getCarePlans(tenantId, patientId, onlyActive);
        String json = jsonParser.encodeResourceToString(bundle);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/fhir+json"))
                .body(json);
    }

    // ==================== Timeline Endpoints ====================

    /**
     * Get patient timeline
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return List of timeline events
     */
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/timeline", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PatientTimelineService.TimelineEvent>> getPatientTimeline(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId
    ) {
        log.info("GET /patient/timeline - patient: {}", patientId);

        List<PatientTimelineService.TimelineEvent> timeline =
                timelineService.getPatientTimeline(tenantId, patientId);

        return ResponseEntity.ok(timeline);
    }

    /**
     * Get patient timeline by date range
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param startDate Start date (ISO format: YYYY-MM-DD)
     * @param endDate End date (ISO format: YYYY-MM-DD)
     * @return List of timeline events
     */
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/timeline/by-date", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PatientTimelineService.TimelineEvent>> getPatientTimelineByDateRange(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("GET /patient/timeline/by-date - patient: {}, range: {} to {}", patientId, startDate, endDate);

        List<PatientTimelineService.TimelineEvent> timeline =
                timelineService.getPatientTimelineByDateRange(tenantId, patientId, startDate, endDate);

        return ResponseEntity.ok(timeline);
    }

    /**
     * Get patient timeline by resource type
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param resourceType FHIR resource type
     * @return List of timeline events
     */
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/timeline/by-type", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PatientTimelineService.TimelineEvent>> getPatientTimelineByResourceType(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId,
            @RequestParam("resourceType") String resourceType
    ) {
        log.info("GET /patient/timeline/by-type - patient: {}, type: {}", patientId, resourceType);

        List<PatientTimelineService.TimelineEvent> timeline =
                timelineService.getPatientTimelineByResourceType(tenantId, patientId, resourceType);

        return ResponseEntity.ok(timeline);
    }

    /**
     * Get monthly timeline summary
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @param year Year to summarize
     * @return Map of month -> event count
     */
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/timeline/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> getTimelineSummaryByMonth(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId,
            @RequestParam("year") int year
    ) {
        log.info("GET /patient/timeline/summary - patient: {}, year: {}", patientId, year);

        Map<String, Integer> summary =
                timelineService.getTimelineSummaryByMonth(tenantId, patientId, year);

        return ResponseEntity.ok(summary);
    }

    // ==================== Health Status Endpoints ====================

    /**
     * Get health status dashboard
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Health status summary
     */
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/health-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientHealthStatusService.HealthStatusSummary> getHealthStatusSummary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId
    ) {
        log.info("GET /patient/health-status - patient: {}", patientId);

        PatientHealthStatusService.HealthStatusSummary summary =
                healthStatusService.getHealthStatusSummary(tenantId, patientId);

        return ResponseEntity.ok(summary);
    }

    /**
     * Get medication summary
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Medication summary
     */
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/medication-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientHealthStatusService.MedicationSummary> getMedicationSummary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId
    ) {
        log.info("GET /patient/medication-summary - patient: {}", patientId);

        PatientHealthStatusService.MedicationSummary summary =
                healthStatusService.getMedicationSummary(tenantId, patientId);

        return ResponseEntity.ok(summary);
    }

    /**
     * Get allergy summary
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Allergy summary
     */
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/allergy-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientHealthStatusService.AllergySummary> getAllergySummary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId
    ) {
        log.info("GET /patient/allergy-summary - patient: {}", patientId);

        PatientHealthStatusService.AllergySummary summary =
                healthStatusService.getAllergySummary(tenantId, patientId);

        return ResponseEntity.ok(summary);
    }

    /**
     * Get condition summary
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Condition summary
     */
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/condition-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientHealthStatusService.ConditionSummary> getConditionSummary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId
    ) {
        log.info("GET /patient/condition-summary - patient: {}", patientId);

        PatientHealthStatusService.ConditionSummary summary =
                healthStatusService.getConditionSummary(tenantId, patientId);

        return ResponseEntity.ok(summary);
    }

    /**
     * Get immunization summary
     *
     * @param tenantId Tenant ID
     * @param patientId Patient ID
     * @return Immunization summary
     */
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/immunization-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientHealthStatusService.ImmunizationSummary> getImmunizationSummary(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId
    ) {
        log.info("GET /patient/immunization-summary - patient: {}", patientId);

        PatientHealthStatusService.ImmunizationSummary summary =
                healthStatusService.getImmunizationSummary(tenantId, patientId);

        return ResponseEntity.ok(summary);
    }

    // ==================== Health Check ====================

    /**
     * Health check endpoint
     *
     * @return Health status
     */
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/_health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "patient-service",
                "timestamp", LocalDate.now().toString()
        ));
    }
}
