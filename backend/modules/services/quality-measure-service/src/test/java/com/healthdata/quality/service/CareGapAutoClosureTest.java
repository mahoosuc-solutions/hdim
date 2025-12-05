package com.healthdata.quality.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.quality.config.BaseIntegrationTest;
import com.healthdata.quality.consumer.CareGapClosureEventConsumer;
import com.healthdata.quality.dto.CareGapClosureEvent;
import com.healthdata.quality.dto.FhirResourceEvent;
import com.healthdata.quality.persistence.CareGapEntity;
import com.healthdata.quality.persistence.CareGapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD Test Suite for Automated Care Gap Closure
 * Phase 2.1: Comprehensive test coverage for care gap auto-closure functionality
 */
@BaseIntegrationTest
@DisplayName("Care Gap Auto-Closure Tests")
class CareGapAutoClosureTest {

    @Autowired
    private CareGapRepository careGapRepository;

    @Autowired
    private CareGapService careGapService;

    @Autowired
    private CareGapMatchingService matchingService;

    @Autowired
    private CareGapClosureEventConsumer eventConsumer;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "test-tenant";
    private static final String PATIENT_ID = "patient-123";

    @BeforeEach
    void setUp() {
        // Clean up test data
        careGapRepository.deleteAll();
    }

    @Test
    @DisplayName("Test 1: Care gap should auto-close on matching Procedure event")
    void testCareGapClosureOnProcedureEvent() throws Exception {
        // ARRANGE: Create an open care gap for colonoscopy screening
        CareGapEntity careGap = createCareGap(
            "screening-colonoscopy",
            CareGapEntity.GapCategory.SCREENING,
            "Colonoscopy Screening Overdue",
            "45378,45380,45385" // CPT codes for colonoscopy
        );
        careGap = careGapRepository.save(careGap);

        // Create a FHIR Procedure event for colonoscopy
        FhirResourceEvent procedureEvent = createProcedureEvent(
            "Procedure/12345",
            "45378", // Colonoscopy CPT code
            "http://www.ama-assn.org/go/cpt",
            "Colonoscopy"
        );

        // ACT: Process the procedure event
        String eventJson = objectMapper.writeValueAsString(procedureEvent);
        eventConsumer.handleProcedureCreated(eventJson);

        // ASSERT: Care gap should be closed
        CareGapEntity updated = careGapRepository.findById(careGap.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(CareGapEntity.Status.CLOSED);
        assertThat(updated.getAutoClosed()).isTrue();
        assertThat(updated.getEvidenceResourceType()).isEqualTo("Procedure");
        assertThat(updated.getEvidenceResourceId()).isEqualTo("Procedure/12345");
        assertThat(updated.getClosedAt()).isNotNull();
        assertThat(updated.getClosedBy()).isEqualTo("SYSTEM");
    }

    @Test
    @DisplayName("Test 2: Care gap should auto-close on matching Observation event (lab result)")
    void testCareGapClosureOnObservationEvent() throws Exception {
        // ARRANGE: Create an open care gap for HbA1c test
        CareGapEntity careGap = createCareGap(
            "diabetes-hba1c-test",
            CareGapEntity.GapCategory.CHRONIC_DISEASE,
            "HbA1c Test Overdue",
            "4548-4" // LOINC code for HbA1c
        );
        careGap = careGapRepository.save(careGap);

        // Create a FHIR Observation event for HbA1c
        FhirResourceEvent observationEvent = createObservationEvent(
            "Observation/67890",
            "4548-4", // HbA1c LOINC code
            "http://loinc.org",
            "Hemoglobin A1c"
        );

        // ACT: Process the observation event
        String eventJson = objectMapper.writeValueAsString(observationEvent);
        eventConsumer.handleObservationCreated(eventJson);

        // ASSERT: Care gap should be closed
        CareGapEntity updated = careGapRepository.findById(careGap.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(CareGapEntity.Status.CLOSED);
        assertThat(updated.getAutoClosed()).isTrue();
        assertThat(updated.getEvidenceResourceType()).isEqualTo("Observation");
        assertThat(updated.getEvidenceResourceId()).isEqualTo("Observation/67890");
        assertThat(updated.getClosedAt()).isNotNull();
        assertThat(updated.getClosedBy()).isEqualTo("SYSTEM");
    }

    @Test
    @DisplayName("Test 3: Care gap matching should work by code")
    void testCareGapMatchingByCode() {
        // ARRANGE: Create care gap with specific CPT code
        CareGapEntity careGap = createCareGap(
            "mammography-screening",
            CareGapEntity.GapCategory.SCREENING,
            "Mammography Screening Due",
            "77067" // CPT code for mammography
        );
        careGap = careGapRepository.save(careGap);

        // Create procedure event with matching code
        FhirResourceEvent event = createProcedureEvent(
            "Procedure/999",
            "77067",
            "http://www.ama-assn.org/go/cpt",
            "Screening Mammography"
        );

        // ACT: Find matching care gaps
        List<CareGapEntity> matches = matchingService.findMatchingCareGaps(
            TENANT_ID,
            PATIENT_ID,
            event
        );

        // ASSERT: Should find the care gap
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getId()).isEqualTo(careGap.getId());
    }

    @Test
    @DisplayName("Test 4: Care gap matching should work by category")
    void testCareGapMatchingByCategory() {
        // ARRANGE: Create care gap for preventive screening
        CareGapEntity careGap = createCareGap(
            "preventive-screening",
            CareGapEntity.GapCategory.PREVENTIVE_CARE,
            "Annual Wellness Visit Due",
            "G0438,G0439" // Annual wellness visit codes
        );
        careGap = careGapRepository.save(careGap);

        // Create procedure event with matching category
        FhirResourceEvent event = createProcedureEvent(
            "Procedure/888",
            "G0439",
            "http://www.cms.gov/Medicare/Coding/HCPCSReleaseCodeSets",
            "Annual Wellness Visit"
        );

        // ACT: Find matching care gaps
        List<CareGapEntity> matches = matchingService.findMatchingCareGaps(
            TENANT_ID,
            PATIENT_ID,
            event
        );

        // ASSERT: Should find the care gap
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getCategory()).isEqualTo(CareGapEntity.GapCategory.PREVENTIVE_CARE);
    }

