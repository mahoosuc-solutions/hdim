package com.healthdata.patient.controller;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.healthdata.patient.service.PatientAggregationService;
import com.healthdata.patient.service.PatientHealthStatusService;
import com.healthdata.patient.service.PatientTimelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 */
@RestController
@RequestMapping("/patient")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Patient Management",
    description = """
        APIs for managing patient records and clinical data aggregation.

        Provides comprehensive patient health record access including:
        - Complete health record (all resources)
        - Allergies and intolerances
        - Immunizations
        - Medications
        - Conditions and diagnoses
        - Procedures
        - Vital signs and lab results
        - Encounters and care plans
        - Patient timeline visualization
        - Health status dashboards

        All endpoints require JWT authentication and X-Tenant-ID header.
        All responses include Cache-Control: no-store headers for HIPAA compliance.
        """
)
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
    @Operation(
        summary = "Get comprehensive patient health record",
        description = """
            Retrieves complete patient health record as a FHIR R4 Bundle.

            Includes all clinical resources for the patient:
            - Demographics and identifiers
            - Allergies and intolerances
            - Immunizations
            - Medications (active and historical)
            - Conditions and diagnoses
            - Procedures
            - Vital signs and observations
            - Lab results
            - Encounters
            - Care plans

            Response includes Cache-Control: no-store header for HIPAA compliance.
            All operations are audited for PHI access tracking.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Patient health record retrieved successfully",
            content = @Content(
                mediaType = "application/fhir+json",
                schema = @Schema(implementation = String.class),
                examples = @ExampleObject(
                    name = "FHIR Bundle Response",
                    value = """
                        {
                          "resourceType": "Bundle",
                          "type": "searchset",
                          "total": 45,
                          "entry": [
                            {
                              "resource": {
                                "resourceType": "Patient",
                                "id": "550e8400-e29b-41d4-a716-446655440000",
                                "name": [{"family": "Doe", "given": ["John"]}],
                                "birthDate": "1980-05-15"
                              }
                            },
                            {
                              "resource": {
                                "resourceType": "AllergyIntolerance",
                                "id": "allergy-1",
                                "patient": {"reference": "Patient/550e8400-e29b-41d4-a716-446655440000"},
                                "code": {"text": "Penicillin"},
                                "criticality": "high"
                              }
                            }
                          ]
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Patient not found for the given ID and tenant"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Access denied - insufficient permissions or wrong tenant"
        )
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/health-record", produces = "application/fhir+json")
    public ResponseEntity<String> getComprehensiveHealthRecord(
            @Parameter(
                description = "Tenant ID for multi-tenant isolation",
                required = true,
                example = "tenant-123"
            )
            @RequestHeader("X-Tenant-ID") String tenantId,

            @Parameter(
                description = "Patient ID (FHIR resource ID or UUID)",
                required = true,
                example = "550e8400-e29b-41d4-a716-446655440000"
            )
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
    @Operation(
        summary = "Get patient allergies and intolerances",
        description = """
            Retrieves patient allergy and intolerance information as a FHIR R4 Bundle.

            Can filter for only critical allergies (e.g., anaphylaxis, severe reactions).
            Includes coded allergy information (SNOMED CT, RxNorm) and reaction details.

            Use for clinical decision support, medication prescribing safety checks.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Allergies retrieved successfully",
            content = @Content(
                mediaType = "application/fhir+json",
                schema = @Schema(implementation = String.class),
                examples = @ExampleObject(
                    name = "Allergy Bundle",
                    value = """
                        {
                          "resourceType": "Bundle",
                          "type": "searchset",
                          "total": 2,
                          "entry": [{
                            "resource": {
                              "resourceType": "AllergyIntolerance",
                              "id": "allergy-1",
                              "clinicalStatus": {"coding": [{"code": "active"}]},
                              "code": {"coding": [{"system": "http://snomed.info/sct", "code": "91936005", "display": "Penicillin"}]},
                              "criticality": "high",
                              "reaction": [{"manifestation": [{"coding": [{"code": "anaphylaxis"}]}]}]
                            }
                          }]
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/allergies", produces = "application/fhir+json")
    public ResponseEntity<String> getAllergies(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam("patient") String patientId,
            @Parameter(description = "Return only critical allergies", example = "false")
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
    @Operation(
        summary = "Get patient immunization history",
        description = """
            Retrieves patient immunization records as a FHIR R4 Bundle.

            Filter for completed immunizations only to exclude declined/not-done records.
            Includes CVX codes, administration dates, lot numbers, and providers.

            Use for immunization compliance, quality measure evaluation (HEDIS).
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Immunizations retrieved successfully",
            content = @Content(
                mediaType = "application/fhir+json",
                schema = @Schema(implementation = String.class),
                examples = @ExampleObject(
                    name = "Immunization Bundle",
                    value = """
                        {
                          "resourceType": "Bundle",
                          "type": "searchset",
                          "total": 5,
                          "entry": [{
                            "resource": {
                              "resourceType": "Immunization",
                              "id": "imm-1",
                              "status": "completed",
                              "vaccineCode": {"coding": [{"system": "http://hl7.org/fhir/sid/cvx", "code": "08", "display": "Hep B"}]},
                              "occurrenceDateTime": "2024-01-15"
                            }
                          }]
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/immunizations", produces = "application/fhir+json")
    public ResponseEntity<String> getImmunizations(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam("patient") String patientId,
            @Parameter(description = "Return only completed immunizations", example = "false")
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
    @Operation(
        summary = "Get patient medications",
        description = """
            Retrieves patient medication list as a FHIR R4 Bundle (MedicationRequest resources).

            Filter for active medications only (default) or include historical medications.
            Includes RxNorm codes, dosage instructions, prescribers, and dates.

            Use for medication reconciliation, drug interaction checks, quality measures.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Medications retrieved successfully",
            content = @Content(
                mediaType = "application/fhir+json",
                schema = @Schema(implementation = String.class),
                examples = @ExampleObject(
                    name = "Medication Bundle",
                    value = """
                        {
                          "resourceType": "Bundle",
                          "type": "searchset",
                          "total": 3,
                          "entry": [{
                            "resource": {
                              "resourceType": "MedicationRequest",
                              "id": "med-1",
                              "status": "active",
                              "medicationCodeableConcept": {"coding": [{"system": "http://www.nlm.nih.gov/research/umls/rxnorm", "code": "860975", "display": "Lisinopril 10 MG"}]},
                              "dosageInstruction": [{"text": "Take 1 tablet by mouth daily"}]
                            }
                          }]
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/medications", produces = "application/fhir+json")
    public ResponseEntity<String> getMedications(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam("patient") String patientId,
            @Parameter(description = "Return only active medications (default: true)", example = "true")
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
    @Operation(
        summary = "Get patient conditions and diagnoses",
        description = """
            Retrieves patient problem list and diagnoses as a FHIR R4 Bundle (Condition resources).

            Filter for active conditions only (default) or include resolved/inactive conditions.
            Includes ICD-10, SNOMED CT codes, onset dates, and clinical status.

            Use for risk stratification, care gap identification, HCC coding.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Conditions retrieved successfully",
            content = @Content(
                mediaType = "application/fhir+json",
                schema = @Schema(implementation = String.class),
                examples = @ExampleObject(
                    name = "Condition Bundle",
                    value = """
                        {
                          "resourceType": "Bundle",
                          "type": "searchset",
                          "total": 4,
                          "entry": [{
                            "resource": {
                              "resourceType": "Condition",
                              "id": "cond-1",
                              "clinicalStatus": {"coding": [{"code": "active"}]},
                              "code": {"coding": [{"system": "http://hl7.org/fhir/sid/icd-10-cm", "code": "E11.9", "display": "Type 2 diabetes mellitus without complications"}]},
                              "onsetDateTime": "2020-03-15"
                            }
                          }]
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/conditions", produces = "application/fhir+json")
    public ResponseEntity<String> getConditions(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam("patient") String patientId,
            @Parameter(description = "Return only active conditions (default: true)", example = "true")
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
    @Operation(
        summary = "Get patient procedures",
        description = """
            Retrieves patient procedure history as a FHIR R4 Bundle (Procedure resources).

            Includes completed procedures with CPT/HCPCS codes, dates, and providers.
            Use for quality measure evaluation, care gap identification, utilization review.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Procedures retrieved successfully",
            content = @Content(
                mediaType = "application/fhir+json",
                schema = @Schema(implementation = String.class),
                examples = @ExampleObject(
                    name = "Procedure Bundle",
                    value = """
                        {
                          "resourceType": "Bundle",
                          "type": "searchset",
                          "total": 6,
                          "entry": [{
                            "resource": {
                              "resourceType": "Procedure",
                              "id": "proc-1",
                              "status": "completed",
                              "code": {"coding": [{"system": "http://www.ama-assn.org/go/cpt", "code": "99213", "display": "Office visit"}]},
                              "performedDateTime": "2024-01-10"
                            }
                          }]
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/procedures", produces = "application/fhir+json")
    public ResponseEntity<String> getProcedures(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
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
    @Operation(
        summary = "Get patient vital signs",
        description = """
            Retrieves patient vital signs as a FHIR R4 Bundle (Observation resources).

            Includes blood pressure, heart rate, temperature, weight, height, BMI, O2 saturation.
            Uses LOINC codes for standardized observation types.

            Use for clinical monitoring, quality measure evaluation, care gap tracking.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Vital signs retrieved successfully",
            content = @Content(
                mediaType = "application/fhir+json",
                schema = @Schema(implementation = String.class),
                examples = @ExampleObject(
                    name = "Vitals Bundle",
                    value = """
                        {
                          "resourceType": "Bundle",
                          "type": "searchset",
                          "total": 8,
                          "entry": [{
                            "resource": {
                              "resourceType": "Observation",
                              "id": "vitals-1",
                              "status": "final",
                              "category": [{"coding": [{"code": "vital-signs"}]}],
                              "code": {"coding": [{"system": "http://loinc.org", "code": "85354-9", "display": "Blood pressure"}]},
                              "valueQuantity": {"value": 120, "unit": "mmHg"},
                              "effectiveDateTime": "2024-01-15"
                            }
                          }]
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/vitals", produces = "application/fhir+json")
    public ResponseEntity<String> getVitalSigns(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
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
    @Operation(
        summary = "Get patient laboratory results",
        description = """
            Retrieves patient lab results as a FHIR R4 Bundle (Observation resources).

            Includes chemistry panels, CBC, lipid panels, HbA1c, and other lab tests.
            Uses LOINC codes for standardized lab test identification.

            Use for clinical monitoring, diabetes management, quality measure evaluation.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lab results retrieved successfully",
            content = @Content(
                mediaType = "application/fhir+json",
                schema = @Schema(implementation = String.class),
                examples = @ExampleObject(
                    name = "Lab Results Bundle",
                    value = """
                        {
                          "resourceType": "Bundle",
                          "type": "searchset",
                          "total": 12,
                          "entry": [{
                            "resource": {
                              "resourceType": "Observation",
                              "id": "lab-1",
                              "status": "final",
                              "category": [{"coding": [{"code": "laboratory"}]}],
                              "code": {"coding": [{"system": "http://loinc.org", "code": "4548-4", "display": "HbA1c"}]},
                              "valueQuantity": {"value": 7.2, "unit": "%"},
                              "effectiveDateTime": "2024-01-10"
                            }
                          }]
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/labs", produces = "application/fhir+json")
    public ResponseEntity<String> getLabResults(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
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
    @Operation(
        summary = "Get patient encounters",
        description = """
            Retrieves patient encounter history as a FHIR R4 Bundle (Encounter resources).

            Filter for active encounters only or include finished encounters.
            Includes visit types, dates, providers, locations, and diagnoses.

            Use for utilization tracking, quality measure evaluation, continuity of care.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Encounters retrieved successfully",
            content = @Content(
                mediaType = "application/fhir+json",
                schema = @Schema(implementation = String.class),
                examples = @ExampleObject(
                    name = "Encounter Bundle",
                    value = """
                        {
                          "resourceType": "Bundle",
                          "type": "searchset",
                          "total": 7,
                          "entry": [{
                            "resource": {
                              "resourceType": "Encounter",
                              "id": "enc-1",
                              "status": "finished",
                              "class": {"code": "AMB", "display": "ambulatory"},
                              "type": [{"coding": [{"code": "99213", "display": "Office visit"}]}],
                              "period": {"start": "2024-01-15T09:00:00Z", "end": "2024-01-15T09:30:00Z"}
                            }
                          }]
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/encounters", produces = "application/fhir+json")
    public ResponseEntity<String> getEncounters(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam("patient") String patientId,
            @Parameter(description = "Return only active encounters", example = "false")
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
    @Operation(
        summary = "Get patient care plans",
        description = """
            Retrieves patient care plans as a FHIR R4 Bundle (CarePlan resources).

            Filter for active care plans only (default) or include completed care plans.
            Includes goals, activities, care team members, and addresses.

            Use for care coordination, chronic disease management, transitional care.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Care plans retrieved successfully",
            content = @Content(
                mediaType = "application/fhir+json",
                schema = @Schema(implementation = String.class),
                examples = @ExampleObject(
                    name = "CarePlan Bundle",
                    value = """
                        {
                          "resourceType": "Bundle",
                          "type": "searchset",
                          "total": 2,
                          "entry": [{
                            "resource": {
                              "resourceType": "CarePlan",
                              "id": "cp-1",
                              "status": "active",
                              "intent": "plan",
                              "category": [{"coding": [{"code": "diabetes-management"}]}],
                              "title": "Diabetes Care Plan",
                              "period": {"start": "2024-01-01"}
                            }
                          }]
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/care-plans", produces = "application/fhir+json")
    public ResponseEntity<String> getCarePlans(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam("patient") String patientId,
            @Parameter(description = "Return only active care plans (default: true)", example = "true")
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
    @Operation(
        summary = "Get patient timeline (all events)",
        description = """
            Retrieves complete patient clinical timeline sorted chronologically.

            Includes all clinical events: encounters, procedures, lab results, medications,
            immunizations, conditions, and vital signs.

            Returns simplified timeline view optimized for UI visualization.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Timeline retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Timeline Events",
                    value = """
                        [
                          {
                            "eventDate": "2024-01-15",
                            "eventType": "Encounter",
                            "title": "Office Visit",
                            "description": "Annual physical examination",
                            "resourceId": "enc-1"
                          },
                          {
                            "eventDate": "2024-01-10",
                            "eventType": "Observation",
                            "title": "HbA1c Lab Result",
                            "description": "7.2%",
                            "resourceId": "lab-1"
                          }
                        ]
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/timeline", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PatientTimelineService.TimelineEvent>> getPatientTimeline(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
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
    @Operation(
        summary = "Get patient timeline by date range",
        description = """
            Retrieves patient clinical timeline filtered by date range.

            Useful for focused review of specific time periods (e.g., last 6 months, past year).
            Includes all event types within the specified date range.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Timeline retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Timeline Events",
                    value = """
                        [
                          {
                            "eventDate": "2024-01-15",
                            "eventType": "Encounter",
                            "title": "Office Visit",
                            "description": "Follow-up for diabetes"
                          }
                        ]
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid date range"),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/timeline/by-date", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PatientTimelineService.TimelineEvent>> getPatientTimelineByDateRange(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam("patient") String patientId,
            @Parameter(description = "Start date (ISO 8601 format)", required = true, example = "2024-01-01")
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date (ISO 8601 format)", required = true, example = "2024-12-31")
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
    @Operation(
        summary = "Get patient timeline by resource type",
        description = """
            Retrieves patient clinical timeline filtered by FHIR resource type.

            Filter for specific event types: Encounter, Observation, Procedure, MedicationRequest,
            Condition, Immunization, AllergyIntolerance.

            Useful for focused clinical review (e.g., all lab results, all procedures).
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Timeline retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Timeline Events",
                    value = """
                        [
                          {
                            "eventDate": "2024-01-10",
                            "eventType": "Observation",
                            "title": "HbA1c",
                            "description": "7.2%"
                          }
                        ]
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid resource type"),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/timeline/by-type", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PatientTimelineService.TimelineEvent>> getPatientTimelineByResourceType(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam("patient") String patientId,
            @Parameter(description = "FHIR resource type to filter", required = true, example = "Observation")
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
    @Operation(
        summary = "Get monthly timeline summary",
        description = """
            Retrieves monthly event count summary for a specific year.

            Returns aggregated counts by month for timeline visualization (e.g., bar charts).
            Useful for identifying periods of high clinical activity.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Timeline summary retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Monthly Summary",
                    value = """
                        {
                          "2024-01": 12,
                          "2024-02": 8,
                          "2024-03": 15
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid year"),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/timeline/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Integer>> getTimelineSummaryByMonth(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam("patient") String patientId,
            @Parameter(description = "Year to summarize", required = true, example = "2024")
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
    @Operation(
        summary = "Get comprehensive health status dashboard",
        description = """
            Retrieves aggregated health status summary for dashboard display.

            Includes counts and summaries for: active conditions, medications, allergies,
            recent vitals, upcoming appointments, care gaps, and risk scores.

            Optimized for clinical overview dashboards and patient portals.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Health status summary retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Health Status Summary",
                    value = """
                        {
                          "activeConditions": 4,
                          "activeMedications": 3,
                          "criticalAllergies": 1,
                          "recentVitals": {"bloodPressure": "120/80", "weight": "180 lbs"},
                          "upcomingAppointments": 2,
                          "openCareGaps": 3,
                          "riskScore": 72
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/health-status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientHealthStatusService.HealthStatusSummary> getHealthStatusSummary(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
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
    @Operation(
        summary = "Get medication summary dashboard",
        description = """
            Retrieves medication summary for dashboard display.

            Includes: total active medications, medication classes, adherence status,
            recent prescriptions, and upcoming refills.

            Useful for medication reconciliation and adherence monitoring.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Medication summary retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Medication Summary",
                    value = """
                        {
                          "totalActive": 3,
                          "medicationClasses": ["antihypertensive", "antidiabetic", "statin"],
                          "adherenceRate": 85,
                          "recentPrescriptions": 2,
                          "upcomingRefills": 1
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/medication-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientHealthStatusService.MedicationSummary> getMedicationSummary(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
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
    @Operation(
        summary = "Get allergy summary dashboard",
        description = """
            Retrieves allergy summary for dashboard display.

            Includes: total allergies, critical allergies, allergy categories (drug, food, environmental),
            and recent reactions.

            Useful for prescribing safety checks and allergy management.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Allergy summary retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Allergy Summary",
                    value = """
                        {
                          "totalAllergies": 2,
                          "criticalAllergies": 1,
                          "allergyCategories": {"drug": 1, "food": 1},
                          "recentReactions": 0
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/allergy-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientHealthStatusService.AllergySummary> getAllergySummary(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
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
    @Operation(
        summary = "Get condition summary dashboard",
        description = """
            Retrieves condition/diagnosis summary for dashboard display.

            Includes: total active conditions, chronic conditions, condition categories,
            HCC risk scores, and recent diagnoses.

            Useful for care management, risk stratification, and HCC coding.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Condition summary retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Condition Summary",
                    value = """
                        {
                          "totalActive": 4,
                          "chronicConditions": 3,
                          "conditionCategories": {"endocrine": 1, "cardiovascular": 2, "respiratory": 1},
                          "hccScore": 72,
                          "recentDiagnoses": 1
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/condition-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientHealthStatusService.ConditionSummary> getConditionSummary(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
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
    @Operation(
        summary = "Get immunization summary dashboard",
        description = """
            Retrieves immunization summary for dashboard display.

            Includes: total immunizations, up-to-date status, overdue immunizations,
            recent immunizations, and upcoming recommendations.

            Useful for immunization compliance tracking and HEDIS quality measures.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Immunization summary retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Immunization Summary",
                    value = """
                        {
                          "totalImmunizations": 12,
                          "upToDate": true,
                          "overdueImmunizations": 0,
                          "recentImmunizations": 2,
                          "upcomingRecommendations": 1
                        }
                        """
                )
            )
        ),
        @ApiResponse(responseCode = "404", description = "Patient not found"),
        @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PreAuthorize("hasPermission('PATIENT_READ')")
    @GetMapping(value = "/immunization-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PatientHealthStatusService.ImmunizationSummary> getImmunizationSummary(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Patient ID", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
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
    @Operation(
        summary = "Service health check",
        description = """
            Simple health check endpoint for monitoring service availability.

            Returns service name, status, and timestamp.
            Use for container health checks, load balancer monitoring.
            """,
        security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Service is healthy",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "Health Check Response",
                    value = """
                        {
                          "status": "UP",
                          "service": "patient-service",
                          "timestamp": "2024-01-24"
                        }
                        """
                )
            )
        )
    })
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
