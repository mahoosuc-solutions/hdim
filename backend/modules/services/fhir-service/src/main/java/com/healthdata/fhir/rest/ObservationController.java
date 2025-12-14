package com.healthdata.fhir.rest;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * FHIR R4 Observation Resource Controller.
 *
 * Provides CRUD and search operations for Observation resources including
 * lab results, vital signs, and other clinical observations.
 */
@RestController
@RequestMapping("/fhir/Observation")
@Tag(name = "Observation", description = "Clinical observations including vital signs, lab results, and social history")
@SecurityRequirement(name = "smart-oauth2")
public class ObservationController {

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser().setPrettyPrint(true);

    private final ObservationService observationService;

    public ObservationController(ObservationService observationService) {
        this.observationService = observationService;
    }

    @Operation(
        summary = "Create a new Observation",
        description = "Creates a new Observation resource (lab result, vital sign, or other clinical observation).",
        operationId = "createObservation"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Observation created successfully",
            headers = @Header(name = "Location", description = "URL of the created Observation"),
            content = @Content(mediaType = "application/fhir+json")
        ),
        @ApiResponse(responseCode = "400", description = "Invalid Observation resource"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PostMapping(consumes = "application/fhir+json", produces = "application/fhir+json")
    public ResponseEntity<String> createObservation(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User ID performing the action")
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "FHIR Observation resource in JSON format",
                required = true,
                content = @Content(mediaType = "application/fhir+json")
            )
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

    @Operation(
        summary = "Read an Observation by ID",
        description = "Retrieves a specific Observation resource by its logical ID.",
        operationId = "readObservation"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Observation found", content = @Content(mediaType = "application/fhir+json")),
        @ApiResponse(responseCode = "404", description = "Observation not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping(value = "/{id}", produces = "application/fhir+json")
    public ResponseEntity<String> getObservation(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Logical ID of the Observation", required = true)
            @PathVariable String id) {
        return observationService.getObservation(tenantId, id)
                .map(observation -> {
                    String responseJson = JSON_PARSER.encodeResourceToString(observation);
                    return ResponseEntity.ok(responseJson);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Update an Observation",
        description = "Updates an existing Observation resource.",
        operationId = "updateObservation"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Observation updated successfully", content = @Content(mediaType = "application/fhir+json")),
        @ApiResponse(responseCode = "400", description = "Invalid Observation resource"),
        @ApiResponse(responseCode = "404", description = "Observation not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @PutMapping(value = "/{id}", consumes = "application/fhir+json", produces = "application/fhir+json")
    public ResponseEntity<String> updateObservation(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User ID performing the action")
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "Logical ID of the Observation to update", required = true)
            @PathVariable String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Updated FHIR Observation resource",
                required = true,
                content = @Content(mediaType = "application/fhir+json")
            )
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

    @Operation(
        summary = "Delete an Observation",
        description = "Deletes an Observation resource by its logical ID.",
        operationId = "deleteObservation"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Observation deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Observation not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteObservation(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "User ID performing the action")
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @Parameter(description = "Logical ID of the Observation to delete", required = true)
            @PathVariable String id) {
        try {
            observationService.deleteObservation(tenantId, id, userId);
            return ResponseEntity.noContent().build();
        } catch (ObservationService.ObservationNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(
        summary = "Search for Observations",
        description = "Searches for Observation resources. Supports filtering by patient, code, category, and date range.",
        operationId = "searchObservations"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search completed successfully", content = @Content(mediaType = "application/fhir+json")),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping(produces = "application/fhir+json")
    public ResponseEntity<String> searchObservations(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient reference (required)", example = "Patient/123")
            @RequestParam(value = "patient", required = false) String patientId,
            @Parameter(description = "Observation code (LOINC)", example = "8480-6")
            @RequestParam(value = "code", required = false) String code,
            @Parameter(description = "Observation category", example = "vital-signs")
            @RequestParam(value = "category", required = false) String category,
            @Parameter(description = "Date range in format: startDate/endDate (ISO 8601)")
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

    @Operation(
        summary = "Get lab results for a patient",
        description = "Retrieves all laboratory result observations for a specific patient.",
        operationId = "getLabResults"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lab results found", content = @Content(mediaType = "application/fhir+json")),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping(value = "/lab-results", produces = "application/fhir+json")
    public ResponseEntity<String> getLabResults(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
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

    @Operation(
        summary = "Get vital signs for a patient",
        description = "Retrieves all vital sign observations for a specific patient (blood pressure, heart rate, temperature, etc.).",
        operationId = "getVitalSigns"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Vital signs found", content = @Content(mediaType = "application/fhir+json")),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping(value = "/vital-signs", produces = "application/fhir+json")
    public ResponseEntity<String> getVitalSigns(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
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

    @Operation(
        summary = "Get latest observation by code",
        description = "Retrieves the most recent observation for a patient with a specific code (e.g., latest blood pressure reading).",
        operationId = "getLatestObservation"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Observation found", content = @Content(mediaType = "application/fhir+json")),
        @ApiResponse(responseCode = "404", description = "No observation found for the specified criteria"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping(value = "/latest", produces = "application/fhir+json")
    public ResponseEntity<String> getLatestObservation(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @RequestParam("patient") String patientId,
            @Parameter(description = "LOINC code for the observation", required = true, example = "8480-6")
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
