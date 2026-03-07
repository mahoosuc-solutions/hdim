package com.healthdata.healthixadapter.integration;

import com.healthdata.healthixadapter.model.HealthixAuditLog;
import com.healthdata.healthixadapter.model.HealthixAuditLogRepository;
import com.healthdata.healthixadapter.service.HealthixAuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * HIPAA audit logging verification for Healthix adapter.
 * Ensures all PHI access operations are properly logged with
 * required fields: tenantId, eventType, resourceType, patientId,
 * phiLevel, correlationId, and status.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit")
@DisplayName("HIPAA Audit: Healthix PHI access logging")
class HealthixAuditLoggingTest {

    @Mock
    private HealthixAuditLogRepository auditLogRepository;

    @InjectMocks
    private HealthixAuditService auditService;

    @Test
    @DisplayName("PHI access should be logged with all required HIPAA fields")
    void phiAccess_shouldLogAllRequiredFields() {
        auditService.logPhiAccess(
                "tenant-acme",
                "FHIR_RESOURCE_RECEIVED",
                "Patient",
                "resource-123",
                "patient-456",
                "correlation-789"
        );

        ArgumentCaptor<HealthixAuditLog> captor = ArgumentCaptor.forClass(HealthixAuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        HealthixAuditLog log = captor.getValue();
        assertThat(log.getTenantId()).isEqualTo("tenant-acme");
        assertThat(log.getEventType()).isEqualTo("FHIR_RESOURCE_RECEIVED");
        assertThat(log.getSourceSystem()).isEqualTo("HEALTHIX");
        assertThat(log.getResourceType()).isEqualTo("Patient");
        assertThat(log.getResourceId()).isEqualTo("resource-123");
        assertThat(log.getPatientId()).isEqualTo("patient-456");
        assertThat(log.getPhiLevel()).isEqualTo("FULL");
        assertThat(log.getCorrelationId()).isEqualTo("correlation-789");
        assertThat(log.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("PHI access failure should be logged with error details")
    void phiAccessFailure_shouldLogErrorDetails() {
        auditService.logPhiAccessFailure(
                "tenant-acme",
                "MPI_QUERY_FAILED",
                "Patient",
                "patient-456",
                "correlation-789",
                "Connection timeout to Verato MPI"
        );

        ArgumentCaptor<HealthixAuditLog> captor = ArgumentCaptor.forClass(HealthixAuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        HealthixAuditLog log = captor.getValue();
        assertThat(log.getStatus()).isEqualTo("FAILURE");
        assertThat(log.getErrorMessage()).isEqualTo("Connection timeout to Verato MPI");
        assertThat(log.getPhiLevel()).isEqualTo("FULL");
    }

    @Test
    @DisplayName("Audit entries should always have FULL PHI level for Healthix")
    void auditEntries_shouldAlwaysHaveFullPhiLevel() {
        auditService.logPhiAccess("t1", "CCDA_INGEST", "Document", "doc-1", "pat-1", "corr-1");

        ArgumentCaptor<HealthixAuditLog> captor = ArgumentCaptor.forClass(HealthixAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getPhiLevel()).isEqualTo("FULL");
    }
}
