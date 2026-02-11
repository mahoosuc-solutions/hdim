package com.healthdata.patient.api;

import com.healthdata.openapi.OpenApiComplianceTestBase;
import com.healthdata.patient.client.ConsentServiceClient;
import com.healthdata.patient.client.ConsentServiceClient.ConsentStatus;
import com.healthdata.patient.client.FhirServiceClient;
import com.healthdata.patient.entity.PatientDemographicsEntity;
import com.healthdata.patient.repository.PatientDemographicsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OpenAPI Compliance Tests for Patient Service APIs.
 * <p>
 * These tests validate that the API responses conform to the documented OpenAPI specification.
 * This ensures API contract stability and catches specification drift before deployment.
 * </p>
 *
 * <h2>Coverage:</h2>
 * <ul>
 *     <li>Patient List API - /api/v1/patients (paginated listing)</li>
 *     <li>Patient Health Record API - /patient/* (FHIR bundles, timeline, status)</li>
 *     <li>Health Check API - /patient/_health</li>
 * </ul>
 *
 * @see OpenApiComplianceTestBase
 */
@Tag("integration")
@WithMockUser(roles = {"ADMIN"})
@DisplayName("Patient API OpenAPI Compliance")
class PatientApiComplianceTest extends OpenApiComplianceTestBase {

    @Autowired
    private PatientDemographicsRepository patientDemographicsRepository;

    @MockBean
    private FhirServiceClient fhirServiceClient;

    @MockBean
    private ConsentServiceClient consentServiceClient;

    private static final String TENANT_ID = "test-tenant";
    private static final String PATIENT_ID = "patient-fhir-123";

    private PatientDemographicsEntity testPatient;

    @BeforeEach
    void setUpTestData() {
        // Create test patient demographics
        testPatient = PatientDemographicsEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .fhirPatientId(PATIENT_ID)
                .mrn("MRN-001")
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .gender("male")
                .active(true)
                .deceased(false)
                .build();
        testPatient = patientDemographicsRepository.save(testPatient);

        // Setup mocks for external services
        setupFhirMocks();
        setupConsentMocks();
    }

