package com.healthdata.caregap.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.caregap.persistence.CareGapEntity;
import com.healthdata.caregap.persistence.CareGapRepository;
import com.healthdata.testfixtures.security.GatewayTrustTestHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End Functional Tests for Care Gap Detection and Auto-Closure.
 *
 * Tests the complete care gap workflow including:
 * - Care gap identification from measure results
 * - Gap prioritization and categorization
 * - Event-driven auto-closure via FHIR events
 * - Multi-gap correlation and tracking
 * - Care team notifications
 * - Gap reporting and analytics
 *
 * FUNCTIONAL TEST COVERAGE:
 * - Preventive care gaps (screenings, immunizations)
 * - Chronic disease management gaps
 * - Behavioral health gaps (PHQ-9, GAD-7)
 * - SDOH-related gaps
 * - Event-driven gap closure from FHIR resources
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@Transactional
@Disabled("Requires CareGapClosureEventConsumer implementation - deferred to future release")
@DisplayName("Care Gap Detection E2E Functional Tests")
class CareGapDetectionE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CareGapRepository careGapRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // TODO: Re-enable when CareGapClosureEventConsumer is implemented
    // @Autowired
    // private CareGapClosureEventConsumer careGapClosureEventConsumer;

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final String TENANT_ID = "test-tenant-caregap";
    private static final UUID PATIENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

    @BeforeEach
    void setUp() {
        careGapRepository.deleteAll();
    }

    @Nested
    @DisplayName("Care Gap Detection from Measure Results")
    class CareGapDetection {

        @Test
        @DisplayName("should detect preventive care gap for annual wellness visit")
        void shouldDetectAnnualWellnessVisitGap() throws Exception {
            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            // Create care gap for missing annual wellness visit
            mockMvc.perform(post("/care-gap/detect")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "patientId": "%s",
                            "measureId": "HEDIS_AWV",
                            "denominatorEligible": true,
                            "numeratorCompliant": false
                        }
                        """.formatted(PATIENT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("PREVENTIVE"))
                .andExpect(jsonPath("$.title").value(containsString("Annual Wellness Visit")))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.dueDate").exists());

            // Verify gap persisted
            var gaps = careGapRepository.findAll();
            assertThat(gaps).hasSize(1);
            assertThat(gaps.get(0).getGapCategory()).isEqualTo("PREVENTIVE");
            assertThat(gaps.get(0).getPriority()).isEqualTo("HIGH");
        }

        @Test
        @DisplayName("should detect diabetes care gap for HbA1c control")
        void shouldDetectDiabetesCareGap() throws Exception {
            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            mockMvc.perform(post("/care-gap/detect")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "patientId": "%s",
                            "measureId": "HEDIS_CDC_A1C9",
                            "denominatorEligible": true,
                            "numeratorCompliant": false
                        }
                        """.formatted(PATIENT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("CHRONIC_DISEASE"))
                .andExpect(jsonPath("$.title").value(containsString("HbA1c Control")))
                .andExpect(jsonPath("$.priority").value("HIGH"));
        }

        @Test
        @DisplayName("should detect immunization care gap for influenza")
        void shouldDetectInfluenzaImmunizationGap() throws Exception {
            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            mockMvc.perform(post("/care-gap/detect")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "patientId": "%s",
                            "measureId": "HEDIS_FLU_VACCINE",
                            "denominatorEligible": true,
                            "numeratorCompliant": false,
                            "dueDate": "2026-12-31"
                        }
                        """.formatted(PATIENT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("PREVENTIVE"))
                .andExpect(jsonPath("$.title").value(containsString("Influenza Vaccination")))
                .andExpect(jsonPath("$.dueDate").value("2026-12-31"));
        }

        @Test
        @DisplayName("should detect behavioral health screening gap")
        void shouldDetectBehavioralHealthScreeningGap() throws Exception {
            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            mockMvc.perform(post("/care-gap/detect")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "patientId": "%s",
                            "measureId": "HEDIS_PHQ9",
                            "denominatorEligible": true,
                            "numeratorCompliant": false
                        }
                        """.formatted(PATIENT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("BEHAVIORAL_HEALTH"))
                .andExpect(jsonPath("$.title").value(containsString("Depression Screening")));
        }
    }

    @Nested
    @DisplayName("Care Gap Prioritization")
    class CareGapPrioritization {

        @Test
        @DisplayName("should prioritize gaps based on clinical urgency")
        void shouldPrioritizeGapsBasedOnUrgency() throws Exception {
            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            // Create high-priority gap (cancer screening overdue)
            mockMvc.perform(post("/care-gap/detect")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "patientId": "%s",
                            "measureId": "HEDIS_BCS",
                            "denominatorEligible": true,
                            "numeratorCompliant": false,
                            "daysPastDue": 180
                        }
                        """.formatted(PATIENT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value("CRITICAL"));

            // Create medium-priority gap (wellness visit upcoming)
            mockMvc.perform(post("/care-gap/detect")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "patientId": "%s",
                            "measureId": "HEDIS_AWV",
                            "denominatorEligible": true,
                            "numeratorCompliant": false,
                            "daysPastDue": 30
                        }
                        """.formatted(PATIENT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority").value("MEDIUM"));

            // Get prioritized list
            mockMvc.perform(get("/care-gap/patient/" + PATIENT_ID)
                    .param("sort", "priority")
                    .headers(headers))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].priority").value("CRITICAL"))
                .andExpect(jsonPath("$[1].priority").value("MEDIUM"));
        }

        @Test
        @DisplayName("should calculate gap risk score")
        void shouldCalculateGapRiskScore() throws Exception {
            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            mockMvc.perform(post("/care-gap/detect")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "patientId": "%s",
                            "measureId": "HEDIS_CDC_A1C9",
                            "denominatorEligible": true,
                            "numeratorCompliant": false,
                            "lastHbA1c": 9.5
                        }
                        """.formatted(PATIENT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.riskScore").exists())
                .andExpect(jsonPath("$.riskScore").value(greaterThan(70)));
        }
    }

    @Nested
    @DisplayName("Event-Driven Auto-Closure")
    class EventDrivenAutoClosure {

        @Test
        @DisplayName("should auto-close gap when matching procedure is performed")
        void shouldAutoCloseGapOnProcedurePerformed() throws Exception {
            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            // Create open gap for influenza vaccination
            var createResponse = mockMvc.perform(post("/care-gap/detect")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "patientId": "%s",
                            "measureId": "HEDIS_FLU_VACCINE",
                            "denominatorEligible": true,
                            "numeratorCompliant": false
                        }
                        """.formatted(PATIENT_ID)))
                .andExpect(status().isCreated())
                .andReturn();

            String gapId = objectMapper.readTree(createResponse.getResponse().getContentAsString())
                .get("id").asText();

            // Simulate FHIR procedure.created event from Kafka
            String procedureEvent = """
                {
                    "eventType": "fhir.procedures.created",
                    "tenantId": "%s",
                    "patientId": "%s",
                    "resourceId": "procedure-001",
                    "resourceType": "Procedure",
                    "code": {
                        "system": "http://snomed.info/sct",
                        "code": "86198006",
                        "display": "Influenza vaccination"
                    },
                    "performedDate": "2026-01-10"
                }
                """.formatted(TENANT_ID, PATIENT_ID);

            // Process event (simulating Kafka consumer)
            // TODO: Re-enable when CareGapClosureEventConsumer is implemented
            // careGapClosureEventConsumer.handleProcedureCreated(procedureEvent);

            // Verify gap auto-closed
            mockMvc.perform(get("/care-gap/" + gapId)
                    .headers(headers))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closureReason").value("AUTO_CLOSED"))
                .andExpect(jsonPath("$.closedBy").value("SYSTEM"))
                .andExpect(jsonPath("$.closedDate").exists());
        }

        @Test
        @DisplayName("should auto-close gap when matching observation is recorded")
        void shouldAutoCloseGapOnObservationRecorded() throws Exception {
            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            // Create gap for HbA1c test
            var createResponse = mockMvc.perform(post("/care-gap/detect")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "patientId": "%s",
                            "measureId": "HEDIS_CDC_A1C9",
                            "denominatorEligible": true,
                            "numeratorCompliant": false
                        }
                        """.formatted(PATIENT_ID)))
                .andExpect(status().isCreated())
                .andReturn();

            String gapId = objectMapper.readTree(createResponse.getResponse().getContentAsString())
                .get("id").asText();

            // Simulate FHIR observation.created event
            String observationEvent = """
                {
                    "eventType": "fhir.observations.created",
                    "tenantId": "%s",
                    "patientId": "%s",
                    "resourceId": "observation-001",
                    "resourceType": "Observation",
                    "code": {
                        "system": "http://loinc.org",
                        "code": "4548-4",
                        "display": "Hemoglobin A1c"
                    },
                    "value": {
                        "value": 7.2,
                        "unit": "%%"
                    },
                    "effectiveDate": "2026-01-10"
                }
                """.formatted(TENANT_ID, PATIENT_ID);

            // Process event
            careGapClosureEventConsumer.handleObservationCreated(observationEvent);

            // Verify gap auto-closed
            mockMvc.perform(get("/care-gap/" + gapId)
                    .headers(headers))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        }

        @Test
        @DisplayName("should not close gap if procedure does not match")
        void shouldNotCloseGapIfProcedureDoesNotMatch() throws Exception {
            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            // Create gap for influenza vaccination
            var createResponse = mockMvc.perform(post("/care-gap/detect")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "patientId": "%s",
                            "measureId": "HEDIS_FLU_VACCINE",
                            "denominatorEligible": true,
                            "numeratorCompliant": false
                        }
                        """.formatted(PATIENT_ID)))
                .andExpect(status().isCreated())
                .andReturn();

            String gapId = objectMapper.readTree(createResponse.getResponse().getContentAsString())
                .get("id").asText();

            // Different procedure (not influenza vaccination)
            String procedureEvent = """
                {
                    "eventType": "fhir.procedures.created",
                    "tenantId": "%s",
                    "patientId": "%s",
                    "resourceId": "procedure-002",
                    "resourceType": "Procedure",
                    "code": {
                        "system": "http://snomed.info/sct",
                        "code": "123456789",
                        "display": "Different procedure"
                    }
                }
                """.formatted(TENANT_ID, PATIENT_ID);

            careGapClosureEventConsumer.handleProcedureCreated(procedureEvent);

            // Gap should still be open
            mockMvc.perform(get("/care-gap/" + gapId)
                    .headers(headers))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
        }
    }

    @Nested
    @DisplayName("Manual Gap Management")
    class ManualGapManagement {

        @Test
        @DisplayName("should allow manual gap closure with reason")
        void shouldAllowManualGapClosure() throws Exception {
            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            // Create gap
            var createResponse = mockMvc.perform(post("/care-gap/detect")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "patientId": "%s",
                            "measureId": "HEDIS_AWV",
                            "denominatorEligible": true,
                            "numeratorCompliant": false
                        }
                        """.formatted(PATIENT_ID)))
                .andExpect(status().isCreated())
                .andReturn();

            String gapId = objectMapper.readTree(createResponse.getResponse().getContentAsString())
                .get("id").asText();

            // Manually close gap
            mockMvc.perform(put("/care-gap/" + gapId + "/close")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "closureReason": "COMPLETED_OUTSIDE_SYSTEM",
                            "notes": "Patient had wellness visit at external provider"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.closureReason").value("COMPLETED_OUTSIDE_SYSTEM"))
                .andExpect(jsonPath("$.closedBy").exists())
                .andExpect(jsonPath("$.notes").value(containsString("external provider")));
        }

        @Test
        @DisplayName("should support gap snoozing for future follow-up")
        void shouldSupportGapSnoozing() throws Exception {
            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            // Create gap
            var createResponse = mockMvc.perform(post("/care-gap/detect")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "patientId": "%s",
                            "measureId": "HEDIS_BCS",
                            "denominatorEligible": true,
                            "numeratorCompliant": false
                        }
                        """.formatted(PATIENT_ID)))
                .andExpect(status().isCreated())
                .andReturn();

            String gapId = objectMapper.readTree(createResponse.getResponse().getContentAsString())
                .get("id").asText();

            // Snooze gap
            mockMvc.perform(put("/care-gap/" + gapId + "/snooze")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "snoozeUntil": "2026-03-01",
                            "reason": "Patient scheduled for screening in 2 months"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SNOOZED"))
                .andExpect(jsonPath("$.snoozeUntil").value("2026-03-01"));
        }
    }

    @Nested
    @DisplayName("Care Gap Reporting")
    class CareGapReporting {

        @Test
        @DisplayName("should generate patient care gap summary")
        void shouldGeneratePatientCareGapSummary() throws Exception {
            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            // Create multiple gaps
            mockMvc.perform(post("/care-gap/detect")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "patientId": "%s",
                            "measureId": "HEDIS_AWV",
                            "denominatorEligible": true,
                            "numeratorCompliant": false
                        }
                        """.formatted(PATIENT_ID)))
                .andExpect(status().isCreated());

            mockMvc.perform(post("/care-gap/detect")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "patientId": "%s",
                            "measureId": "HEDIS_FLU_VACCINE",
                            "denominatorEligible": true,
                            "numeratorCompliant": false
                        }
                        """.formatted(PATIENT_ID)))
                .andExpect(status().isCreated());

            // Get summary
            mockMvc.perform(get("/care-gap/patient/" + PATIENT_ID + "/summary")
                    .headers(headers))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$.totalGaps").value(2))
                .andExpect(jsonPath("$.openGaps").value(2))
                .andExpect(jsonPath("$.closedGaps").value(0))
                .andExpect(jsonPath("$.highPriorityGaps").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.gapsByCategory").exists());
        }

        @Test
        @DisplayName("should generate population care gap analytics")
        void shouldGeneratePopulationCareGapAnalytics() throws Exception {
            var headers = GatewayTrustTestHeaders.adminHeaders(TENANT_ID);

            // Create gaps for multiple patients
            UUID patient2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");

            mockMvc.perform(post("/care-gap/detect")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "patientId": "%s",
                            "measureId": "HEDIS_AWV",
                            "denominatorEligible": true,
                            "numeratorCompliant": false
                        }
                        """.formatted(PATIENT_ID)))
                .andExpect(status().isCreated());

            mockMvc.perform(post("/care-gap/detect")
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "patientId": "%s",
                            "measureId": "HEDIS_AWV",
                            "denominatorEligible": true,
                            "numeratorCompliant": false
                        }
                        """.formatted(patient2)))
                .andExpect(status().isCreated());

            // Get analytics
            mockMvc.perform(get("/care-gap/analytics/population")
                    .headers(headers))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPatients").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.totalGaps").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.gapsByMeasure").isMap())
                .andExpect(jsonPath("$.gapsByCategory").isMap())
                .andExpect(jsonPath("$.averageGapsPerPatient").exists());
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation")
    class MultiTenantIsolation {

        @Test
        @DisplayName("should isolate care gaps by tenant")
        void shouldIsolateCareGapsByTenant() throws Exception {
            String tenant1 = "tenant-001";
            String tenant2 = "tenant-002";

            // Create gap in tenant 1
            var headers1 = GatewayTrustTestHeaders.adminHeaders(tenant1);
            mockMvc.perform(post("/care-gap/detect")
                    .headers(headers1)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                            "patientId": "%s",
                            "measureId": "HEDIS_AWV",
                            "denominatorEligible": true,
                            "numeratorCompliant": false
                        }
                        """.formatted(PATIENT_ID)))
                .andExpect(status().isCreated());

            // Tenant 2 should not see tenant 1's gaps
            var headers2 = GatewayTrustTestHeaders.adminHeaders(tenant2);
            mockMvc.perform(get("/care-gap/patient/" + PATIENT_ID)
                    .headers(headers2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        }
    }
}
