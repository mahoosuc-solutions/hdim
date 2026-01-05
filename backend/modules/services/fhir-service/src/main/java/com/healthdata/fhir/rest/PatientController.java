package com.healthdata.fhir.rest;

import java.net.URI;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthdata.fhir.service.PatientService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.healthdata.auth.context.ScopedTenant;

/**
 * FHIR R4 Patient Resource Controller.
 *
 * Provides CRUD operations for Patient resources following HL7 FHIR R4 specification.
 */
@RestController
@RequestMapping(produces = "application/fhir+json")
@Tag(name = "Patient", description = "Patient demographics and administrative information")
@SecurityRequirement(name = "smart-oauth2")
public class PatientController {

    private static final String DEFAULT_TENANT = "tenant-1";
    private static final String DEFAULT_ACTOR = "admin-portal";

    private final PatientService patientService;
    private final IParser parser;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
        this.parser = FhirContext.forR4().newJsonParser().setPrettyPrint(false);
    }

    @Operation(
        summary = "Create a new Patient",
        description = "Creates a new Patient resource. The server will assign an ID and return the created resource with version.",
        operationId = "createPatient"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Patient created successfully",
            headers = @Header(name = "Location", description = "URL of the created Patient resource"),
            content = @Content(mediaType = "application/fhir+json", schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid Patient resource"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - missing or invalid authentication"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    @PostMapping(value = "/Patient", consumes = "application/fhir+json")
    @Audited(
            action = AuditAction.CREATE,
            resourceType = "Patient",
            purposeOfUse = "TREATMENT",
            description = "Create new patient record"
    )
    public ResponseEntity<String> createPatient(
            @Parameter(description = "Tenant ID for multi-tenant isolation", example = "tenant-1")
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "FHIR Patient resource in JSON format",
                required = true,
                content = @Content(mediaType = "application/fhir+json")
            )
            @RequestBody String body) {

        Patient patient = parser.parseResource(Patient.class, body);
        Patient created = patientService.createPatient(resolveTenant(tenantId), patient, DEFAULT_ACTOR);
        String payload = parser.encodeResourceToString(created);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/fhir/Patient/" + created.getIdElement().getIdPart())
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(payload);
    }

    @Operation(
        summary = "Read a Patient by ID",
        description = "Retrieves a specific Patient resource by its logical ID.",
        operationId = "readPatient"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Patient found",
            content = @Content(mediaType = "application/fhir+json")
        ),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/Patient/{id}")
    @Audited(
            action = AuditAction.READ,
            resourceType = "Patient",
            purposeOfUse = "TREATMENT",
            description = "Read patient record"
    )
    public ResponseEntity<String> getPatient(
            @Parameter(description = "Tenant ID for multi-tenant isolation")
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @Parameter(description = "Logical ID of the Patient resource", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") String id) {

        return patientService.getPatient(resolveTenant(tenantId), id)
                .map(patient -> ResponseEntity.ok()
                        .contentType(MediaType.valueOf("application/fhir+json"))
                        .body(parser.encodeResourceToString(patient)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @Operation(
        summary = "Search for Patients",
        description = "Searches for Patient resources matching the specified criteria. Returns a FHIR Bundle containing matching patients.",
        operationId = "searchPatients"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Search completed successfully",
            content = @Content(mediaType = "application/fhir+json", schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    @GetMapping("/Patient")
    @Audited(
            action = AuditAction.SEARCH,
            resourceType = "Patient",
            purposeOfUse = "TREATMENT",
            description = "Search patient records"
    )
    public ResponseEntity<String> searchPatients(
            @Parameter(description = "Tenant ID for multi-tenant isolation")
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @Parameter(description = "Patient family name (last name)", example = "Smith")
            @RequestParam(value = "family", required = false) String family,
            @Parameter(description = "Patient name (searches given and family)", example = "John")
            @RequestParam(value = "name", required = false) String name,
            @Parameter(description = "Maximum number of results to return", example = "20")
            @RequestParam(value = "_count", required = false, defaultValue = "20") int count) {

        String filter = family != null ? family : name;
        Bundle bundle = patientService.searchPatients(resolveTenant(tenantId), filter, count);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(parser.encodeResourceToString(bundle));
    }

    @Operation(
        summary = "Update a Patient",
        description = "Updates an existing Patient resource. The resource ID in the URL must match the resource ID in the body.",
        operationId = "updatePatient"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Patient updated successfully",
            content = @Content(mediaType = "application/fhir+json", schema = @Schema(implementation = String.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid Patient resource or ID mismatch"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "409", description = "Version conflict")
    })
    @PutMapping(value = "/Patient/{id}", consumes = "application/fhir+json")
    @Audited(
            action = AuditAction.UPDATE,
            resourceType = "Patient",
            purposeOfUse = "TREATMENT",
            description = "Update patient record"
    )
    public ResponseEntity<String> updatePatient(
            @Parameter(description = "Tenant ID for multi-tenant isolation")
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @Parameter(description = "Logical ID of the Patient resource to update", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") String id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Updated FHIR Patient resource in JSON format",
                required = true,
                content = @Content(mediaType = "application/fhir+json")
            )
            @RequestBody String body) {

        Patient patient = parser.parseResource(Patient.class, body);
        Patient updated = patientService.updatePatient(resolveTenant(tenantId), id, patient, DEFAULT_ACTOR);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(parser.encodeResourceToString(updated));
    }

    @Operation(
        summary = "Delete a Patient",
        description = "Deletes a Patient resource by its logical ID. This performs a soft delete for HIPAA compliance - the record is marked as deleted but retained for audit purposes.",
        operationId = "deletePatient"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Patient deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Patient not found")
    })
    @DeleteMapping("/Patient/{id}")
    @Audited(
            action = AuditAction.DELETE,
            resourceType = "Patient",
            purposeOfUse = "OPERATIONS",
            description = "Delete patient record (soft delete for HIPAA compliance)"
    )
    public ResponseEntity<Void> deletePatient(
            @Parameter(description = "Tenant ID for multi-tenant isolation")
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @Parameter(description = "Logical ID of the Patient resource to delete", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable("id") String id) {

        patientService.deletePatient(resolveTenant(tenantId), id, DEFAULT_ACTOR);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(PatientService.PatientValidationException.class)
    public ResponseEntity<String> handleValidation(PatientService.PatientValidationException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(PatientService.PatientNotFoundException.class)
    public ResponseEntity<Void> handleNotFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    private String resolveTenant(String tenantId) {
        return ScopedTenant.currentTenant().orElseGet(() ->
                (tenantId == null || tenantId.isBlank()) ? DEFAULT_TENANT : tenantId);
    }
}
