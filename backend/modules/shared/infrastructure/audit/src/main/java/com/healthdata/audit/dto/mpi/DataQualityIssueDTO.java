package com.healthdata.audit.dto.mpi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Data quality issue associated with patient records
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQualityIssueDTO {
    private String issueId;
    private String tenantId;
    private String patientId;
    private String issueType;         // DUPLICATE, INCOMPLETE, INCONSISTENT, INVALID
    private String severity;          // CRITICAL, HIGH, MEDIUM, LOW
    private String status;            // OPEN, IN_PROGRESS, RESOLVED, IGNORED
    private String description;
    private String affectedField;
    private String currentValue;
    private String suggestedValue;
    private String recommendation;
    private LocalDateTime detectedAt;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private String resolutionNotes;
}