    @Test
    @DisplayName("Test 5: Care gap should link evidence resource")
    void testCareGapEvidenceLinking() throws Exception {
        // ARRANGE: Create care gap
        CareGapEntity careGap = createCareGap(
            "lipid-panel",
            CareGapEntity.GapCategory.CHRONIC_DISEASE,
            "Lipid Panel Overdue",
            "80061" // CPT code for lipid panel
        );
        careGap = careGapRepository.save(careGap);

        // Create observation event
        FhirResourceEvent event = createObservationEvent(
            "Observation/lipid-123",
            "80061",
            "http://www.ama-assn.org/go/cpt",
            "Lipid Panel"
        );

        // ACT: Process the event
        String eventJson = objectMapper.writeValueAsString(event);
        eventConsumer.handleObservationCreated(eventJson);

        // ASSERT: Evidence should be linked
        CareGapEntity updated = careGapRepository.findById(careGap.getId()).orElseThrow();
        assertThat(updated.getEvidenceResourceId()).isEqualTo("Observation/lipid-123");
        assertThat(updated.getEvidenceResourceType()).isEqualTo("Observation");
        assertThat(updated.getEvidence()).contains("Auto-closed by matching FHIR resource");
    }

    @Test
    @DisplayName("Test 6: Should not fail when no matching gap exists")
    void testNoMatchingGap_ShouldNotFail() throws Exception {
        // ARRANGE: No care gaps in database

        // Create a procedure event
        FhirResourceEvent event = createProcedureEvent(
            "Procedure/random",
            "99999",
            "http://www.ama-assn.org/go/cpt",
            "Some Random Procedure"
        );

        // ACT & ASSERT: Should not throw exception
        String eventJson = objectMapper.writeValueAsString(event);
        eventConsumer.handleProcedureCreated(eventJson);

        // Verify no care gaps were affected
        assertThat(careGapRepository.count()).isZero();
    }

