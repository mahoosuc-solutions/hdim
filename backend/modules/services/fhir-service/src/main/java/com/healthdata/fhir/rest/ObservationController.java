package com.healthdata.fhir.rest;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.healthdata.fhir.service.ObservationService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@RestController
@RequestMapping("/fhir/Observation")
public class ObservationController {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);

    private final ObservationService observationService;

    public ObservationController(ObservationService observationService) {
        this.observationService = observationService;
    }

    /**
     * Create a new Observation resource
     * POST /fhir/Observation
     */
    @PostMapping(consumes = "application/fhir+json", produces = "application/fhir+json")
    public ResponseEntity<String> createObservation(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @RequestBody String observationJson) {
        try {
            Observation observation = (Observation) JSON_PARSER.parseResource(observationJson);
            Observation created = observationService.createObservation(tenantId, observation, userId);
            String responseJson = JSON_PARSER.encodeResourceToString(created);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .header("Location", "/fhir/Observation/" + created.getId())
                    .body(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Read an Observation resource by ID
     * GET /fhir/Observation/{id}
     */
    @GetMapping(value = "/{id}", produces = "application/fhir+json")
    public ResponseEntity<String> getObservation(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String id) {
        return observationService.getObservation(tenantId, id)
                .map(observation -> {
                    String responseJson = JSON_PARSER.encodeResourceToString(observation);
                    return ResponseEntity.ok(responseJson);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update an Observation resource
     * PUT /fhir/Observation/{id}
     */
    @PutMapping(value = "/{id}", consumes = "application/fhir+json", produces = "application/fhir+json")
    public ResponseEntity<String> updateObservation(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable String id,
            @RequestBody String observationJson) {
        try {
            Observation observation = (Observation) JSON_PARSER.parseResource(observationJson);
            Observation updated = observationService.updateObservation(tenantId, id, observation, userId);
            String responseJson = JSON_PARSER.encodeResourceToString(updated);
            return ResponseEntity.ok(responseJson);
        } catch (ObservationService.ObservationNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Delete an Observation resource
     * DELETE /fhir/Observation/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteObservation(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable String id) {
        try {
            observationService.deleteObservation(tenantId, id, userId);
            return ResponseEntity.noContent().build();
        } catch (ObservationService.ObservationNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Search Observations by patient
     * GET /fhir/Observation?patient={patientId}
     */
    @GetMapping(produces = "application/fhir+json")
    public ResponseEntity<String> searchObservations(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(value = "patient", required = false) String patientId,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "date", required = false) String date,
            @PageableDefault(size = 20) Pageable pageable) {

        try {
            Bundle bundle;

            if (patientId != null && code != null) {
                // Search by patient and code
                bundle = observationService.searchObservationsByPatientAndCode(
                        tenantId, patientId, code);
            } else if (patientId != null && category != null) {
                // Search by patient and category
                bundle = observationService.searchObservationsByPatientAndCategory(
                        tenantId, patientId, category);
            } else if (patientId != null && date != null) {
                // Search by patient and date range
                String[] dateRange = date.split("/");
                if (dateRange.length != 2) {
                    return ResponseEntity.badRequest()
                            .body("{\"error\": \"Date parameter must be in format: startDate/endDate\"}");
                }
                LocalDateTime startDate = LocalDateTime.parse(dateRange[0]);
                LocalDateTime endDate = LocalDateTime.parse(dateRange[1]);
                bundle = observationService.searchObservationsByPatientAndDateRange(
                        tenantId, patientId, startDate, endDate);
            } else if (patientId != null) {
                // Search by patient only (with pagination)
                bundle = observationService.searchObservationsByPatient(tenantId, patientId, pageable);
            } else {
                return ResponseEntity.badRequest()
                        .body("{\"error\": \"patient parameter is required\"}");
            }

            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"Invalid date format. Use ISO 8601 format\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get lab results for a patient
     * GET /fhir/Observation/lab-results?patient={patientId}
     */
    @GetMapping(value = "/lab-results", produces = "application/fhir+json")
    public ResponseEntity<String> getLabResults(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId) {
        try {
            Bundle bundle = observationService.getLabResultsByPatient(tenantId, patientId);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get vital signs for a patient
     * GET /fhir/Observation/vital-signs?patient={patientId}
     */
    @GetMapping(value = "/vital-signs", produces = "application/fhir+json")
    public ResponseEntity<String> getVitalSigns(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId) {
        try {
            Bundle bundle = observationService.getVitalSignsByPatient(tenantId, patientId);
            String responseJson = JSON_PARSER.encodeResourceToString(bundle);
            return ResponseEntity.ok(responseJson);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * Get latest observation for a patient and code
     * GET /fhir/Observation/latest?patient={patientId}&code={code}
     */
    @GetMapping(value = "/latest", produces = "application/fhir+json")
    public ResponseEntity<String> getLatestObservation(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam("patient") String patientId,
            @RequestParam("code") String code) {
        return observationService.getLatestObservationByPatientAndCode(tenantId, patientId, code)
                .map(observation -> {
                    String responseJson = JSON_PARSER.encodeResourceToString(observation);
                    return ResponseEntity.ok(responseJson);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Health check endpoint
     * GET /fhir/Observation/_health
     */
    @GetMapping("/_health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("{\"status\": \"UP\", \"service\": \"Observation\"}");
    }
}
