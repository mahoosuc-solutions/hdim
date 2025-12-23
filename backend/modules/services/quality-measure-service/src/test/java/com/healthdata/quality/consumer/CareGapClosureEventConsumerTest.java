package com.healthdata.quality.consumer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.quality.dto.FhirResourceEvent;
import com.healthdata.quality.persistence.CareGapEntity;
import com.healthdata.quality.service.CareGapMatchingService;
import com.healthdata.quality.service.CareGapService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.kafka.core.KafkaTemplate;

@DisplayName("CareGapClosureEventConsumer Tests")
class CareGapClosureEventConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private static KafkaTemplate<String, String> mockKafkaTemplate() {
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate =
            (KafkaTemplate<String, String>) Mockito.mock(KafkaTemplate.class);
        return kafkaTemplate;
    }

    @Test
    @DisplayName("Should ignore events missing tenant or patient")
    void shouldIgnoreEventsMissingTenantOrPatient() throws Exception {
        CareGapService careGapService = Mockito.mock(CareGapService.class);
        CareGapMatchingService matchingService = Mockito.mock(CareGapMatchingService.class);
        KafkaTemplate<String, String> kafkaTemplate = mockKafkaTemplate();

        CareGapClosureEventConsumer consumer = new CareGapClosureEventConsumer(
            careGapService,
            matchingService,
            kafkaTemplate,
            objectMapper
        );

        FhirResourceEvent event = FhirResourceEvent.builder()
            .resourceType("Procedure")
            .resourceId("proc-1")
            .tenantId(null)
            .patientId(null)
            .build();

        String message = objectMapper.writeValueAsString(event);

        consumer.handleProcedureCreated(message);

        verify(matchingService, never()).findMatchingCareGaps(any(), any(), any());
        verify(careGapService, never()).autoCloseCareGap(any(), any(), any(), any(), any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should skip when no matching gaps found")
    void shouldSkipWhenNoMatchingGapsFound() throws Exception {
        CareGapService careGapService = Mockito.mock(CareGapService.class);
        CareGapMatchingService matchingService = Mockito.mock(CareGapMatchingService.class);
        KafkaTemplate<String, String> kafkaTemplate = mockKafkaTemplate();

        CareGapClosureEventConsumer consumer = new CareGapClosureEventConsumer(
            careGapService,
            matchingService,
            kafkaTemplate,
            objectMapper
        );

        UUID patientId = UUID.randomUUID();
        FhirResourceEvent event = FhirResourceEvent.builder()
            .resourceType("Observation")
            .resourceId("obs-1")
            .tenantId("tenant-1")
            .patientId(patientId)
            .build();
        String message = objectMapper.writeValueAsString(event);

        when(matchingService.findMatchingCareGaps(eq("tenant-1"), eq(patientId), any(FhirResourceEvent.class)))
            .thenReturn(List.of());

        consumer.handleObservationCreated(message);

        verify(careGapService, never()).autoCloseCareGap(any(), any(), any(), any(), any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should auto-close open gaps and publish closure event")
    void shouldAutoCloseOpenGapsAndPublishEvent() throws Exception {
        CareGapService careGapService = Mockito.mock(CareGapService.class);
        CareGapMatchingService matchingService = Mockito.mock(CareGapMatchingService.class);
        KafkaTemplate<String, String> kafkaTemplate = mockKafkaTemplate();

        CareGapClosureEventConsumer consumer = new CareGapClosureEventConsumer(
            careGapService,
            matchingService,
            kafkaTemplate,
            objectMapper
        );

        UUID patientId = UUID.randomUUID();
        CareGapEntity gap = CareGapEntity.builder()
            .id(UUID.randomUUID())
            .tenantId("tenant-1")
            .patientId(patientId)
            .gapType("A1C_TEST")
            .category(CareGapEntity.GapCategory.CHRONIC_DISEASE)
            .status(CareGapEntity.Status.OPEN)
            .build();

        FhirResourceEvent event = FhirResourceEvent.builder()
            .resourceType("Procedure")
            .resourceId("proc-2")
            .tenantId("tenant-1")
            .patientId(patientId)
            .build();
        String message = objectMapper.writeValueAsString(event);

        when(matchingService.findMatchingCareGaps(eq("tenant-1"), eq(patientId), any(FhirResourceEvent.class)))
            .thenReturn(List.of(gap));
        when(matchingService.getMatchingSummary(eq(gap), any(FhirResourceEvent.class)))
            .thenReturn("matched on A1C");

        consumer.handleProcedureCreated(message);

        verify(careGapService).autoCloseCareGap(
            eq("tenant-1"),
            eq(gap.getId()),
            eq("Procedure"),
            eq("proc-2"),
            eq("matched on A1C")
        );
        verify(kafkaTemplate).send(eq("care-gap.auto-closed"), any(), any());
    }

    @Test
    @DisplayName("Should skip closed gaps")
    void shouldSkipClosedGaps() throws Exception {
        CareGapService careGapService = Mockito.mock(CareGapService.class);
        CareGapMatchingService matchingService = Mockito.mock(CareGapMatchingService.class);
        KafkaTemplate<String, String> kafkaTemplate = mockKafkaTemplate();

        CareGapClosureEventConsumer consumer = new CareGapClosureEventConsumer(
            careGapService,
            matchingService,
            kafkaTemplate,
            objectMapper
        );

        UUID patientId = UUID.randomUUID();
        CareGapEntity gap = CareGapEntity.builder()
            .id(UUID.randomUUID())
            .tenantId("tenant-1")
            .patientId(patientId)
            .gapType("SCREENING")
            .category(CareGapEntity.GapCategory.SCREENING)
            .status(CareGapEntity.Status.CLOSED)
            .build();

        FhirResourceEvent event = FhirResourceEvent.builder()
            .resourceType("Observation")
            .resourceId("obs-2")
            .tenantId("tenant-1")
            .patientId(patientId)
            .build();
        String message = objectMapper.writeValueAsString(event);

        when(matchingService.findMatchingCareGaps(eq("tenant-1"), eq(patientId), any(FhirResourceEvent.class)))
            .thenReturn(List.of(gap));

        consumer.handleObservationCreated(message);

        verify(careGapService, never()).autoCloseCareGap(any(), any(), any(), any(), any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should swallow exceptions when publishing closure event")
    void shouldSwallowExceptionsWhenPublishingClosureEvent() throws Exception {
        CareGapService careGapService = Mockito.mock(CareGapService.class);
        CareGapMatchingService matchingService = Mockito.mock(CareGapMatchingService.class);
        KafkaTemplate<String, String> kafkaTemplate = mockKafkaTemplate();

        CareGapClosureEventConsumer consumer = new CareGapClosureEventConsumer(
            careGapService,
            matchingService,
            kafkaTemplate,
            objectMapper
        );

        UUID patientId = UUID.randomUUID();
        CareGapEntity gap = CareGapEntity.builder()
            .id(UUID.randomUUID())
            .tenantId("tenant-1")
            .patientId(patientId)
            .gapType("SCREENING")
            .category(CareGapEntity.GapCategory.SCREENING)
            .status(CareGapEntity.Status.OPEN)
            .build();

        FhirResourceEvent event = FhirResourceEvent.builder()
            .resourceType("Procedure")
            .resourceId("proc-3")
            .tenantId("tenant-1")
            .patientId(patientId)
            .build();
        String message = objectMapper.writeValueAsString(event);

        when(matchingService.findMatchingCareGaps(eq("tenant-1"), eq(patientId), any(FhirResourceEvent.class)))
            .thenReturn(List.of(gap));
        when(matchingService.getMatchingSummary(eq(gap), any(FhirResourceEvent.class)))
            .thenReturn("summary");
        Mockito.doThrow(new RuntimeException("kafka down"))
            .when(kafkaTemplate).send(eq("care-gap.auto-closed"), any(), any());

        assertThatCode(() -> consumer.handleProcedureCreated(message)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should ignore invalid JSON payloads")
    void shouldIgnoreInvalidJsonPayloads() {
        CareGapService careGapService = Mockito.mock(CareGapService.class);
        CareGapMatchingService matchingService = Mockito.mock(CareGapMatchingService.class);
        KafkaTemplate<String, String> kafkaTemplate = mockKafkaTemplate();

        CareGapClosureEventConsumer consumer = new CareGapClosureEventConsumer(
            careGapService,
            matchingService,
            kafkaTemplate,
            objectMapper
        );

        assertThatCode(() -> consumer.handleObservationCreated("not-json")).doesNotThrowAnyException();

        verify(matchingService, never()).findMatchingCareGaps(any(), any(), any());
        verify(careGapService, never()).autoCloseCareGap(any(), any(), any(), any(), any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should skip dismissed gaps")
    void shouldSkipDismissedGaps() throws Exception {
        CareGapService careGapService = Mockito.mock(CareGapService.class);
        CareGapMatchingService matchingService = Mockito.mock(CareGapMatchingService.class);
        KafkaTemplate<String, String> kafkaTemplate = mockKafkaTemplate();

        CareGapClosureEventConsumer consumer = new CareGapClosureEventConsumer(
            careGapService,
            matchingService,
            kafkaTemplate,
            objectMapper
        );

        UUID patientId = UUID.randomUUID();
        CareGapEntity gap = CareGapEntity.builder()
            .id(UUID.randomUUID())
            .tenantId("tenant-1")
            .patientId(patientId)
            .gapType("SCREENING")
            .category(CareGapEntity.GapCategory.SCREENING)
            .status(CareGapEntity.Status.DISMISSED)
            .build();

        FhirResourceEvent event = FhirResourceEvent.builder()
            .resourceType("Observation")
            .resourceId("obs-3")
            .tenantId("tenant-1")
            .patientId(patientId)
            .build();
        String message = objectMapper.writeValueAsString(event);

        when(matchingService.findMatchingCareGaps(eq("tenant-1"), eq(patientId), any(FhirResourceEvent.class)))
            .thenReturn(List.of(gap));

        consumer.handleObservationCreated(message);

        verify(careGapService, never()).autoCloseCareGap(any(), any(), any(), any(), any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    @DisplayName("Should swallow exceptions when auto-close fails")
    void shouldSwallowExceptionsWhenAutoCloseFails() throws Exception {
        CareGapService careGapService = Mockito.mock(CareGapService.class);
        CareGapMatchingService matchingService = Mockito.mock(CareGapMatchingService.class);
        KafkaTemplate<String, String> kafkaTemplate = mockKafkaTemplate();

        CareGapClosureEventConsumer consumer = new CareGapClosureEventConsumer(
            careGapService,
            matchingService,
            kafkaTemplate,
            objectMapper
        );

        UUID patientId = UUID.randomUUID();
        CareGapEntity gap = CareGapEntity.builder()
            .id(UUID.randomUUID())
            .tenantId("tenant-1")
            .patientId(patientId)
            .gapType("SCREENING")
            .category(CareGapEntity.GapCategory.SCREENING)
            .status(CareGapEntity.Status.OPEN)
            .build();

        FhirResourceEvent event = FhirResourceEvent.builder()
            .resourceType("Procedure")
            .resourceId("proc-4")
            .tenantId("tenant-1")
            .patientId(patientId)
            .build();
        String message = objectMapper.writeValueAsString(event);

        when(matchingService.findMatchingCareGaps(eq("tenant-1"), eq(patientId), any(FhirResourceEvent.class)))
            .thenReturn(List.of(gap));
        when(matchingService.getMatchingSummary(eq(gap), any(FhirResourceEvent.class)))
            .thenReturn("summary");
        doThrow(new RuntimeException("auto-close failed"))
            .when(careGapService)
            .autoCloseCareGap(eq("tenant-1"), eq(gap.getId()), eq("Procedure"), eq("proc-4"), eq("summary"));

        assertThatCode(() -> consumer.handleProcedureCreated(message)).doesNotThrowAnyException();

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}
