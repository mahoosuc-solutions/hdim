package com.healthdata.caregap.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareGapEventResponse {

    private String gapCode;
    private String severity;  // CRITICAL, HIGH, MEDIUM, LOW
    private String status;  // OPEN, CLOSED, WAIVED
    private String description;
    private LocalDate detectionDate;
    private LocalDate closureDate;
    private int daysOpen;
    private String recommendedIntervention;
    private boolean patientQualified;
    private Instant timestamp;
    private double closureRate;  // For population-level responses
    private String patientId;  // For response mapping
    private int totalGapsOpen;  // For population health responses
    private int criticalGaps;
    private int highGaps;
    private int mediumGaps;
    private int lowGaps;
    private int gapsClosed;
}
