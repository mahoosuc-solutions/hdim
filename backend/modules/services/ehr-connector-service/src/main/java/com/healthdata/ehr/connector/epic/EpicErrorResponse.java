package com.healthdata.ehr.connector.epic;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Error response model from Epic FHIR API.
 */
public class EpicErrorResponse {

    @JsonProperty("error")
    private String error;

    @JsonProperty("error_description")
    private String errorDescription;

    @JsonProperty("resourceType")
    private String resourceType;

    @JsonProperty("issue")
    private java.util.List<Issue> issues;

    public static class Issue {
        private String severity;
        private String code;
        private Details details;
        private String diagnostics;

        public static class Details {
            private java.util.List<Coding> coding;
            private String text;

            public static class Coding {
                private String system;
                private String code;
                private String display;

                public String getSystem() {
                    return system;
                }

                public void setSystem(String system) {
                    this.system = system;
                }

                public String getCode() {
                    return code;
                }

                public void setCode(String code) {
                    this.code = code;
                }

                public String getDisplay() {
                    return display;
                }

                public void setDisplay(String display) {
                    this.display = display;
                }
            }

            public java.util.List<Coding> getCoding() {
                return coding;
            }

            public void setCoding(java.util.List<Coding> coding) {
                this.coding = coding;
            }

            public String getText() {
                return text;
            }

            public void setText(String text) {
                this.text = text;
            }
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public Details getDetails() {
            return details;
        }

        public void setDetails(Details details) {
            this.details = details;
        }

        public String getDiagnostics() {
            return diagnostics;
        }

        public void setDiagnostics(String diagnostics) {
            this.diagnostics = diagnostics;
        }
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public java.util.List<Issue> getIssues() {
        return issues;
    }

    public void setIssues(java.util.List<Issue> issues) {
        this.issues = issues;
    }

    public String getMessage() {
        if (errorDescription != null) {
            return errorDescription;
        }
        if (issues != null && !issues.isEmpty()) {
            Issue issue = issues.get(0);
            if (issue.getDiagnostics() != null) {
                return issue.getDiagnostics();
            }
            if (issue.getDetails() != null && issue.getDetails().getText() != null) {
                return issue.getDetails().getText();
            }
        }
        return error != null ? error : "Unknown error";
    }
}
