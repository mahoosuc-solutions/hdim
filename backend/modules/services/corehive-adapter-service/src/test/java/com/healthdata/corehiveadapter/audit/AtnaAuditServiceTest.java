package com.healthdata.corehiveadapter.audit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class AtnaAuditServiceTest {

    private final AtnaAuditService auditService = new AtnaAuditService("corehive-adapter-service", "NONE");

    @Test
    void buildAuditEvent_includesAllRequiredFields() {
        AtnaAuditEvent event = auditService.buildAuditEvent(
                "tenant-1", "EXTERNAL_API_CALL", "ScoringRequest",
                "req-123", null, "corr-456", "SUCCESS", null);

        assertThat(event.getSourceSystem()).isEqualTo("corehive-adapter-service");
        assertThat(event.getTenantId()).isEqualTo("tenant-1");
        assertThat(event.getEventType()).isEqualTo("EXTERNAL_API_CALL");
        assertThat(event.getResourceType()).isEqualTo("ScoringRequest");
        assertThat(event.getResourceId()).isEqualTo("req-123");
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getTimestamp()).isBefore(Instant.now().plusSeconds(1));
        assertThat(event.getCorrelationId()).isEqualTo("corr-456");
        assertThat(event.getStatus()).isEqualTo("SUCCESS");
        assertThat(event.getPhiLevel()).isEqualTo("NONE");
    }

    @Test
    void buildAuditEvent_failureIncludesErrorMessage() {
        AtnaAuditEvent event = auditService.buildAuditEvent(
                "tenant-1", "PHI_BOUNDARY_CHECK", "Request",
                "req-123", "patient-abc", "corr-789", "FAILURE", "Error occurred");

        assertThat(event.getStatus()).isEqualTo("FAILURE");
        assertThat(event.getErrorMessage()).isEqualTo("Error occurred");
    }

    @Test
    void auditEvent_hasRfc3881EventId() {
        AtnaAuditEvent event = auditService.buildAuditEvent(
                "tenant-1", "EXTERNAL_API_CALL", "Request",
                "req-1", null, "corr-1", "SUCCESS", null);

        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventId()).isNotEmpty();
    }
}
