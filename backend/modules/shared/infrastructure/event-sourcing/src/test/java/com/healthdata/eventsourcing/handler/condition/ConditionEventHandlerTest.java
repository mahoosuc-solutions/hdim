package com.healthdata.eventsourcing.handler.condition;

import com.healthdata.eventsourcing.command.condition.ConditionDiagnosedEvent;
import com.healthdata.eventsourcing.projection.condition.ConditionProjection;
import com.healthdata.eventsourcing.projection.condition.ConditionProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConditionEventHandlerTest {

    @Mock
    private ConditionProjectionService projectionService;

    @InjectMocks
    private ConditionEventHandler eventHandler;

    private ConditionDiagnosedEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = ConditionDiagnosedEvent.builder()
            .tenantId("tenant-123")
            .patientId("patient-123")
            .icdCode("E11.9")
            .clinicalStatus("active")
            .verificationStatus("confirmed")
            .onsetDate(LocalDate.of(2020, 1, 15))
            .build();
    }

    @Test
    @DisplayName("Should handle ConditionDiagnosedEvent and save projection")
    void shouldHandleEventAndSaveProjection() {
        when(projectionService.saveProjection(any(ConditionProjection.class)))
            .thenReturn(ConditionProjection.builder().build());

        eventHandler.handleConditionDiagnosedEvent(testEvent);

        verify(projectionService, times(1)).saveProjection(any());
    }

    @Test
    @DisplayName("Should preserve patient and tenant")
    void shouldPreservePatientAndTenant() {
        when(projectionService.saveProjection(any(ConditionProjection.class)))
            .thenReturn(ConditionProjection.builder().build());

        eventHandler.handleConditionDiagnosedEvent(testEvent);

        verify(projectionService).saveProjection(argThat(proj ->
            proj.getPatientId().equals("patient-123") &&
            proj.getTenantId().equals("tenant-123")
        ));
    }

    @Test
    @DisplayName("Should preserve ICD code")
    void shouldPreserveIcdCode() {
        when(projectionService.saveProjection(any(ConditionProjection.class)))
            .thenReturn(ConditionProjection.builder().build());

        eventHandler.handleConditionDiagnosedEvent(testEvent);

        verify(projectionService).saveProjection(argThat(proj ->
            proj.getIcdCode().equals("E11.9")
        ));
    }

    @Test
    @DisplayName("Should preserve status and verification")
    void shouldPreserveStatusAndVerification() {
        when(projectionService.saveProjection(any(ConditionProjection.class)))
            .thenReturn(ConditionProjection.builder().build());

        eventHandler.handleConditionDiagnosedEvent(testEvent);

        verify(projectionService).saveProjection(argThat(proj ->
            proj.getVerificationStatus().equals("confirmed")
        ));
    }

    @Test
    @DisplayName("Should handle multi-tenant isolation")
    void shouldEnforceMultiTenantIsolation() {
        ConditionDiagnosedEvent tenant2Event = ConditionDiagnosedEvent.builder()
            .tenantId("tenant-456")
            .patientId("patient-456")
            .icdCode("I10")
            .clinicalStatus("active")
            .verificationStatus("confirmed")
            .build();

        when(projectionService.saveProjection(any(ConditionProjection.class)))
            .thenReturn(ConditionProjection.builder().build());

        eventHandler.handleConditionDiagnosedEvent(tenant2Event);

        verify(projectionService).saveProjection(argThat(proj ->
            proj.getTenantId().equals("tenant-456")
        ));
    }

    @Test
    @DisplayName("Should handle null onset date")
    void shouldHandleNullOnsetDate() {
        ConditionDiagnosedEvent eventWithoutOnsetDate = ConditionDiagnosedEvent.builder()
            .tenantId("tenant-123")
            .patientId("patient-123")
            .icdCode("E11.9")
            .clinicalStatus("active")
            .verificationStatus("confirmed")
            .onsetDate(null)
            .build();

        when(projectionService.saveProjection(any(ConditionProjection.class)))
            .thenReturn(ConditionProjection.builder().build());

        eventHandler.handleConditionDiagnosedEvent(eventWithoutOnsetDate);

        verify(projectionService).saveProjection(any());
    }

    @Test
    @DisplayName("Should handle service exception")
    void shouldHandleServiceException() {
        when(projectionService.saveProjection(any(ConditionProjection.class)))
            .thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> eventHandler.handleConditionDiagnosedEvent(testEvent))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should preserve onset date")
    void shouldPreserveOnsetDate() {
        LocalDate testDate = LocalDate.of(2019, 5, 10);
        ConditionDiagnosedEvent timedEvent = ConditionDiagnosedEvent.builder()
            .tenantId("tenant-123")
            .patientId("patient-123")
            .icdCode("I10")
            .onsetDate(testDate)
            .build();

        when(projectionService.saveProjection(any(ConditionProjection.class)))
            .thenReturn(ConditionProjection.builder().build());

        eventHandler.handleConditionDiagnosedEvent(timedEvent);

        verify(projectionService).saveProjection(argThat(proj ->
            proj.getOnsetDate().equals(testDate)
        ));
    }

    @Test
    @DisplayName("Should not modify event after handling")
    void shouldNotModifyEvent() {
        String originalCode = testEvent.getIcdCode();
        when(projectionService.saveProjection(any(ConditionProjection.class)))
            .thenReturn(ConditionProjection.builder().build());

        eventHandler.handleConditionDiagnosedEvent(testEvent);

        assertThat(testEvent.getIcdCode()).isEqualTo(originalCode);
    }

    @Test
    @DisplayName("Should call service once per event")
    void shouldCallServiceOncePerEvent() {
        when(projectionService.saveProjection(any(ConditionProjection.class)))
            .thenReturn(ConditionProjection.builder().build());

        eventHandler.handleConditionDiagnosedEvent(testEvent);
        eventHandler.handleConditionDiagnosedEvent(testEvent);

        verify(projectionService, times(2)).saveProjection(any());
    }

    @Test
    @DisplayName("Should handle active condition clinical status")
    void shouldHandleActiveStatus() {
        when(projectionService.saveProjection(any(ConditionProjection.class)))
            .thenReturn(ConditionProjection.builder().build());

        eventHandler.handleConditionDiagnosedEvent(testEvent);

        verify(projectionService).saveProjection(any());
    }
}
