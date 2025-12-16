package com.healthdata.patient.controller;

import com.healthdata.patient.service.PatientAggregationService;
import com.healthdata.patient.service.PatientHealthStatusService;
import com.healthdata.patient.service.PatientTimelineService;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for PatientController.
 * Tests REST API endpoints for patient data aggregation and health status.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Patient Controller Tests")
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PatientAggregationService aggregationService;

    @MockBean
    private PatientTimelineService timelineService;

    @MockBean
    private PatientHealthStatusService healthStatusService;

    private static final String TENANT_ID = "tenant-123";
    private static final String PATIENT_ID = "patient-456";

    @Nested
    @DisplayName("GET /patient/health-record Tests")
    class GetHealthRecordTests {

        @Test
        @DisplayName("Should return FHIR bundle for health record")
        void shouldReturnFhirBundle() throws Exception {
            // Given
            Bundle bundle = createEmptyBundle();
            when(aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID))
                    .thenReturn(bundle);

            // When/Then
            mockMvc.perform(get("/patient/health-record")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/fhir+json"));
        }
    }

    @Nested
    @DisplayName("GET /patient/allergies Tests")
    class GetAllergiesTests {

        @Test
        @DisplayName("Should return allergies bundle")
        void shouldReturnAllergies() throws Exception {
            // Given
            Bundle bundle = createAllergyBundle();
            when(aggregationService.getAllergies(TENANT_ID, PATIENT_ID, false))
                    .thenReturn(bundle);

            // When/Then
            mockMvc.perform(get("/patient/allergies")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/fhir+json"));
        }

        @Test
        @DisplayName("Should filter critical allergies")
        void shouldFilterCriticalAllergies() throws Exception {
            // Given
            Bundle bundle = createEmptyBundle();
            when(aggregationService.getAllergies(TENANT_ID, PATIENT_ID, true))
                    .thenReturn(bundle);

            // When/Then
            mockMvc.perform(get("/patient/allergies")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .param("onlyCritical", "true"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /patient/medications Tests")
    class GetMedicationsTests {

        @Test
        @DisplayName("Should return medications bundle")
        void shouldReturnMedications() throws Exception {
            // Given
            Bundle bundle = createEmptyBundle();
            when(aggregationService.getMedications(TENANT_ID, PATIENT_ID, true))
                    .thenReturn(bundle);

            // When/Then
            mockMvc.perform(get("/patient/medications")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/fhir+json"));
        }
    }

    @Nested
    @DisplayName("GET /patient/conditions Tests")
    class GetConditionsTests {

        @Test
        @DisplayName("Should return conditions bundle")
        void shouldReturnConditions() throws Exception {
            // Given
            Bundle bundle = createEmptyBundle();
            when(aggregationService.getConditions(TENANT_ID, PATIENT_ID, true))
                    .thenReturn(bundle);

            // When/Then
            mockMvc.perform(get("/patient/conditions")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/fhir+json"));
        }
    }

    @Nested
    @DisplayName("GET /patient/timeline Tests")
    class GetTimelineTests {

        @Test
        @DisplayName("Should return patient timeline")
        void shouldReturnTimeline() throws Exception {
            // Given
            List<PatientTimelineService.TimelineEvent> events = List.of(
                    new PatientTimelineService.TimelineEvent(
                            "enc-1", "Encounter", LocalDate.now(), "Office Visit", "finished", null),
                    new PatientTimelineService.TimelineEvent(
                            "proc-1", "Procedure", LocalDate.now().minusDays(5), "Blood Test", "completed", null)
            );
            when(timelineService.getPatientTimeline(TENANT_ID, PATIENT_ID))
                    .thenReturn(events);

            // When/Then
            mockMvc.perform(get("/patient/timeline")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].resourceType").value("Encounter"));
        }
    }

    @Nested
    @DisplayName("GET /patient/timeline/by-date Tests")
    class GetTimelineByDateTests {

        @Test
        @DisplayName("Should return timeline filtered by date range")
        void shouldReturnTimelineByDateRange() throws Exception {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            List<PatientTimelineService.TimelineEvent> events = List.of(
                    new PatientTimelineService.TimelineEvent(
                            "enc-1", "Encounter", LocalDate.now().minusDays(10), "Office Visit", "finished", null)
            );
            when(timelineService.getPatientTimelineByDateRange(TENANT_ID, PATIENT_ID, startDate, endDate))
                    .thenReturn(events);

            // When/Then
            mockMvc.perform(get("/patient/timeline/by-date")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .param("startDate", startDate.toString())
                            .param("endDate", endDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /patient/timeline/by-type Tests")
    class GetTimelineByTypeTests {

        @Test
        @DisplayName("Should return timeline filtered by resource type")
        void shouldReturnTimelineByType() throws Exception {
            // Given
            List<PatientTimelineService.TimelineEvent> events = List.of(
                    new PatientTimelineService.TimelineEvent(
                            "enc-1", "Encounter", LocalDate.now(), "Office Visit", "finished", null)
            );
            when(timelineService.getPatientTimelineByResourceType(TENANT_ID, PATIENT_ID, "Encounter"))
                    .thenReturn(events);

            // When/Then
            mockMvc.perform(get("/patient/timeline/by-type")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .param("resourceType", "Encounter"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].resourceType").value("Encounter"));
        }
    }

    @Nested
    @DisplayName("GET /patient/timeline/summary Tests")
    class GetTimelineSummaryTests {

        @Test
        @DisplayName("Should return monthly timeline summary")
        void shouldReturnTimelineSummary() throws Exception {
            // Given
            Map<String, Integer> summary = Map.of(
                    "2024-01", 3,
                    "2024-02", 5,
                    "2024-03", 2
            );
            when(timelineService.getTimelineSummaryByMonth(TENANT_ID, PATIENT_ID, 2024))
                    .thenReturn(summary);

            // When/Then
            mockMvc.perform(get("/patient/timeline/summary")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID)
                            .param("year", "2024"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.['2024-01']").value(3))
                    .andExpect(jsonPath("$.['2024-02']").value(5));
        }
    }

    @Nested
    @DisplayName("GET /patient/health-status Tests")
    class GetHealthStatusTests {

        @Test
        @DisplayName("Should return health status summary")
        void shouldReturnHealthStatus() throws Exception {
            // Given
            PatientHealthStatusService.HealthStatusSummary summary =
                    new PatientHealthStatusService.HealthStatusSummary(
                            PATIENT_ID,
                            LocalDate.now(),
                            3, // activeConditionsCount
                            5, // activeMedicationsCount
                            1, // criticalAllergiesCount
                            12, // completedImmunizationsCount
                            2, // recentEncountersCount
                            1, // recentProceduresCount
                            1, // activeCarePlansCount
                            2, // activeGoalsCount
                            2, // recentActivityScore
                            List.of("Patient has 1 critical allergy"),
                            "compliant",
                            "low",
                            true,
                            15
                    );
            when(healthStatusService.getHealthStatusSummary(TENANT_ID, PATIENT_ID))
                    .thenReturn(summary);

            // When/Then
            mockMvc.perform(get("/patient/health-status")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.patientId").value(PATIENT_ID))
                    .andExpect(jsonPath("$.activeConditionsCount").value(3))
                    .andExpect(jsonPath("$.activeMedicationsCount").value(5))
                    .andExpect(jsonPath("$.immunizationComplianceStatus").value("compliant"));
        }
    }

    @Nested
    @DisplayName("GET /patient/medication-summary Tests")
    class GetMedicationSummaryTests {

        @Test
        @DisplayName("Should return medication summary")
        void shouldReturnMedicationSummary() throws Exception {
            // Given
            PatientHealthStatusService.MedicationSummary summary =
                    new PatientHealthStatusService.MedicationSummary(
                            5, 8, List.of("Metformin", "Lisinopril"), "low", false);
            when(healthStatusService.getMedicationSummary(TENANT_ID, PATIENT_ID))
                    .thenReturn(summary);

            // When/Then
            mockMvc.perform(get("/patient/medication-summary")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activeCount").value(5))
                    .andExpect(jsonPath("$.adherenceRisk").value("low"))
                    .andExpect(jsonPath("$.polypharmacyFlag").value(false));
        }
    }

    @Nested
    @DisplayName("GET /patient/allergy-summary Tests")
    class GetAllergySummaryTests {

        @Test
        @DisplayName("Should return allergy summary")
        void shouldReturnAllergySummary() throws Exception {
            // Given
            PatientHealthStatusService.AllergySummary summary =
                    new PatientHealthStatusService.AllergySummary(
                            1, 3, List.of("Penicillin"), true, true);
            when(healthStatusService.getAllergySummary(TENANT_ID, PATIENT_ID))
                    .thenReturn(summary);

            // When/Then
            mockMvc.perform(get("/patient/allergy-summary")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.criticalCount").value(1))
                    .andExpect(jsonPath("$.hasCriticalAllergies").value(true))
                    .andExpect(jsonPath("$.criticalAllergens[0]").value("Penicillin"));
        }
    }

    @Nested
    @DisplayName("GET /patient/condition-summary Tests")
    class GetConditionSummaryTests {

        @Test
        @DisplayName("Should return condition summary")
        void shouldReturnConditionSummary() throws Exception {
            // Given
            PatientHealthStatusService.ConditionSummary summary =
                    new PatientHealthStatusService.ConditionSummary(
                            3, 5, List.of("Type 2 Diabetes", "Hypertension"), true, "medium");
            when(healthStatusService.getConditionSummary(TENANT_ID, PATIENT_ID))
                    .thenReturn(summary);

            // When/Then
            mockMvc.perform(get("/patient/condition-summary")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activeCount").value(3))
                    .andExpect(jsonPath("$.hasChronicConditions").value(true))
                    .andExpect(jsonPath("$.severity").value("medium"));
        }
    }

    @Nested
    @DisplayName("GET /patient/immunization-summary Tests")
    class GetImmunizationSummaryTests {

        @Test
        @DisplayName("Should return immunization summary")
        void shouldReturnImmunizationSummary() throws Exception {
            // Given
            PatientHealthStatusService.ImmunizationSummary summary =
                    new PatientHealthStatusService.ImmunizationSummary(
                            12, 14, "compliant", List.of("Flu", "COVID-19", "Tdap"), true);
            when(healthStatusService.getImmunizationSummary(TENANT_ID, PATIENT_ID))
                    .thenReturn(summary);

            // When/Then
            mockMvc.perform(get("/patient/immunization-summary")
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("patient", PATIENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.completedCount").value(12))
                    .andExpect(jsonPath("$.complianceStatus").value("compliant"))
                    .andExpect(jsonPath("$.isCompliant").value(true));
        }
    }

    @Nested
    @DisplayName("GET /patient/_health Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return health status")
        void shouldReturnHealthStatus() throws Exception {
            // When/Then
            mockMvc.perform(get("/patient/_health")
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("UP"))
                    .andExpect(jsonPath("$.service").value("patient-service"));
        }
    }

    // ==================== Helper Methods ====================

    private Bundle createEmptyBundle() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setTotal(0);
        return bundle;
    }

    private Bundle createAllergyBundle() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setTotal(1);

        AllergyIntolerance allergy = new AllergyIntolerance();
        allergy.setId("allergy-1");
        allergy.setCriticality(AllergyIntolerance.AllergyIntoleranceCriticality.HIGH);
        allergy.getCode().setText("Penicillin");
        bundle.addEntry().setResource(allergy);

        return bundle;
    }
}
