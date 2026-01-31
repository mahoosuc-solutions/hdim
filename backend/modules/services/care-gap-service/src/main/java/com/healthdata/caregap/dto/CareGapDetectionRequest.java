package com.healthdata.caregap.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CareGapDetectionRequest {
    private UUID patientId;
    private String measureId;
    private boolean denominatorEligible;
    private boolean numeratorCompliant;
    private String createdBy;
}