    private void setupFhirMocks() {
        String emptyBundleJson = """
            {
              "resourceType": "Bundle",
              "type": "collection",
              "total": 0,
              "entry": []
            }
            """;

        // Mock all FHIR endpoints to return valid bundles
        when(fhirServiceClient.getAllergyIntolerances(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createAllergyBundleJson());
        when(fhirServiceClient.getActiveAllergies(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createAllergyBundleJson());
        when(fhirServiceClient.getCriticalAllergies(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createAllergyBundleJson());
        when(fhirServiceClient.getMedicationRequests(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createMedicationBundleJson());
        when(fhirServiceClient.getActiveMedications(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createMedicationBundleJson());
        when(fhirServiceClient.getConditions(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createConditionBundleJson());
        when(fhirServiceClient.getActiveConditions(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createConditionBundleJson());
        when(fhirServiceClient.getImmunizations(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createImmunizationBundleJson());
        when(fhirServiceClient.getCompletedImmunizations(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createImmunizationBundleJson());
        when(fhirServiceClient.getProcedures(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(emptyBundleJson);
        when(fhirServiceClient.getCompletedProcedures(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(emptyBundleJson);
        when(fhirServiceClient.getObservations(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(emptyBundleJson);
        when(fhirServiceClient.getVitalSigns(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createVitalSignsBundleJson());
        when(fhirServiceClient.getLabResults(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(emptyBundleJson);
        when(fhirServiceClient.getEncounters(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createEncounterBundleJson());
        when(fhirServiceClient.getActiveEncounters(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(emptyBundleJson);
        when(fhirServiceClient.getCarePlans(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(emptyBundleJson);
        when(fhirServiceClient.getActiveCarePlans(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(emptyBundleJson);
        when(fhirServiceClient.getGoals(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(emptyBundleJson);
        when(fhirServiceClient.getActiveGoals(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(emptyBundleJson);
        when(fhirServiceClient.getDiagnosticReports(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(emptyBundleJson);
    }

    private void setupConsentMocks() {
        ConsentStatus activeConsent = new ConsentStatus(
                "active",
                LocalDate.now().minusYears(1).toString(),
                LocalDate.now().plusYears(1).toString(),
                false
        );
        when(consentServiceClient.getConsentStatus(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(activeConsent);
        when(consentServiceClient.getRestrictedResourceTypes(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(Collections.emptyList());
        when(consentServiceClient.checkAccess(eq(TENANT_ID), eq(PATIENT_ID), anyString(), anyString()))
                .thenReturn(true);
        when(consentServiceClient.getSensitiveCategories(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(Collections.emptyList());
    }

    // ==================== Patient List API Tests ====================

    @Nested
    @DisplayName("GET /api/v1/patients - Patient List")
    class PatientListApiTests {

        @Test
        @DisplayName("Should return 200 with patients matching OpenAPI spec")
        void listPatients_shouldReturn200WithPatients() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/patients")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("page", "0")
                            .param("size", "20")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            assertResponseMatchesSpec(result);
        }

        @Test
        @DisplayName("Should return 200 with empty results matching OpenAPI spec")
        void listPatients_shouldReturn200EmptyResults() throws Exception {
            // Query with different tenant to get empty results
            MvcResult result = mockMvc.perform(get("/api/v1/patients")
                            .header("X-Tenant-ID", "nonexistent-tenant")
                            .param("page", "0")
                            .param("size", "20")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            assertResponseMatchesSpec(result);
        }

        @Test
        @DisplayName("Should accept pagination parameters per OpenAPI spec")
        void listPatients_shouldAcceptPaginationParams() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/v1/patients")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("page", "1")
                            .param("size", "10")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            assertResponseMatchesSpec(result);
        }
    }

    // ==================== Health Record API Tests ====================

    @Nested
    @DisplayName("GET /patient/* - Health Record Endpoints")
    class HealthRecordApiTests {

        @Test
        @DisplayName("GET /patient/health-record should return FHIR bundle matching OpenAPI spec")
        void getHealthRecord_shouldReturn200() throws Exception {
            MvcResult result = mockMvc.perform(get("/patient/health-record")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .accept("application/fhir+json"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertResponseMatchesSpec(result);
        }

        @Test
        @DisplayName("GET /patient/allergies should return allergy bundle matching OpenAPI spec")
        void getAllergies_shouldReturn200() throws Exception {
            MvcResult result = mockMvc.perform(get("/patient/allergies")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .accept("application/fhir+json"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertResponseMatchesSpec(result);
        }

        @Test
        @DisplayName("GET /patient/medications should return medication bundle matching OpenAPI spec")
        void getMedications_shouldReturn200() throws Exception {
            MvcResult result = mockMvc.perform(get("/patient/medications")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .accept("application/fhir+json"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertResponseMatchesSpec(result);
        }

        @Test
        @DisplayName("GET /patient/conditions should return condition bundle matching OpenAPI spec")
        void getConditions_shouldReturn200() throws Exception {
            MvcResult result = mockMvc.perform(get("/patient/conditions")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .accept("application/fhir+json"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertResponseMatchesSpec(result);
        }

        @Test
        @DisplayName("GET /patient/immunizations should return immunization bundle matching OpenAPI spec")
        void getImmunizations_shouldReturn200() throws Exception {
            MvcResult result = mockMvc.perform(get("/patient/immunizations")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .accept("application/fhir+json"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertResponseMatchesSpec(result);
        }

        @Test
        @DisplayName("GET /patient/vitals should return vital signs bundle matching OpenAPI spec")
        void getVitals_shouldReturn200() throws Exception {
            MvcResult result = mockMvc.perform(get("/patient/vitals")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .accept("application/fhir+json"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertResponseMatchesSpec(result);
        }

        @Test
        @DisplayName("GET /patient/encounters should return encounter bundle matching OpenAPI spec")
        void getEncounters_shouldReturn200() throws Exception {
            MvcResult result = mockMvc.perform(get("/patient/encounters")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .accept("application/fhir+json"))
                    .andExpect(status().isOk())
                    .andReturn();

            assertResponseMatchesSpec(result);
        }
    }

    // ==================== Timeline API Tests ====================

    @Nested
    @DisplayName("GET /patient/timeline/* - Timeline Endpoints")
    class TimelineApiTests {

        @Test
        @DisplayName("GET /patient/timeline should return timeline events matching OpenAPI spec")
        void getTimeline_shouldReturn200() throws Exception {
            MvcResult result = mockMvc.perform(get("/patient/timeline")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            assertResponseMatchesSpec(result);
        }

        @Test
        @DisplayName("GET /patient/timeline/by-date should filter by date range matching OpenAPI spec")
        void getTimelineByDate_shouldReturn200() throws Exception {
            MvcResult result = mockMvc.perform(get("/patient/timeline/by-date")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .param("startDate", LocalDate.now().minusMonths(6).toString())
                            .param("endDate", LocalDate.now().toString())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            assertResponseMatchesSpec(result);
        }

        @Test
        @DisplayName("GET /patient/timeline/by-type should filter by resource type matching OpenAPI spec")
        void getTimelineByType_shouldReturn200() throws Exception {
            MvcResult result = mockMvc.perform(get("/patient/timeline/by-type")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .param("resourceType", "Observation")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            assertResponseMatchesSpec(result);
        }

        @Test
        @DisplayName("GET /patient/timeline/summary should return monthly summary matching OpenAPI spec")
        void getTimelineSummary_shouldReturn200() throws Exception {
            MvcResult result = mockMvc.perform(get("/patient/timeline/summary")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .param("year", "2024")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            assertResponseMatchesSpec(result);
        }
    }

    // ==================== Health Status API Tests ====================

    @Nested
    @DisplayName("GET /patient/*-summary - Health Status Endpoints")
    class HealthStatusApiTests {

        @Test
        @DisplayName("GET /patient/health-status should return health summary matching OpenAPI spec")
        void getHealthStatus_shouldReturn200() throws Exception {
            MvcResult result = mockMvc.perform(get("/patient/health-status")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            assertResponseMatchesSpec(result);
        }

        @Test
        @DisplayName("GET /patient/medication-summary should return medication summary matching OpenAPI spec")
        void getMedicationSummary_shouldReturn200() throws Exception {
            MvcResult result = mockMvc.perform(get("/patient/medication-summary")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            assertResponseMatchesSpec(result);
        }

        @Test
        @DisplayName("GET /patient/allergy-summary should return allergy summary matching OpenAPI spec")
        void getAllergySummary_shouldReturn200() throws Exception {
            MvcResult result = mockMvc.perform(get("/patient/allergy-summary")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            assertResponseMatchesSpec(result);
        }

        @Test
        @DisplayName("GET /patient/condition-summary should return condition summary matching OpenAPI spec")
        void getConditionSummary_shouldReturn200() throws Exception {
            MvcResult result = mockMvc.perform(get("/patient/condition-summary")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            assertResponseMatchesSpec(result);
        }

        @Test
        @DisplayName("GET /patient/immunization-summary should return immunization summary matching OpenAPI spec")
        void getImmunizationSummary_shouldReturn200() throws Exception {
            MvcResult result = mockMvc.perform(get("/patient/immunization-summary")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            assertResponseMatchesSpec(result);
        }
    }

    // ==================== Health Check API Tests ====================

    @Nested
    @DisplayName("GET /patient/_health - Health Check")
    class HealthCheckApiTests {

        @Test
        @DisplayName("Should return 200 UP status matching OpenAPI spec")
        void healthCheck_shouldReturn200() throws Exception {
            MvcResult result = mockMvc.perform(get("/patient/_health")
                            .header("X-Tenant-ID", TENANT_ID)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andReturn();

            assertResponseMatchesSpec(result);
        }
    }

    // ==================== Helper Methods for FHIR Bundle JSON ====================

    private String createAllergyBundleJson() {
        return """
            {
              "resourceType": "Bundle",
              "type": "collection",
              "total": 1,
              "entry": [
                {
                  "resource": {
                    "resourceType": "AllergyIntolerance",
                    "id": "allergy-1",
                    "clinicalStatus": {
                      "coding": [{"system": "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical", "code": "active"}]
                    },
                    "criticality": "high",
                    "code": {"text": "Penicillin"},
                    "patient": {"reference": "Patient/%s"}
                  }
                }
              ]
            }
            """.formatted(PATIENT_ID);
    }

    private String createMedicationBundleJson() {
        return """
            {
              "resourceType": "Bundle",
              "type": "collection",
              "total": 2,
              "entry": [
                {
                  "resource": {
                    "resourceType": "MedicationRequest",
                    "id": "med-1",
                    "status": "active",
                    "intent": "order",
                    "medicationCodeableConcept": {"text": "Metformin 500mg"},
                    "subject": {"reference": "Patient/%s"}
                  }
                },
                {
                  "resource": {
                    "resourceType": "MedicationRequest",
                    "id": "med-2",
                    "status": "active",
                    "intent": "order",
                    "medicationCodeableConcept": {"text": "Lisinopril 10mg"},
                    "subject": {"reference": "Patient/%s"}
                  }
                }
              ]
            }
            """.formatted(PATIENT_ID, PATIENT_ID);
    }

    private String createConditionBundleJson() {
        return """
            {
              "resourceType": "Bundle",
              "type": "collection",
              "total": 2,
              "entry": [
                {
                  "resource": {
                    "resourceType": "Condition",
                    "id": "cond-1",
                    "clinicalStatus": {
                      "coding": [{"system": "http://terminology.hl7.org/CodeSystem/condition-clinical", "code": "active"}]
                    },
                    "code": {"text": "Type 2 Diabetes"},
                    "subject": {"reference": "Patient/%s"}
                  }
                },
                {
                  "resource": {
                    "resourceType": "Condition",
                    "id": "cond-2",
                    "clinicalStatus": {
                      "coding": [{"system": "http://terminology.hl7.org/CodeSystem/condition-clinical", "code": "active"}]
                    },
                    "code": {"text": "Hypertension"},
                    "subject": {"reference": "Patient/%s"}
                  }
                }
              ]
            }
            """.formatted(PATIENT_ID, PATIENT_ID);
    }

    private String createImmunizationBundleJson() {
        return """
            {
              "resourceType": "Bundle",
              "type": "collection",
              "total": 1,
              "entry": [
                {
                  "resource": {
                    "resourceType": "Immunization",
                    "id": "imm-1",
                    "status": "completed",
                    "vaccineCode": {"text": "Influenza Vaccine"},
                    "patient": {"reference": "Patient/%s"},
                    "occurrenceDateTime": "2024-10-15"
                  }
                }
              ]
            }
            """.formatted(PATIENT_ID);
    }

    private String createVitalSignsBundleJson() {
        return """
            {
              "resourceType": "Bundle",
              "type": "collection",
              "total": 1,
              "entry": [
                {
                  "resource": {
                    "resourceType": "Observation",
                    "id": "bp-1",
                    "status": "final",
                    "category": [{"coding": [{"system": "http://terminology.hl7.org/CodeSystem/observation-category", "code": "vital-signs"}]}],
                    "code": {"coding": [{"system": "http://loinc.org", "code": "85354-9", "display": "Blood Pressure"}]},
                    "subject": {"reference": "Patient/%s"},
                    "component": [
                      {"code": {"coding": [{"system": "http://loinc.org", "code": "8480-6", "display": "Systolic Blood Pressure"}]}, "valueQuantity": {"value": 120, "unit": "mmHg"}},
                      {"code": {"coding": [{"system": "http://loinc.org", "code": "8462-4", "display": "Diastolic Blood Pressure"}]}, "valueQuantity": {"value": 80, "unit": "mmHg"}}
                    ]
                  }
                }
              ]
            }
            """.formatted(PATIENT_ID);
    }

    private String createEncounterBundleJson() {
        return """
            {
              "resourceType": "Bundle",
              "type": "collection",
              "total": 1,
              "entry": [
                {
                  "resource": {
                    "resourceType": "Encounter",
                    "id": "enc-1",
                    "status": "finished",
                    "class": {"system": "http://terminology.hl7.org/CodeSystem/v3-ActCode", "code": "AMB", "display": "ambulatory"},
                    "type": [{"text": "Office Visit"}],
                    "subject": {"reference": "Patient/%s"},
                    "period": {"start": "2024-12-01T09:00:00Z", "end": "2024-12-01T10:00:00Z"}
                  }
                }
              ]
            }
            """.formatted(PATIENT_ID);
    }
}
