package com.healthdata.nurseworkflow.api.v1.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PatientEngagementKpiResponse {
    private Instant windowStart;
    private Instant windowEnd;
    private long totalThreads;
    private long openThreads;
    private long totalMessages;
    private long patientMessages;
    private long clinicianMessages;
    private long totalEscalations;
    private long criticalEscalations;
}