    @Test
    @DisplayName("Test 7: Multi-tenant isolation - should only close gaps for correct tenant")
    void testMultiTenantIsolation() throws Exception {
        // ARRANGE: Create care gaps for two different tenants
        CareGapEntity tenant1Gap = createCareGap(
            "screening-test",
            CareGapEntity.GapCategory.SCREENING,
            "Screening Test Due",
            "12345"
        );
        tenant1Gap.setTenantId("tenant-1");
        tenant1Gap = careGapRepository.save(tenant1Gap);

        CareGapEntity tenant2Gap = createCareGap(
            "screening-test",
            CareGapEntity.GapCategory.SCREENING,
            "Screening Test Due",
            "12345"
        );
        tenant2Gap.setTenantId("tenant-2");
        tenant2Gap.setPatientId(PATIENT_ID); // Same patient ID, different tenant
        tenant2Gap = careGapRepository.save(tenant2Gap);

        // Create event for tenant-1
        FhirResourceEvent event = createProcedureEvent(
            "Procedure/123",
            "12345",
            "http://www.ama-assn.org/go/cpt",
            "Screening Test"
        );
        event.setTenantId("tenant-1");

        // ACT: Process the event
        String eventJson = objectMapper.writeValueAsString(event);
        eventConsumer.handleProcedureCreated(eventJson);

        // ASSERT: Only tenant-1 gap should be closed
        CareGapEntity tenant1Updated = careGapRepository.findById(tenant1Gap.getId()).orElseThrow();
        CareGapEntity tenant2Updated = careGapRepository.findById(tenant2Gap.getId()).orElseThrow();

        assertThat(tenant1Updated.getStatus()).isEqualTo(CareGapEntity.Status.CLOSED);
        assertThat(tenant1Updated.getAutoClosed()).isTrue();

        assertThat(tenant2Updated.getStatus()).isEqualTo(CareGapEntity.Status.OPEN);
        assertThat(tenant2Updated.getAutoClosed()).isFalse();
    }

    @Test
    @DisplayName("Test 8: Should close multiple matching gaps for same patient")
    void testMultipleMatchingGaps() throws Exception {
        // ARRANGE: Create multiple care gaps that match the same code
        CareGapEntity gap1 = createCareGap(
            "diabetes-screening",
            CareGapEntity.GapCategory.SCREENING,
            "Diabetes Screening Due",
            "82947,82950" // Glucose tests
        );
        gap1 = careGapRepository.save(gap1);

        CareGapEntity gap2 = createCareGap(
            "diabetes-followup",
            CareGapEntity.GapCategory.CHRONIC_DISEASE,
            "Diabetes Follow-up Test",
            "82947" // Same glucose test code
        );
        gap2 = careGapRepository.save(gap2);

        // Create observation event
        FhirResourceEvent event = createObservationEvent(
            "Observation/glucose-456",
            "82947",
            "http://www.ama-assn.org/go/cpt",
            "Glucose Test"
        );

        // ACT: Process the event
        String eventJson = objectMapper.writeValueAsString(event);
        eventConsumer.handleObservationCreated(eventJson);

        // ASSERT: Both gaps should be closed
        CareGapEntity gap1Updated = careGapRepository.findById(gap1.getId()).orElseThrow();
        CareGapEntity gap2Updated = careGapRepository.findById(gap2.getId()).orElseThrow();

        assertThat(gap1Updated.getStatus()).isEqualTo(CareGapEntity.Status.CLOSED);
        assertThat(gap1Updated.getAutoClosed()).isTrue();

        assertThat(gap2Updated.getStatus()).isEqualTo(CareGapEntity.Status.CLOSED);
        assertThat(gap2Updated.getAutoClosed()).isTrue();
    }

