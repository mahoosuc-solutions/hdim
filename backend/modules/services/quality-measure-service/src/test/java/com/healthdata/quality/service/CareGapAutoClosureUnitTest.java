package com.healthdata.quality.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.quality.consumer.CareGapClosureEventConsumer;
import com.healthdata.quality.dto.FhirResourceEvent;
import com.healthdata.quality.persistence.CareGapEntity;
import com.healthdata.quality.persistence.CareGapRepository;
import com.healthdata.quality.service.notification.CareGapNotificationTrigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit Test Suite for Automated Care Gap Closure
 * Phase 2.1: TDD-based unit tests without requiring full Spring context
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Care Gap Auto-Closure Unit Tests")
class CareGapAutoClosureUnitTest {

    @Mock
    private CareGapRepository careGapRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private CareGapNotificationTrigger notificationTrigger;

    @InjectMocks
    private CareGapService careGapService;

    @InjectMocks
    private CareGapMatchingService matchingService;

    @InjectMocks
    private CareGapClosureEventConsumer eventConsumer;

    @Mock
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void setUp() {
        matchingService = new CareGapMatchingService(careGapRepository);
        careGapService = new CareGapService(careGapRepository, notificationTrigger);
        eventConsumer = new CareGapClosureEventConsumer(
            careGapService,
            matchingService,
            kafkaTemplate,
            objectMapper
        );
    }

    @Test
    @DisplayName("Test 1: Auto-close care gap method should update status and evidence")
    void testAutoCloseCareGap_UpdatesStatusAndEvidence() {
        // ARRANGE
        UUID gapId = UUID.randomUUID();
        CareGapEntity careGap = createCareGap(
            gapId,
            "screening-test",
            CareGapEntity.GapCategory.SCREENING,
            "Test Due",
            "99999"
        );

        when(careGapRepository.findById(gapId)).thenReturn(Optional.of(careGap));
        when(careGapRepository.save(any(CareGapEntity.class))).thenAnswer(i -> i.getArgument(0));

        // ACT
        careGapService.autoCloseCareGap(
            TENANT_ID,
            gapId,
            "Procedure",
            "Procedure/123",
            "99999"
        );

        // ASSERT
        ArgumentCaptor<CareGapEntity> captor = ArgumentCaptor.forClass(CareGapEntity.class);
        verify(careGapRepository).save(captor.capture());

        CareGapEntity saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(CareGapEntity.Status.CLOSED);
        assertThat(saved.getAutoClosed()).isTrue();
        assertThat(saved.getEvidenceResourceType()).isEqualTo("Procedure");
        assertThat(saved.getEvidenceResourceId()).isEqualTo("Procedure/123");
        assertThat(saved.getClosedAt()).isNotNull();
        assertThat(saved.getClosedBy()).isEqualTo("SYSTEM");
        assertThat(saved.getEvidence()).contains("Auto-closed by matching FHIR resource");
    }

    @Test
    @DisplayName("Test 2: Should not close already closed care gaps")
    void testAutoCloseCareGap_SkipsAlreadyClosedGaps() {
        // ARRANGE
        UUID gapId = UUID.randomUUID();
        CareGapEntity careGap = createCareGap(
            gapId,
            "screening-test",
            CareGapEntity.GapCategory.SCREENING,
            "Test Due",
            "99999"
        );
        careGap.setStatus(CareGapEntity.Status.CLOSED);

        when(careGapRepository.findById(gapId)).thenReturn(Optional.of(careGap));

        // ACT
        careGapService.autoCloseCareGap(
            TENANT_ID,
            gapId,
            "Procedure",
            "Procedure/123",
            "99999"
        );

        // ASSERT
        verify(careGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 3: Care gap matching should find gaps by code")
    void testFindMatchingCareGaps_ByCode() {
        // ARRANGE
        CareGapEntity gap1 = createCareGap(
            UUID.randomUUID(),
            "screening-test",
            CareGapEntity.GapCategory.SCREENING,
            "Screening Due",
            "45378,45380"
        );

        CareGapEntity gap2 = createCareGap(
            UUID.randomUUID(),
            "lab-test",
            CareGapEntity.GapCategory.SCREENING,
            "Lab Test Due",
            "82947"
        );

        when(careGapRepository.findOpenCareGaps(TENANT_ID, PATIENT_ID))
            .thenReturn(List.of(gap1, gap2));

        FhirResourceEvent event = createProcedureEvent(
            "Procedure/123",
            "45378",
            "http://www.ama-assn.org/go/cpt",
            "Colonoscopy"
        );

        // ACT
        List<CareGapEntity> matches = matchingService.findMatchingCareGaps(
            TENANT_ID,
            PATIENT_ID,
            event
        );

        // ASSERT
        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).getGapType()).isEqualTo("screening-test");
    }

