package com.healthdata.audit.models.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Audit event for user configuration actions.
 * 
 * Tracks:
 * - User-initiated configuration changes
 * - UI interactions with configuration tools
 * - Natural language queries to AI configuration assistant
 * - Approval/rejection of AI recommendations
 * - Manual overrides of auto-scaled settings
 * - Configuration validation requests
 * 
 * Enables:
 * - User activity monitoring
 * - Compliance auditing for manual changes
 * - User experience analytics
 * - Training data for AI improvements
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserConfigurationActionEvent {

    @JsonProperty("eventId")
    private UUID eventId;

    @JsonProperty("timestamp")
    private Instant timestamp;

    @JsonProperty("tenantId")
    private String tenantId;

    @JsonProperty("eventType")
    @Builder.Default
    private String eventType = "USER_CONFIGURATION_ACTION";

    // User Identity
    @JsonProperty("userId")
    private String userId;

    @JsonProperty("username")
    private String username;

    @JsonProperty("userRole")
    private String userRole; // e.g., ADMIN, OPERATOR, DEVELOPER

    @JsonProperty("userEmail")
    private String userEmail;

    // Action Context
    @JsonProperty("actionType")
    private ActionType actionType;

    @JsonProperty("actionSource")
    private ActionSource actionSource;

    @JsonProperty("sessionId")
    private String sessionId; // UI session tracking

    @JsonProperty("ipAddress")
    private String ipAddress;

    @JsonProperty("userAgent")
    private String userAgent;

    // Configuration Details
    @JsonProperty("serviceName")
    private String serviceName;

    @JsonProperty("resourceType")
    private String resourceType;

    @JsonProperty("resourceId")
    private String resourceId;

    @JsonProperty("configurationKey")
    private String configurationKey;

    @JsonProperty("requestedValue")
    private Object requestedValue;

    @JsonProperty("appliedValue")
    private Object appliedValue; // May differ due to validation/constraints

    // AI Interaction
    @JsonProperty("aiRecommendationId")
    private UUID aiRecommendationId; // If responding to AI recommendation

    @JsonProperty("aiRecommendationAction")
    private AIRecommendationAction aiRecommendationAction;

    @JsonProperty("naturalLanguageQuery")
    private String naturalLanguageQuery; // If user asked AI assistant

    @JsonProperty("aiResponseId")
    private UUID aiResponseId; // Link to AI response

    @JsonProperty("userFeedbackRating")
    private Integer userFeedbackRating; // 1-5 stars on AI recommendation

    @JsonProperty("userFeedbackComment")
    private String userFeedbackComment;

    // Action Outcome
    @JsonProperty("actionStatus")
    private ActionStatus actionStatus;

    @JsonProperty("validationErrors")
    private List<ValidationError> validationErrors;

    @JsonProperty("warningMessages")
    private List<String> warningMessages;

    @JsonProperty("errorMessage")
    private String errorMessage;

    @JsonProperty("successMessage")
    private String successMessage;

    // Impact Assessment
    @JsonProperty("impactAssessment")
    private ImpactAssessment impactAssessment;

    @JsonProperty("requiresApproval")
    private Boolean requiresApproval;

    @JsonProperty("approvalStatus")
    private ApprovalStatus approvalStatus;

    @JsonProperty("approvedBy")
    private String approvedBy;

    @JsonProperty("approvedAt")
    private Instant approvedAt;

    // Compliance & Tracing
    @JsonProperty("requestId")
    private String requestId;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("changeTicketId")
    private String changeTicketId; // For production changes requiring change management

    @JsonProperty("environment")
    private String environment;

    /**
     * Types of user actions
     */
    public enum ActionType {
        VIEW_CONFIGURATION,           // User viewed current config
        EDIT_CONFIGURATION,          // User changed a configuration value
        CREATE_TENANT_OVERRIDE,      // User created tenant-specific override
        DELETE_TENANT_OVERRIDE,      // User removed tenant override
        VALIDATE_CONFIGURATION,      // User requested config validation
        QUERY_AI_ASSISTANT,          // User asked AI for help
        ACCEPT_AI_RECOMMENDATION,    // User accepted AI recommendation
        REJECT_AI_RECOMMENDATION,    // User rejected AI recommendation
        MODIFY_AI_RECOMMENDATION,    // User modified then applied AI recommendation
        REQUEST_IMPACT_ANALYSIS,     // User requested what-if analysis
        ROLLBACK_CHANGE,             // User rolled back a change
        EXPORT_CONFIGURATION,        // User exported config
        IMPORT_CONFIGURATION,        // User imported config
        COMPARE_CONFIGURATIONS,      // User compared configs across environments
        SCHEDULE_CHANGE,             // User scheduled a future change
        CANCEL_SCHEDULED_CHANGE      // User cancelled scheduled change
    }

    /**
     * Source of the user action
     */
    public enum ActionSource {
        WEB_UI,                  // Clinical portal web interface
        MOBILE_APP,              // Mobile application
        REST_API,                // Direct API call
        CLI_TOOL,                // Command line interface
        CHATBOT,                 // AI chatbot interface
        CONFIGURATION_WIZARD,    // Guided setup wizard
        SLACK_INTEGRATION,       // Slack bot integration
        EMAIL_COMMAND            // Email-based command
    }

    /**
     * User action on AI recommendations
     */
    public enum AIRecommendationAction {
        VIEWED,
        ACCEPTED,
        REJECTED,
        MODIFIED_THEN_ACCEPTED,
        REQUESTED_EXPLANATION,
        REQUESTED_ALTERNATIVES,
        FLAGGED_AS_INCORRECT,
        DEFERRED_FOR_LATER
    }

    /**
     * Status of the user action
     */
    public enum ActionStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        VALIDATION_FAILED,
        CANCELLED,
        REQUIRES_APPROVAL,
        APPROVED,
        REJECTED_BY_APPROVER,
        SCHEDULED
    }

    /**
     * Validation error details
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ValidationError {
        @JsonProperty("field")
        private String field;

        @JsonProperty("errorCode")
        private String errorCode;

        @JsonProperty("errorMessage")
        private String errorMessage;

        @JsonProperty("constraint")
        private String constraint; // e.g., "min: 1, max: 100"

        @JsonProperty("providedValue")
        private Object providedValue;
    }

    /**
     * Impact assessment of the user action
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ImpactAssessment {
        @JsonProperty("riskLevel")
        private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL

        @JsonProperty("affectedTenants")
        private Integer affectedTenants;

        @JsonProperty("affectedUsers")
        private Integer affectedUsers;

        @JsonProperty("estimatedDowntime")
        private String estimatedDowntime; // e.g., "0 minutes" or "5-10 minutes"

        @JsonProperty("rollbackAvailable")
        private Boolean rollbackAvailable;

        @JsonProperty("testingRequired")
        private Boolean testingRequired;

        @JsonProperty("estimatedImpact")
        private Map<String, String> estimatedImpact; // Key metrics and their expected changes
    }

    /**
     * Approval status for high-risk changes
     */
    public enum ApprovalStatus {
        NOT_REQUIRED,
        PENDING_APPROVAL,
        APPROVED,
        REJECTED,
        AUTO_APPROVED,
        EXPIRED
    }

}