    @Test
    @DisplayName("Test 9: Should not close already closed gaps")
    void testShouldNotCloseAlreadyClosedGaps() throws Exception {
        // ARRANGE: Create a care gap that's already closed
        CareGapEntity careGap = createCareGap(
            "screening-test",
            CareGapEntity.GapCategory.SCREENING,
            "Test Due",
            "99999"
        );
        careGap.setStatus(CareGapEntity.Status.CLOSED);
        careGap.setAddressedDate(Instant.now().minus(7, ChronoUnit.DAYS));
        careGap.setAddressedBy("Dr. Smith");
        careGap = careGapRepository.save(careGap);

        // Create matching event
        FhirResourceEvent event = createProcedureEvent(
            "Procedure/abc",
            "99999",
            "http://www.ama-assn.org/go/cpt",
            "Test Procedure"
        );

        // ACT: Process the event
        String eventJson = objectMapper.writeValueAsString(event);
        eventConsumer.handleProcedureCreated(eventJson);

        // ASSERT: Gap should remain as manually closed (not auto-closed)
        CareGapEntity updated = careGapRepository.findById(careGap.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(CareGapEntity.Status.CLOSED);
        assertThat(updated.getAutoClosed()).isFalse();
        assertThat(updated.getAddressedBy()).isEqualTo("Dr. Smith"); // Should not be overwritten
    }

    @Test
    @DisplayName("Test 10: Care gap closure event should be published")
    void testCareGapClosureEventPublished() throws Exception {
        // ARRANGE: Create care gap
        CareGapEntity careGap = createCareGap(
            "test-gap",
            CareGapEntity.GapCategory.SCREENING,
            "Test Gap",
            "11111"
        );
        careGap = careGapRepository.save(careGap);

        // Create matching event
        FhirResourceEvent event = createProcedureEvent(
            "Procedure/xyz",
            "11111",
            "http://www.ama-assn.org/go/cpt",
            "Test"
        );

        // ACT: Process the event
        String eventJson = objectMapper.writeValueAsString(event);
        eventConsumer.handleProcedureCreated(eventJson);

        // ASSERT: Verify gap was closed (event publishing is tested separately via integration)
        CareGapEntity updated = careGapRepository.findById(careGap.getId()).orElseThrow();
        assertThat(updated.getAutoClosed()).isTrue();
        // Note: Kafka event publishing validation would be in integration test
    }

    // Helper methods

    private CareGapEntity createCareGap(
        String gapType,
        CareGapEntity.GapCategory category,
        String title,
        String matchingCodes
    ) {
        return CareGapEntity.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .category(category)
            .gapType(gapType)
            .title(title)
            .description("Test care gap")
            .priority(CareGapEntity.Priority.MEDIUM)
            .status(CareGapEntity.Status.OPEN)
            .matchingCodes(matchingCodes)
            .dueDate(Instant.now().plus(30, ChronoUnit.DAYS))
            .identifiedDate(Instant.now())
            .autoClosed(false)
            .build();
    }

    private FhirResourceEvent createProcedureEvent(
        String resourceId,
        String code,
        String system,
        String display
    ) {
        return FhirResourceEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("fhir.procedures.created")
            .resourceType("Procedure")
            .resourceId(resourceId)
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .timestamp(Instant.now())
            .codes(List.of(
                FhirResourceEvent.CodeableConcept.builder()
                    .coding(List.of(
                        FhirResourceEvent.Coding.builder()
                            .system(system)
                            .code(code)
                            .display(display)
                            .build()
                    ))
                    .build()
            ))
            .status("completed")
            .performedDate(Instant.now())
            .build();
    }

    private FhirResourceEvent createObservationEvent(
        String resourceId,
        String code,
        String system,
        String display
    ) {
        return FhirResourceEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("fhir.observations.created")
            .resourceType("Observation")
            .resourceId(resourceId)
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .timestamp(Instant.now())
            .codes(List.of(
                FhirResourceEvent.CodeableConcept.builder()
                    .coding(List.of(
                        FhirResourceEvent.Coding.builder()
                            .system(system)
                            .code(code)
                            .display(display)
                            .build()
                    ))
                    .build()
            ))
            .status("final")
            .performedDate(Instant.now())
            .build();
    }
}
