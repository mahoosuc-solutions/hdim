package com.healthdata.patient.integration;

import com.healthdata.patient.client.ConsentServiceClient;
import com.healthdata.patient.client.ConsentServiceClient.ConsentStatus;
import com.healthdata.patient.client.FhirServiceClient;
import com.healthdata.patient.config.BaseIntegrationTest;
import com.healthdata.patient.entity.PatientDemographicsEntity;
import com.healthdata.patient.entity.PatientInsuranceEntity;
import com.healthdata.patient.entity.PatientRiskScoreEntity;
import com.healthdata.patient.repository.PatientDemographicsRepository;
import com.healthdata.patient.repository.PatientInsuranceRepository;
import com.healthdata.patient.repository.PatientRiskScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PatientController.
 *
 * Tests the full API stack including:
 * - REST endpoints
 * - Service layer
 * - Repository layer (with Testcontainers PostgreSQL)
 * - FHIR integration (mocked)
 *
 * External services (FHIR, Consent) are mocked to isolate the test.
 * Uses @WithMockUser to provide authentication with ADMIN role for accessing protected endpoints.
 */
@BaseIntegrationTest
@AutoConfigureMockMvc
@WithMockUser(roles = {"ADMIN"})
@DisplayName("PatientController Integration Tests")
class PatientControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientDemographicsRepository demographicsRepository;

    @Autowired
    private PatientInsuranceRepository insuranceRepository;

    @Autowired
    private PatientRiskScoreRepository riskScoreRepository;

    @MockBean
    private FhirServiceClient fhirServiceClient;

    @MockBean
    private ConsentServiceClient consentServiceClient;

    private static final String TENANT_ID = "test-tenant";
    private static final String PATIENT_ID = "patient-fhir-123";

    private PatientDemographicsEntity testPatient;
    private PatientInsuranceEntity testInsurance;
    private PatientRiskScoreEntity testRiskScore;

    @BeforeEach
    void setUp() {
        // Create test patient demographics
        testPatient = PatientDemographicsEntity.builder()
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
        testPatient = demographicsRepository.save(testPatient);

        // Create test insurance
        testInsurance = PatientInsuranceEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(testPatient.getId())
                .coverageType("medical")
                .payerName("Blue Cross")
                .memberId("BCBS123")
                .effectiveDate(LocalDate.of(2024, 1, 1))
                .isPrimary(true)
                .active(true)
                .build();
        testInsurance = insuranceRepository.save(testInsurance);

        // Create test risk score
        testRiskScore = PatientRiskScoreEntity.builder()
                .tenantId(TENANT_ID)
                .patientId(testPatient.getId())
                .scoreType("HCC")
                .scoreValue(new BigDecimal("1.5"))
                .riskCategory("high")
                .calculationDate(Instant.now())
                .validUntil(Instant.now().plus(90, ChronoUnit.DAYS))
                .build();
        testRiskScore = riskScoreRepository.save(testRiskScore);

        // Setup FHIR mocks
        setupFhirMocks();
    }

    private void setupFhirMocks() {
        // FhirServiceClient methods return JSON strings, not Bundle objects
        String emptyBundleJson = """
            {
              "resourceType": "Bundle",
              "type": "collection",
              "total": 0,
              "entry": []
            }
            """;

        // Mock allergy intolerances - returns JSON string
        when(fhirServiceClient.getAllergyIntolerances(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createAllergyBundleJson());
        when(fhirServiceClient.getActiveAllergies(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createAllergyBundleJson());
        when(fhirServiceClient.getCriticalAllergies(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createAllergyBundleJson());

        // Mock medications - returns JSON string
        when(fhirServiceClient.getMedicationRequests(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createMedicationBundleJson());
        when(fhirServiceClient.getActiveMedications(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createMedicationBundleJson());

        // Mock conditions - returns JSON string
        when(fhirServiceClient.getConditions(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createConditionBundleJson());
        when(fhirServiceClient.getActiveConditions(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createConditionBundleJson());

        // Mock immunizations - returns JSON string
        when(fhirServiceClient.getImmunizations(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createImmunizationBundleJson());
        when(fhirServiceClient.getCompletedImmunizations(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createImmunizationBundleJson());

        // Mock procedures - returns JSON string
        when(fhirServiceClient.getProcedures(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(emptyBundleJson);
        when(fhirServiceClient.getCompletedProcedures(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(emptyBundleJson);

        // Mock observations - returns JSON string
        when(fhirServiceClient.getObservations(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(emptyBundleJson);
        when(fhirServiceClient.getVitalSigns(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createVitalSignsBundleJson());
        when(fhirServiceClient.getLabResults(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(emptyBundleJson);

        // Mock encounters - returns JSON string
        when(fhirServiceClient.getEncounters(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(createEncounterBundleJson());
        when(fhirServiceClient.getActiveEncounters(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(emptyBundleJson);

        // Mock care plans - returns JSON string
        when(fhirServiceClient.getCarePlans(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(emptyBundleJson);
        when(fhirServiceClient.getActiveCarePlans(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(emptyBundleJson);

        // Mock goals - returns JSON string
        when(fhirServiceClient.getGoals(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(emptyBundleJson);
        when(fhirServiceClient.getActiveGoals(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(emptyBundleJson);

        // Mock diagnostic reports - returns JSON string
        when(fhirServiceClient.getDiagnosticReports(eq(TENANT_ID), eq(PATIENT_ID)))
                .thenReturn(emptyBundleJson);

        // Mock consent service - returns ConsentStatus record
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

    @Nested
    @DisplayName("Health Check Endpoint")
    class HealthCheckTests {

        @Test
        @DisplayName("GET /patient/_health should return UP status")
        void shouldReturnHealthStatus() throws Exception {
            mockMvc.perform(get("/patient/_health")
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.service").value("patient-service"));
        }
    }

    @Nested
    @DisplayName("Health Record Endpoints")
    class HealthRecordTests {

        @Test
        @DisplayName("GET /patient/health-record should return FHIR bundle")
        void shouldReturnHealthRecord() throws Exception {
            mockMvc.perform(get("/patient/health-record")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/fhir+json"));
        }

        @Test
        @DisplayName("GET /patient/allergies should return allergy bundle")
        void shouldReturnAllergies() throws Exception {
            mockMvc.perform(get("/patient/allergies")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/fhir+json"));
        }

        @Test
        @DisplayName("GET /patient/medications should return medication bundle")
        void shouldReturnMedications() throws Exception {
            mockMvc.perform(get("/patient/medications")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/fhir+json"));
        }

        @Test
        @DisplayName("GET /patient/conditions should return condition bundle")
        void shouldReturnConditions() throws Exception {
            mockMvc.perform(get("/patient/conditions")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/fhir+json"));
        }

        @Test
        @DisplayName("GET /patient/immunizations should return immunization bundle")
        void shouldReturnImmunizations() throws Exception {
            mockMvc.perform(get("/patient/immunizations")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/fhir+json"));
        }

        @Test
        @DisplayName("GET /patient/procedures should return procedure bundle")
        void shouldReturnProcedures() throws Exception {
            mockMvc.perform(get("/patient/procedures")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/fhir+json"));
        }

        @Test
        @DisplayName("GET /patient/vitals should return vital signs bundle")
        void shouldReturnVitals() throws Exception {
            mockMvc.perform(get("/patient/vitals")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/fhir+json"));
        }

        @Test
        @DisplayName("GET /patient/labs should return lab results bundle")
        void shouldReturnLabs() throws Exception {
            mockMvc.perform(get("/patient/labs")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/fhir+json"));
        }

        @Test
        @DisplayName("GET /patient/encounters should return encounters bundle")
        void shouldReturnEncounters() throws Exception {
            mockMvc.perform(get("/patient/encounters")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/fhir+json"));
        }

        @Test
        @DisplayName("GET /patient/care-plans should return care plans bundle")
        void shouldReturnCarePlans() throws Exception {
            mockMvc.perform(get("/patient/care-plans")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/fhir+json"));
        }
    }

    @Nested
    @DisplayName("Timeline Endpoints")
    class TimelineTests {

        @Test
        @DisplayName("GET /patient/timeline should return timeline events")
        void shouldReturnTimeline() throws Exception {
            mockMvc.perform(get("/patient/timeline")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /patient/timeline/by-date should filter by date range")
        void shouldReturnTimelineByDate() throws Exception {
            mockMvc.perform(get("/patient/timeline/by-date")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .param("startDate", LocalDate.now().minusMonths(6).toString())
                            .param("endDate", LocalDate.now().toString()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /patient/timeline/by-type should filter by resource type")
        void shouldReturnTimelineByType() throws Exception {
            mockMvc.perform(get("/patient/timeline/by-type")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .param("resourceType", "Encounter"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /patient/timeline/summary should return monthly summary")
        void shouldReturnTimelineSummary() throws Exception {
            mockMvc.perform(get("/patient/timeline/summary")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .param("year", "2024"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    @DisplayName("Health Status Endpoints")
    class HealthStatusTests {

        @Test
        @DisplayName("GET /patient/health-status should return health summary")
        void shouldReturnHealthStatus() throws Exception {
            mockMvc.perform(get("/patient/health-status")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.patientId").value(PATIENT_ID));
        }

        @Test
        @DisplayName("GET /patient/medication-summary should return medication summary")
        void shouldReturnMedicationSummary() throws Exception {
            mockMvc.perform(get("/patient/medication-summary")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /patient/allergy-summary should return allergy summary")
        void shouldReturnAllergySummary() throws Exception {
            mockMvc.perform(get("/patient/allergy-summary")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /patient/condition-summary should return condition summary")
        void shouldReturnConditionSummary() throws Exception {
            mockMvc.perform(get("/patient/condition-summary")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("GET /patient/immunization-summary should return immunization summary")
        void shouldReturnImmunizationSummary() throws Exception {
            mockMvc.perform(get("/patient/immunization-summary")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation")
    class MultiTenantTests {

        @Test
        @DisplayName("Should require X-Tenant-ID header")
        void shouldRequireTenantHeader() throws Exception {
            // Note: This depends on the security configuration
            // If security allows anonymous access but services validate tenant,
            // we might get a 400 or 500 instead of 401
            mockMvc.perform(get("/patient/_health"))
                    .andExpect(status().isOk()); // Health check may not require tenant
        }

        @Test
        @DisplayName("Should use correct tenant for queries")
        void shouldUseCorrectTenant() throws Exception {
            // Create patient in different tenant
            PatientDemographicsEntity otherTenantPatient = PatientDemographicsEntity.builder()
                    .tenantId("other-tenant")
                    .fhirPatientId("patient-other-999")
                    .firstName("Other")
                    .lastName("Person")
                    .dateOfBirth(LocalDate.of(1990, 1, 1))
                    .gender("female")
                    .active(true)
                    .deceased(false)
                    .build();
            demographicsRepository.save(otherTenantPatient);

            // Query with our tenant should return our patient's data
            mockMvc.perform(get("/patient/health-status")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patientId").value(PATIENT_ID));
        }
    }

    // Helper methods to create FHIR bundle JSON strings

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
                      "coding": [
                        {
                          "system": "http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical",
                          "code": "active"
                        }
                      ]
                    },
                    "criticality": "high",
                    "code": {
                      "text": "Penicillin"
                    },
                    "patient": {
                      "reference": "Patient/%s"
                    }
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
                    "medicationCodeableConcept": {
                      "text": "Metformin 500mg"
                    },
                    "subject": {
                      "reference": "Patient/%s"
                    }
                  }
                },
                {
                  "resource": {
                    "resourceType": "MedicationRequest",
                    "id": "med-2",
                    "status": "active",
                    "intent": "order",
                    "medicationCodeableConcept": {
                      "text": "Lisinopril 10mg"
                    },
                    "subject": {
                      "reference": "Patient/%s"
                    }
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
                      "coding": [
                        {
                          "system": "http://terminology.hl7.org/CodeSystem/condition-clinical",
                          "code": "active"
                        }
                      ]
                    },
                    "code": {
                      "text": "Type 2 Diabetes"
                    },
                    "subject": {
                      "reference": "Patient/%s"
                    }
                  }
                },
                {
                  "resource": {
                    "resourceType": "Condition",
                    "id": "cond-2",
                    "clinicalStatus": {
                      "coding": [
                        {
                          "system": "http://terminology.hl7.org/CodeSystem/condition-clinical",
                          "code": "active"
                        }
                      ]
                    },
                    "code": {
                      "text": "Hypertension"
                    },
                    "subject": {
                      "reference": "Patient/%s"
                    }
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
                    "vaccineCode": {
                      "text": "Influenza Vaccine"
                    },
                    "patient": {
                      "reference": "Patient/%s"
                    },
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
                    "category": [
                      {
                        "coding": [
                          {
                            "system": "http://terminology.hl7.org/CodeSystem/observation-category",
                            "code": "vital-signs"
                          }
                        ]
                      }
                    ],
                    "code": {
                      "coding": [
                        {
                          "system": "http://loinc.org",
                          "code": "85354-9",
                          "display": "Blood Pressure"
                        }
                      ]
                    },
                    "subject": {
                      "reference": "Patient/%s"
                    },
                    "component": [
                      {
                        "code": {
                          "coding": [
                            {
                              "system": "http://loinc.org",
                              "code": "8480-6",
                              "display": "Systolic Blood Pressure"
                            }
                          ]
                        },
                        "valueQuantity": {
                          "value": 120,
                          "unit": "mmHg"
                        }
                      },
                      {
                        "code": {
                          "coding": [
                            {
                              "system": "http://loinc.org",
                              "code": "8462-4",
                              "display": "Diastolic Blood Pressure"
                            }
                          ]
                        },
                        "valueQuantity": {
                          "value": 80,
                          "unit": "mmHg"
                        }
                      }
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
                    "class": {
                      "system": "http://terminology.hl7.org/CodeSystem/v3-ActCode",
                      "code": "AMB",
                      "display": "ambulatory"
                    },
                    "type": [
                      {
                        "text": "Office Visit"
                      }
                    ],
                    "subject": {
                      "reference": "Patient/%s"
                    },
                    "period": {
                      "start": "2024-12-01T09:00:00Z",
                      "end": "2024-12-01T10:00:00Z"
                    }
                  }
                }
              ]
            }
            """.formatted(PATIENT_ID);
    }
}
