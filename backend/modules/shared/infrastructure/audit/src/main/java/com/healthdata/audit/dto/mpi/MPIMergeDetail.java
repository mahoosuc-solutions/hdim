package com.healthdata.audit.dto.mpi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Detailed MPI merge information including patient data comparison
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MPIMergeDetail {
    private String mergeId;
    private String tenantId;
    
    // Source and target patient information
    private PatientSnapshot sourcePatient;
    private PatientSnapshot targetPatient;
    private PatientSnapshot mergedPatient;
    
    // Merge metadata
    private String mergeType;
    private Double confidenceScore;
    private String mergeStatus;
    private String validationStatus;
    private LocalDateTime mergeTimestamp;
    private String performedBy;
    
    // Match details
    private List<AttributeMatch> matchedAttributes;
    private List<AttributeConflict> conflicts;
    private Map<String, Object> matchingAlgorithmDetails;
    
    // Data quality
    private List<DataQualityIssue> dataQualityIssues;
    private String overallDataQualityScore;
    
    // Audit trail
    private String validatedBy;
    private LocalDateTime validatedAt;
    private String validationNotes;
    private String rollbackReason;
    private LocalDateTime rolledBackAt;
    private String rolledBackBy;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PatientSnapshot {
        private String patientId;
        private String firstName;
        private String lastName;
        private String dateOfBirth;
        private String gender;
        private String ssn;
        private String mrn;
        private List<String> addresses;
        private List<String> phoneNumbers;
        private List<String> emails;
        private Integer recordCount;
        private LocalDateTime lastUpdated;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttributeMatch {
        private String attributeName;
        private String sourceValue;
        private String targetValue;
        private Double matchScore;
        private String matchType;  // EXACT, FUZZY, PHONETIC
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttributeConflict {
        private String attributeName;
        private String sourceValue;
        private String targetValue;
        private String resolvedValue;
        private String resolutionStrategy;  // SOURCE_PREFERRED, TARGET_PREFERRED, MANUAL
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DataQualityIssue {
        private String issueId;
        private String issueType;
        private String severity;
        private String description;
        private String affectedField;
        private String recommendation;
    }
}
