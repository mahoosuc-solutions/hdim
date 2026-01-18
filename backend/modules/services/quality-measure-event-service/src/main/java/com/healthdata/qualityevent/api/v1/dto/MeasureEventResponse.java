package com.healthdata.qualityevent.api.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeasureEventResponse {

    private String measureCode;
    private String status;
    private String measureStatus;  // MET, NOT_MET, PARTIAL
    private String riskLevel;  // LOW, MEDIUM, HIGH, VERY_HIGH
    private float score;
    private long version;
    private Instant timestamp;
    private float complianceRate;  // For cohort-level aggregations
}
