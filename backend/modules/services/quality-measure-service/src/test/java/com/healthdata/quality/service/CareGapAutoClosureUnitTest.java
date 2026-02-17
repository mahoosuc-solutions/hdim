package com.healthdata.quality.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.quality.consumer.CareGapClosureEventConsumer;
import com.healthdata.quality.dto.FhirResourceEvent;
import com.healthdata.quality.persistence.CareGapEntity;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class CareGapAutoClosureUnitTest {

    private static final String TENANT_ID = "TENANT001";
    private static final String PATIENT_ID = "PATIENT001"; // Must be valid UUID if parsed as UUID, but existing test used generic string? 
    // In Consumer: parseUuid(message.get("patientId"))
    // private UUID parseUuid(Object uuid) { if (uuid == null) return null; return UUID.fromString(uuid.toString()); }
    // So PATIENT_ID must be a valid UUID string.
    
    // Changing constants to valid UUIDs to be safe
    private static final String VALID_PATIENT_UUID = UUID.randomUUID().toString();

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
            mapper,
            mock(com.healthdata.audit.service.AuditService.class)
        );

        Map<String, Object> message = new HashMap<>();
        // missing tenant and patient
        message.put("resourceType", "Procedure");
        message.put("resourceId", "Procedure/1");

        consumer.handleProcedureCreated(message, "topic");

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
            mapper,
            mock(com.healthdata.audit.service.AuditService.class)
        );

        Map<String, Object> message = new HashMap<>();
        message.put("resourceType", "Procedure");
        message.put("resourceId", "Procedure/2");
        message.put("tenantId", TENANT_ID);
        message.put("patientId", VALID_PATIENT_UUID);
        
        // Mock code/coding
        Map<String, Object> resource = new HashMap<>();
        resource.put("code", Map.of("text", "Colonoscopy"));
        message.put("resource", resource);

        when(matchingServiceMock.findMatchingCareGaps(
            eq(TENANT_ID), eq(UUID.fromString(VALID_PATIENT_UUID)), any(FhirResourceEvent.class))).thenReturn(List.of());

        consumer.handleProcedureCreated(message, "topic");

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
            mapper,
            mock(com.healthdata.audit.service.AuditService.class)
        );

        Map<String, Object> message = new HashMap<>();
        message.put("resourceType", "Procedure");
        message.put("resourceId", "Procedure/3");
        message.put("tenantId", TENANT_ID);
        message.put("patientId", VALID_PATIENT_UUID);

        CareGapEntity gap = CareGapEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(UUID.fromString(VALID_PATIENT_UUID))
            .gapType("screening-test")
            .category(CareGapEntity.GapCategory.SCREENING)
            .status(CareGapEntity.Status.OPEN)
            .build();

        when(matchingServiceMock.findMatchingCareGaps(
            eq(TENANT_ID), eq(UUID.fromString(VALID_PATIENT_UUID)), any(FhirResourceEvent.class))).thenReturn(List.of(gap));
        when(matchingServiceMock.getMatchingSummary(eq(gap), any(FhirResourceEvent.class))).thenReturn("match");
        when(mapper.writeValueAsString(any())).thenThrow(new RuntimeException("json fail"));

        consumer.handleProcedureCreated(message, "topic");

        verify(careGapServiceMock).autoCloseCareGap(
            eq(TENANT_ID),
            eq(gap.getId()),
            eq("Procedure"),
            eq("Procedure/3"),
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
            mapper,
            mock(com.healthdata.audit.service.AuditService.class)
        );

        Map<String, Object> message = new HashMap<>();
        message.put("resourceType", "Observation");
        message.put("resourceId", "Observation/1");
        message.put("tenantId", TENANT_ID);
        message.put("patientId", VALID_PATIENT_UUID);

        CareGapEntity gap = CareGapEntity.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .patientId(UUID.fromString(VALID_PATIENT_UUID))
            .gapType("lab-test")
            .category(CareGapEntity.GapCategory.SCREENING)
            .status(CareGapEntity.Status.OPEN)
            .build();

        when(matchingServiceMock.findMatchingCareGaps(
            eq(TENANT_ID), eq(UUID.fromString(VALID_PATIENT_UUID)), any(FhirResourceEvent.class))).thenReturn(List.of(gap));
        when(matchingServiceMock.getMatchingSummary(eq(gap), any(FhirResourceEvent.class))).thenReturn("match");
        when(mapper.writeValueAsString(any())).thenReturn("{\"event\":\"ok\"}");

        consumer.handleObservationCreated(message, "topic");

        verify(careGapServiceMock).autoCloseCareGap(
            eq(TENANT_ID),
            eq(gap.getId()),
            eq("Observation"),
            eq("Observation/1"),
            eq("match")
        );
    }
}
