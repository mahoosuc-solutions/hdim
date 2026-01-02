package com.healthdata.api;

import com.healthdata.patient.domain.Patient;
import com.healthdata.patient.service.PatientService;
import com.healthdata.quality.service.QualityMeasureService;
import com.healthdata.quality.domain.MeasureResult;
import com.healthdata.caregap.service.CareGapDetector;
import com.healthdata.api.dto.PatientHealthOverview;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Unified REST API Controller
 *
 * Single API gateway for all modules - much simpler than 9 separate APIs!
 * All endpoints are in one place, making it easy to understand and maintain.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class HealthDataController {

    // All services injected directly - no REST clients!
    private final PatientService patientService;
    private final QualityMeasureService qualityMeasureService;
    private final CareGapDetector careGapDetector;
    // private final FhirService fhirService;
    // private final NotificationService notificationService;

    // ==================== Patient Endpoints ====================

    @PostMapping("/patients")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Patient> createPatient(@RequestBody Patient patient) {
        log.info("Creating patient: {} {}", patient.getFirstName(), patient.getLastName());
        Patient created = patientService.createPatient(patient);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/patients/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Patient> getPatient(@PathVariable String id) {
        return patientService.getPatient(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/patients")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<Patient>> searchPatients(
            @RequestParam String tenantId,
            @RequestParam(required = false) String query,
            Pageable pageable) {
        Page<Patient> patients = patientService.searchPatients(tenantId, query, pageable);
        return ResponseEntity.ok(patients);
    }

    @PutMapping("/patients/{id}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Patient> updatePatient(
            @PathVariable String id,
            @RequestBody Patient patient) {
        Patient updated = patientService.updatePatient(id, patient);
        return ResponseEntity.ok(updated);
    }

    // ==================== Quality Measure Endpoints ====================

    @PostMapping("/measures/calculate")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MeasureResult> calculateMeasure(
            @RequestParam String patientId,
            @RequestParam String measureId) {
        log.info("Calculating measure {} for patient {}", measureId, patientId);
        MeasureResult result = qualityMeasureService.calculateMeasure(patientId, measureId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/measures/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CompletableFuture<List<MeasureResult>>> batchCalculate(
            @RequestParam String tenantId,
            @RequestParam String measureId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        log.info("Starting batch calculation for tenant {}", tenantId);
        var future = qualityMeasureService.calculateMeasuresForPopulation(
            tenantId, measureId, page, size
        );
        return ResponseEntity.accepted().body(future);
    }

    @GetMapping("/measures/status")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<QualityMeasureService.MeasureStatus> getMeasureStatus(
            @RequestParam String patientId,
            @RequestParam String measureId) {
        var status = qualityMeasureService.getMeasureStatus(patientId, measureId);
        return ResponseEntity.ok(status);
    }

    // ==================== Care Gap Management ====================

    @GetMapping("/caregaps/{patientId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<?>> getCareGaps(@PathVariable String patientId) {
        log.info("Fetching care gaps for patient: {}", patientId);
        var gaps = careGapDetector.detectCareGaps(patientId);
        return ResponseEntity.ok(gaps);
    }

    @PostMapping("/caregaps/detect-batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> detectCareGapsBatch(
            @RequestBody List<String> patientIds) {
        log.info("Starting batch care gap detection for {} patients", patientIds.size());
        careGapDetector.detectCareGapsBatch(patientIds);
        return ResponseEntity.accepted().body(
            Map.of("status", "processing", "patients", String.valueOf(patientIds.size()))
        );
    }

    @PostMapping("/caregaps/{gapId}/close")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, String>> closeCareGap(
            @PathVariable String gapId,
            @RequestParam String reason) {
        log.info("Closing care gap: {} with reason: {}", gapId, reason);
        careGapDetector.closeCareGap(gapId, reason);
        return ResponseEntity.ok(Map.of("status", "closed", "gapId", gapId));
    }

    // ==================== FHIR Resources ====================

    @GetMapping("/fhir/observations/{patientId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<?>> getObservations(@PathVariable String patientId) {
        log.info("Fetching observations for patient: {}", patientId);
        // Uncomment when FhirService is injected
        // var observations = fhirService.getObservationsForPatient(patientId);
        // return ResponseEntity.ok(observations);
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/fhir/conditions/{patientId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<?>> getConditions(@PathVariable String patientId) {
        log.info("Fetching conditions for patient: {}", patientId);
        // var conditions = fhirService.getConditionsForPatient(patientId);
        // return ResponseEntity.ok(conditions);
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/fhir/medications/{patientId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<?>> getMedications(@PathVariable String patientId) {
        log.info("Fetching medications for patient: {}", patientId);
        // var medications = fhirService.getMedicationsForPatient(patientId);
        // return ResponseEntity.ok(medications);
        return ResponseEntity.ok(List.of());
    }

    // ==================== Health Check ====================

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", System.currentTimeMillis(),
            "service", "healthdata-platform",
            "version", "2.0.0",
            "architecture", "modular-monolith"
        ));
    }

    // ==================== Patient Health Overview ====================

    @GetMapping("/patient-health/overview")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PatientHealthOverview> getPatientHealthOverview(
            @RequestParam String patientId) {

        // All these calls happen in-memory!
        var patient = patientService.getPatient(patientId).orElse(null);
        if (patient == null) {
            return ResponseEntity.notFound().build();
        }

        // Parallel in-memory processing
        var diabetesScore = qualityMeasureService.calculateMeasure(patientId, "HbA1c-Control");
        var bpScore = qualityMeasureService.calculateMeasure(patientId, "BP-Control");
        var medAdherence = qualityMeasureService.calculateMeasure(patientId, "Medication-Adherence");

        // Build comprehensive overview
        var overview = PatientHealthOverview.builder()
            .patient(patient)
            .overallScore(calculateOverallScore(diabetesScore, bpScore, medAdherence))
            .diabetesControl(diabetesScore)
            .bloodPressureControl(bpScore)
            .medicationAdherence(medAdherence)
            .lastUpdated(java.time.LocalDateTime.now())
            .build();

        return ResponseEntity.ok(overview);
    }

    // ==================== System Health ====================

    @GetMapping("/health/ready")
    public ResponseEntity<String> readiness() {
        // Simple readiness check - all modules in same JVM
        return ResponseEntity.ok("Ready");
    }

    @GetMapping("/health/live")
    public ResponseEntity<String> liveness() {
        return ResponseEntity.ok("Alive");
    }

    // ==================== Helper Methods ====================

    private double calculateOverallScore(MeasureResult... results) {
        if (results.length == 0) return 0.0;

        double sum = 0;
        for (MeasureResult result : results) {
            sum += result.getScore();
        }
        return sum / results.length;
    }

    // ==================== DTOs ====================

    @lombok.Data
    @lombok.Builder
    public static class PatientHealthOverview {
        private Patient patient;
        private double overallScore;
        private MeasureResult diabetesControl;
        private MeasureResult bloodPressureControl;
        private MeasureResult medicationAdherence;
        private java.time.LocalDateTime lastUpdated;
    }
}