package com.healthdata.healthixadapter.audit;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AtnaAuditEvent {
    private final String eventId;
    private final Instant timestamp;
    private final String sourceSystem;
    private final String tenantId;
    private final String eventType;
    private final String resourceType;
    private final String resourceId;
    private final String patientId;
    private final String phiLevel;
    private final String correlationId;
    private final String status;
    private final String errorMessage;
}
