package com.healthdata.caregap.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private float closureRate;  // For population-level responses
}
