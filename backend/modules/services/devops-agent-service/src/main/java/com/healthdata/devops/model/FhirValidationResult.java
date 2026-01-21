package com.healthdata.devops.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FhirValidationResult {
    private String validationId;
    private Instant validationTimestamp;
    private String overallStatus;
    private Integer totalChecks;
    private Integer passedChecks;
    private Integer failedChecks;
    private Integer warningChecks;
    private List<ResourceCountCheck> resourceCountChecks;
    private List<CodeSystemCheck> codeSystemChecks;
    private List<AuthenticityCheck> authenticityChecks;
    private List<ComplianceCheck> complianceChecks;
    private List<RelationshipCheck> relationshipChecks;
    private Map<String, Object> summary;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceCountCheck {
        private String resourceType;
        private Integer actualCount;
        private Integer minimumRequired;
        private String status;
        private String message;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeSystemCheck {
        private String resourceType;
        private String codeSystem;
        private String code;
        private String description;
        private Integer count;
        private String status;
        private String message;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthenticityCheck {
        private String checkName;
        private String description;
        private String status;
        private String message;
        private Map<String, Object> details;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComplianceCheck {
        private String checkName;
        private String description;
        private String status;
        private String message;
        private List<String> errors;
        private List<String> warnings;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationshipCheck {
        private String relationshipType;
        private Integer patientCount;
        private Integer relatedResourceCount;
        private Double averageResourcesPerPatient;
        private String status;
        private String message;
    }
}
