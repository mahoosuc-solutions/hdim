package com.healthdata.events.intelligence.dto;

public record TrustProfileResponse(
        String patientRef,
        int trustScore,
        long openFindings,
        long highSeverityOpenFindings,
        long consistencyFindings,
        long dataCompletenessFindings,
        long temporalFindings
) {
}
