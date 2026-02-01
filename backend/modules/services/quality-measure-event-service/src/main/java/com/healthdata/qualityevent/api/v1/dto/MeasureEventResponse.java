package com.healthdata.qualityevent.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MeasureEventResponse {

    private String patientId;
    private String measureCode;
    private String status;
    private String measureStatus;  // MET, NOT_MET, PARTIAL
    private String riskLevel;  // LOW, MEDIUM, HIGH, VERY_HIGH
    private float score;
    private long version;
    private Instant timestamp;
    private float complianceRate;  // For cohort-level aggregations
    private int numeratorCount;  // For cohort metrics
    private int denominatorCount;  // For cohort metrics
}
