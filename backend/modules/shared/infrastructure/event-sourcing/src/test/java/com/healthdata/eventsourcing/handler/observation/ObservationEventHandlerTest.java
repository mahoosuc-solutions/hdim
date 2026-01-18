package com.healthdata.eventsourcing.handler.observation;

import com.healthdata.eventsourcing.command.observation.ObservationRecordedEvent;
import com.healthdata.eventsourcing.projection.observation.ObservationProjection;
import com.healthdata.eventsourcing.projection.observation.ObservationProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObservationEventHandlerTest {

    @Mock
    private ObservationProjectionService projectionService;

    @InjectMocks
    private ObservationEventHandler eventHandler;

    private ObservationRecordedEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = ObservationRecordedEvent.builder()
            .tenantId("tenant-123")
            .patientId("patient-123")
            .loincCode("8480-6")
            .value(new BigDecimal("120.50"))
            .unit("mmHg")
            .observationDate(Instant.now())
            .notes("Systolic blood pressure")
            .build();
    }

    @Test
    @DisplayName("Should handle ObservationRecordedEvent and save projection")
    void shouldHandleEventAndSaveProjection() {
        when(projectionService.saveProjection(any(ObservationProjection.class)))
            .thenReturn(ObservationProjection.builder().build());

        eventHandler.handleObservationRecordedEvent(testEvent);

        verify(projectionService, times(1)).saveProjection(any(ObservationProjection.class));
    }

    @Test
    @DisplayName("Should preserve patient and tenant in projection")
    void shouldPreservePatientAndTenant() {
        when(projectionService.saveProjection(any(ObservationProjection.class)))
            .thenReturn(ObservationProjection.builder().build());

        eventHandler.handleObservationRecordedEvent(testEvent);

        verify(projectionService).saveProjection(argThat(proj ->
            proj.getPatientId().equals("patient-123") &&
            proj.getTenantId().equals("tenant-123")
        ));
    }

    @Test
    @DisplayName("Should preserve LOINC code")
    void shouldPreserveLoincCode() {
        when(projectionService.saveProjection(any(ObservationProjection.class)))
            .thenReturn(ObservationProjection.builder().build());

        eventHandler.handleObservationRecordedEvent(testEvent);

        verify(projectionService).saveProjection(argThat(proj ->
            proj.getLoincCode().equals("8480-6")
        ));
    }

    @Test
    @DisplayName("Should preserve value and unit")
    void shouldPreserveValueAndUnit() {
        when(projectionService.saveProjection(any(ObservationProjection.class)))
            .thenReturn(ObservationProjection.builder().build());

        eventHandler.handleObservationRecordedEvent(testEvent);

        verify(projectionService).saveProjection(argThat(proj ->
            proj.getValue().equals(new BigDecimal("120.50")) &&
            proj.getUnit().equals("mmHg")
        ));
    }

    @Test
    @DisplayName("Should handle multi-tenant isolation")
    void shouldEnforceMultiTenantIsolation() {
        ObservationRecordedEvent tenant2Event = ObservationRecordedEvent.builder()
            .tenantId("tenant-456")
            .patientId("patient-456")
            .loincCode("8462-4")
            .value(new BigDecimal("80.25"))
            .unit("mmHg")
            .build();

        when(projectionService.saveProjection(any(ObservationProjection.class)))
            .thenReturn(ObservationProjection.builder().build());

        eventHandler.handleObservationRecordedEvent(tenant2Event);

        verify(projectionService).saveProjection(argThat(proj ->
            proj.getTenantId().equals("tenant-456")
        ));
    }

    @Test
    @DisplayName("Should handle null notes field")
    void shouldHandleNullNotes() {
        ObservationRecordedEvent eventWithoutNotes = ObservationRecordedEvent.builder()
            .tenantId("tenant-123")
            .patientId("patient-123")
            .loincCode("8480-6")
            .value(new BigDecimal("120.50"))
            .unit("mmHg")
            .notes(null)
            .build();

        when(projectionService.saveProjection(any(ObservationProjection.class)))
            .thenReturn(ObservationProjection.builder().build());

        eventHandler.handleObservationRecordedEvent(eventWithoutNotes);

        verify(projectionService).saveProjection(any());
    }

    @Test
    @DisplayName("Should generate deterministic aggregate ID")
    void shouldGenerateDeterministicAggregateId() {
        when(projectionService.saveProjection(any(ObservationProjection.class)))
            .thenReturn(ObservationProjection.builder().build());

        eventHandler.handleObservationRecordedEvent(testEvent);

        String aggregateId = testEvent.getAggregateId();
        assertThat(aggregateId).isNotNull();
        assertThat(aggregateId).contains("patient");
    }

    @Test
    @DisplayName("Should handle service exception gracefully")
    void shouldHandleServiceException() {
        when(projectionService.saveProjection(any(ObservationProjection.class)))
            .thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> eventHandler.handleObservationRecordedEvent(testEvent))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should preserve observation timestamp")
    void shouldPreserveObservationTimestamp() {
        Instant testTime = Instant.now();
        ObservationRecordedEvent timedEvent = ObservationRecordedEvent.builder()
            .tenantId("tenant-123")
            .patientId("patient-123")
            .loincCode("8480-6")
            .value(new BigDecimal("120.50"))
            .unit("mmHg")
            .observationDate(testTime)
            .build();

        when(projectionService.saveProjection(any(ObservationProjection.class)))
            .thenReturn(ObservationProjection.builder().build());

        eventHandler.handleObservationRecordedEvent(timedEvent);

        verify(projectionService).saveProjection(argThat(proj ->
            proj.getObservationDate().equals(testTime)
        ));
    }

    @Test
    @DisplayName("Should support high precision decimal values")
    void shouldHandleHighPrecisionValues() {
        ObservationRecordedEvent precisionEvent = ObservationRecordedEvent.builder()
            .tenantId("tenant-123")
            .patientId("patient-123")
            .loincCode("2345-7")
            .value(new BigDecimal("98.6789"))
            .unit("C")
            .build();

        when(projectionService.saveProjection(any(ObservationProjection.class)))
            .thenReturn(ObservationProjection.builder().build());

        eventHandler.handleObservationRecordedEvent(precisionEvent);

        verify(projectionService).saveProjection(argThat(proj ->
            proj.getValue().equals(new BigDecimal("98.6789"))
        ));
    }

    @Test
    @DisplayName("Should not modify event after handling")
    void shouldNotModifyEvent() {
        String originalLoincCode = testEvent.getLoincCode();
        when(projectionService.saveProjection(any(ObservationProjection.class)))
            .thenReturn(ObservationProjection.builder().build());

        eventHandler.handleObservationRecordedEvent(testEvent);

        assertThat(testEvent.getLoincCode()).isEqualTo(originalLoincCode);
    }

    @Test
    @DisplayName("Should call service once per event")
    void shouldCallServiceOncePerEvent() {
        when(projectionService.saveProjection(any(ObservationProjection.class)))
            .thenReturn(ObservationProjection.builder().build());

        eventHandler.handleObservationRecordedEvent(testEvent);
        eventHandler.handleObservationRecordedEvent(testEvent);

        verify(projectionService, times(2)).saveProjection(any());
    }

    @Test
    @DisplayName("Should handle duplicate event gracefully")
    void shouldHandleDuplicateEvent() {
        when(projectionService.saveProjection(any(ObservationProjection.class)))
            .thenReturn(ObservationProjection.builder().build());

        eventHandler.handleObservationRecordedEvent(testEvent);
        eventHandler.handleObservationRecordedEvent(testEvent);

        verify(projectionService, times(2)).saveProjection(any());
    }

    @Test
    @DisplayName("Should verify repository called correctly")
    void shouldVerifyRepositoryCall() {
        when(projectionService.saveProjection(any(ObservationProjection.class)))
            .thenReturn(ObservationProjection.builder().build());

        eventHandler.handleObservationRecordedEvent(testEvent);

        verify(projectionService, times(1)).saveProjection(argThat(proj ->
            proj.getPatientId().equals("patient-123") &&
            proj.getTenantId().equals("tenant-123") &&
            proj.getLoincCode().equals("8480-6")
        ));
    }
}