    @Test
    @DisplayName("Test 4: Should handle multiple matching codes")
    void testFindMatchingCareGaps_MultipleMatchingCodes() {
        // ARRANGE
        CareGapEntity gap = createCareGap(
            UUID.randomUUID(),
            "screening-test",
            CareGapEntity.GapCategory.SCREENING,
            "Screening Due",
            "45378,45380,45385"
        );

        when(careGapRepository.findOpenCareGaps(TENANT_ID, PATIENT_ID))
            .thenReturn(List.of(gap));

        FhirResourceEvent event = createProcedureEvent(
            "Procedure/123",
            "45380",
            "http://www.ama-assn.org/go/cpt",
            "Colonoscopy with biopsy"
        );

        // ACT
        List<CareGapEntity> matches = matchingService.findMatchingCareGaps(
            TENANT_ID,
            PATIENT_ID,
            event
        );

        // ASSERT
        assertThat(matches).hasSize(1);
    }

    @Test
    @DisplayName("Test 5: Should return empty list when no codes match")
    void testFindMatchingCareGaps_NoMatches() {
        // ARRANGE
        CareGapEntity gap = createCareGap(
            UUID.randomUUID(),
            "screening-test",
            CareGapEntity.GapCategory.SCREENING,
            "Screening Due",
            "45378"
        );

        when(careGapRepository.findOpenCareGaps(TENANT_ID, PATIENT_ID))
            .thenReturn(List.of(gap));

        FhirResourceEvent event = createProcedureEvent(
            "Procedure/123",
            "99999",
            "http://www.ama-assn.org/go/cpt",
            "Unrelated Procedure"
        );

        // ACT
        List<CareGapEntity> matches = matchingService.findMatchingCareGaps(
            TENANT_ID,
            PATIENT_ID,
            event
        );

        // ASSERT
        assertThat(matches).isEmpty();
    }

    @Test
    @DisplayName("Event consumer should skip when tenant or patient missing")
    void shouldSkipEventWhenTenantOrPatientMissing() throws Exception {
        CareGapService careGapServiceMock = mock(CareGapService.class);
        CareGapMatchingService matchingServiceMock = mock(CareGapMatchingService.class);
        KafkaTemplate<String, String> kafkaTemplateMock = mock(KafkaTemplate.class);
        ObjectMapper mapper = mock(ObjectMapper.class);

        CareGapClosureEventConsumer consumer = new CareGapClosureEventConsumer(
            careGapServiceMock,
            matchingServiceMock,
            kafkaTemplateMock,
            mapper
        );

        FhirResourceEvent event = FhirResourceEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("fhir.procedures.created")
            .resourceType("Procedure")
            .resourceId("Procedure/1")
            .tenantId(null)
            .patientId(null)
            .build();
        when(mapper.readValue(any(String.class), eq(FhirResourceEvent.class))).thenReturn(event);

        consumer.handleProcedureCreated("{ }");

        verifyNoInteractions(matchingServiceMock);
        verifyNoInteractions(careGapServiceMock);
    }

