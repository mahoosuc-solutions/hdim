package com.healthdata.ehr.connector.cerner.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CernerErrorResponse {
    @JsonProperty("resourceType")
    private String resourceType;
    @JsonProperty("issue")
    private List<Issue> issues;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Issue {
        @JsonProperty("severity")
        private String severity;
        @JsonProperty("code")
        private String code;
        @JsonProperty("details")
        private Details details;
        @JsonProperty("diagnostics")
        private String diagnostics;
        @JsonProperty("expression")
        private List<String> expressions;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Details {
        @JsonProperty("coding")
        private List<Coding> coding;
        @JsonProperty("text")
        private String text;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Coding {
        @JsonProperty("system")
        private String system;
        @JsonProperty("code")
        private String code;
        @JsonProperty("display")
        private String display;
    }
    
    public String getMessage() {
        if (issues == null || issues.isEmpty()) {
            return "Unknown error";
        }
        Issue firstIssue = issues.get(0);
        if (firstIssue.getDiagnostics() != null) {
            return firstIssue.getDiagnostics();
        }
        if (firstIssue.getDetails() != null && firstIssue.getDetails().getText() != null) {
            return firstIssue.getDetails().getText();
        }
        return firstIssue.getCode() != null ? firstIssue.getCode() : "Unknown error";
    }
}
