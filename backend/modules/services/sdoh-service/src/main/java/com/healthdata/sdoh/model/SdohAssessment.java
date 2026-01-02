package com.healthdata.sdoh.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Complete SDOH Assessment for a patient
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SdohAssessment {
    private String assessmentId;
    private String patientId;
    private String tenantId;
    private LocalDateTime assessmentDate;
    private String screeningTool; // AHC-HRSN, PRAPARE, etc.
    private List<SdohScreeningResponse> responses;
    private Map<SdohCategory, Boolean> identifiedNeeds;
    private List<String> identifiedZCodes;
    private Double riskScore;
    private String assessedBy;
    private AssessmentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum AssessmentStatus {
        IN_PROGRESS,
        COMPLETED,
        REVIEWED,
        ARCHIVED
    }
}