    @Test
    @DisplayName("Event consumer should skip when no matching gaps")
    void shouldSkipWhenNoMatchingGaps() throws Exception {
        CareGapService careGapServiceMock = mock(CareGapService.class);
        CareGapMatchingService matchingServiceMock = mock(CareGapMatchingService.class);
        KafkaTemplate<String, String> kafkaTemplateMock = mock(KafkaTemplate.class);
        ObjectMapper mapper = mock(ObjectMapper.class);

        CareGapClosureEventConsumer consumer = new CareGapClosureEventConsumer(
            careGapServiceMock,
            matchingServiceMock,
            kafkaTemplateMock,
            mapper
        );

        FhirResourceEvent event = createProcedureEvent(
            "Procedure/2",
            "45378",
            "http://www.ama-assn.org/go/cpt",
            "Colonoscopy"
        );
        when(mapper.readValue(any(String.class), eq(FhirResourceEvent.class))).thenReturn(event);
        when(matchingServiceMock.findMatchingCareGaps(
            TENANT_ID, PATIENT_ID, event)).thenReturn(List.of());

        consumer.handleProcedureCreated("{ }");

        verify(careGapServiceMock, never()).autoCloseCareGap(any(), any(), any(), any(), any());
        verify(kafkaTemplateMock, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Event consumer should swallow publish errors")
    void shouldSwallowPublishErrors() throws Exception {
        CareGapService careGapServiceMock = mock(CareGapService.class);
        CareGapMatchingService matchingServiceMock = mock(CareGapMatchingService.class);
        KafkaTemplate<String, String> kafkaTemplateMock = mock(KafkaTemplate.class);
        ObjectMapper mapper = mock(ObjectMapper.class);

        CareGapClosureEventConsumer consumer = new CareGapClosureEventConsumer(
            careGapServiceMock,
            matchingServiceMock,
            kafkaTemplateMock,
            mapper
        );

        FhirResourceEvent event = createProcedureEvent(
            "Procedure/3",
            "45380",
            "http://www.ama-assn.org/go/cpt",
            "Colonoscopy"
        );
        CareGapEntity gap = createCareGap(
            UUID.randomUUID(),
            "screening-test",
            CareGapEntity.GapCategory.SCREENING,
            "Screening Due",
            "45380"
        );

        when(mapper.readValue(any(String.class), eq(FhirResourceEvent.class))).thenReturn(event);
        when(matchingServiceMock.findMatchingCareGaps(
            TENANT_ID, PATIENT_ID, event)).thenReturn(List.of(gap));
        when(matchingServiceMock.getMatchingSummary(eq(gap), eq(event))).thenReturn("match");
        when(mapper.writeValueAsString(any())).thenThrow(new RuntimeException("json fail"));

        consumer.handleProcedureCreated("{ }");

        verify(careGapServiceMock).autoCloseCareGap(
            eq(TENANT_ID),
            eq(gap.getId()),
            eq(event.getResourceType()),
            eq(event.getResourceId()),
            eq("match")
        );
    }

    @Test
    @DisplayName("Event consumer should handle observation created events")
    void shouldHandleObservationCreatedEvents() throws Exception {
        CareGapService careGapServiceMock = mock(CareGapService.class);
        CareGapMatchingService matchingServiceMock = mock(CareGapMatchingService.class);
        KafkaTemplate<String, String> kafkaTemplateMock = mock(KafkaTemplate.class);
        ObjectMapper mapper = mock(ObjectMapper.class);

        CareGapClosureEventConsumer consumer = new CareGapClosureEventConsumer(
            careGapServiceMock,
            matchingServiceMock,
            kafkaTemplateMock,
            mapper
        );

        FhirResourceEvent event = FhirResourceEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType("fhir.observations.created")
            .resourceType("Observation")
            .resourceId("Observation/1")
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .build();
        CareGapEntity gap = createCareGap(
            UUID.randomUUID(),
            "lab-test",
            CareGapEntity.GapCategory.SCREENING,
            "Lab Test Due",
            "82947"
        );

        when(mapper.readValue(any(String.class), eq(FhirResourceEvent.class))).thenReturn(event);
        when(matchingServiceMock.findMatchingCareGaps(
            TENANT_ID, PATIENT_ID, event)).thenReturn(List.of(gap));
        when(matchingServiceMock.getMatchingSummary(eq(gap), eq(event))).thenReturn("match");
        when(mapper.writeValueAsString(any())).thenReturn("{\"event\":\"ok\"}");

        consumer.handleObservationCreated("{ }");

        verify(careGapServiceMock).autoCloseCareGap(
            eq(TENANT_ID),
            eq(gap.getId()),
            eq("Observation"),
            eq("Observation/1"),
            eq("match")
        );
        verify(kafkaTemplateMock).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Test 6: Should handle gaps with no matching codes defined")
    void testFindMatchingCareGaps_NoMatchingCodesDefined() {
        // ARRANGE
        CareGapEntity gap = createCareGap(
            UUID.randomUUID(),
            "manual-gap",
            CareGapEntity.GapCategory.SCREENING,
            "Manual Gap",
            null
        );

        when(careGapRepository.findOpenCareGaps(TENANT_ID, PATIENT_ID))
            .thenReturn(List.of(gap));

        FhirResourceEvent event = createProcedureEvent(
            "Procedure/123",
            "45378",
            "http://www.ama-assn.org/go/cpt",
            "Colonoscopy"
        );

        // ACT
        List<CareGapEntity> matches = matchingService.findMatchingCareGaps(
            TENANT_ID,
            PATIENT_ID,
            event
        );

        // ASSERT
        assertThat(matches).isEmpty();
    }

    @Test
    @DisplayName("Test 7: Should verify tenant ownership before auto-closing")
    void testAutoCloseCareGap_VerifiesTenantOwnership() {
        // ARRANGE
        UUID gapId = UUID.randomUUID();
        CareGapEntity careGap = createCareGap(
            gapId,
            "screening-test",
            CareGapEntity.GapCategory.SCREENING,
            "Test Due",
            "99999"
        );
        careGap.setTenantId("other-tenant");

        when(careGapRepository.findById(gapId)).thenReturn(Optional.of(careGap));

        // ACT & ASSERT
        try {
            careGapService.autoCloseCareGap(
                TENANT_ID,
                gapId,
                "Procedure",
                "Procedure/123",
                "99999"
            );
            // Should throw exception
            assertThat(false).as("Should have thrown exception for tenant mismatch").isTrue();
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("does not belong to tenant");
        }

        verify(careGapRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test 8: Matching summary should return intersection of codes")
    void testGetMatchingSummary() {
        // ARRANGE
        CareGapEntity gap = createCareGap(
            UUID.randomUUID(),
            "screening-test",
            CareGapEntity.GapCategory.SCREENING,
            "Screening Due",
            "45378,45380,45385"
        );

        FhirResourceEvent event = createProcedureEvent(
            "Procedure/123",
            "45380",
            "http://www.ama-assn.org/go/cpt",
            "Colonoscopy with biopsy"
        );

        // ACT
        String summary = matchingService.getMatchingSummary(gap, event);

        // ASSERT
        assertThat(summary).contains("45380");
    }

    // Helper methods

    private CareGapEntity createCareGap(
        UUID id,
        String gapType,
        CareGapEntity.GapCategory category,
        String title,
        String matchingCodes
    ) {
        CareGapEntity entity = CareGapEntity.builder()
            .id(id)
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

        // Manually set ID since @Builder doesn't work with @Id
        try {
            var idField = CareGapEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(entity, id);
        } catch (Exception e) {
            // Ignore - ID already set by builder
        }

        return entity;
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
}
