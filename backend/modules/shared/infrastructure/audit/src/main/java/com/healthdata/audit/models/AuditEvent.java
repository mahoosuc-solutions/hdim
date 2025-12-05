package com.healthdata.audit.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

/**
 * HIPAA-compliant audit event for tracking all access to Protected Health Information (PHI).
 *
 * Meets requirements:
 * - 45 CFR § 164.312(b) - Audit Controls
 * - 45 CFR § 164.308(a)(1)(ii)(D) - Information System Activity Review
 * - 7-year retention requirement
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditEvent {

    private UUID id;
    private Instant timestamp;
    private String tenantId;

    // Who - User performing the action
    private String userId;
    private String username;
    private String role;
    private String ipAddress;
    private String userAgent;

    // What - Action performed
    private AuditAction action;
    private String resourceType;  // e.g., "Patient", "Observation"
    private String resourceId;
    private AuditOutcome outcome;

    // Where - System context
    private String serviceName;
    private String methodName;
    private String requestPath;

    // Why - Purpose of use (HIPAA requirement)
    private String purposeOfUse;  // Treatment, Payment, Operations, Research, etc.

    // Additional context
    private JsonNode requestPayload;  // Encrypted if contains PHI
    private JsonNode responsePayload; // Encrypted if contains PHI
    private String errorMessage;
    private Long durationMs;

    // FHIR AuditEvent reference (optional)
    private String fhirAuditEventId;

    // Encryption flag
    private boolean encrypted;

    // Constructors
    public AuditEvent() {
        this.id = UUID.randomUUID();
        this.timestamp = Instant.now();
        this.encrypted = false;
    }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final AuditEvent event = new AuditEvent();

        public Builder tenantId(String tenantId) {
            event.tenantId = tenantId;
            return this;
        }

        public Builder userId(String userId) {
            event.userId = userId;
            return this;
        }

        public Builder username(String username) {
            event.username = username;
            return this;
        }

        public Builder role(String role) {
            event.role = role;
            return this;
        }

        public Builder ipAddress(String ipAddress) {
            event.ipAddress = ipAddress;
            return this;
        }

        public Builder userAgent(String userAgent) {
            event.userAgent = userAgent;
            return this;
        }

        public Builder action(AuditAction action) {
            event.action = action;
            return this;
        }

        public Builder resourceType(String resourceType) {
            event.resourceType = resourceType;
            return this;
        }

        public Builder resourceId(String resourceId) {
            event.resourceId = resourceId;
            return this;
        }

        public Builder outcome(AuditOutcome outcome) {
            event.outcome = outcome;
            return this;
        }

        public Builder serviceName(String serviceName) {
            event.serviceName = serviceName;
            return this;
        }

        public Builder methodName(String methodName) {
            event.methodName = methodName;
            return this;
        }

        public Builder requestPath(String requestPath) {
            event.requestPath = requestPath;
            return this;
        }

        public Builder purposeOfUse(String purposeOfUse) {
            event.purposeOfUse = purposeOfUse;
            return this;
        }

        public Builder requestPayload(JsonNode requestPayload) {
            event.requestPayload = requestPayload;
            return this;
        }

        public Builder responsePayload(JsonNode responsePayload) {
            event.responsePayload = responsePayload;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            event.errorMessage = errorMessage;
            return this;
        }

        public Builder durationMs(Long durationMs) {
            event.durationMs = durationMs;
            return this;
        }

        public Builder fhirAuditEventId(String fhirAuditEventId) {
            event.fhirAuditEventId = fhirAuditEventId;
            return this;
        }

        public Builder encrypted(boolean encrypted) {
            event.encrypted = encrypted;
            return this;
        }

        public AuditEvent build() {
            return event;
        }
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public AuditAction getAction() { return action; }
    public void setAction(AuditAction action) { this.action = action; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public AuditOutcome getOutcome() { return outcome; }
    public void setOutcome(AuditOutcome outcome) { this.outcome = outcome; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public String getRequestPath() { return requestPath; }
    public void setRequestPath(String requestPath) { this.requestPath = requestPath; }

    public String getPurposeOfUse() { return purposeOfUse; }
    public void setPurposeOfUse(String purposeOfUse) { this.purposeOfUse = purposeOfUse; }

    public JsonNode getRequestPayload() { return requestPayload; }
    public void setRequestPayload(JsonNode requestPayload) { this.requestPayload = requestPayload; }

    public JsonNode getResponsePayload() { return responsePayload; }
    public void setResponsePayload(JsonNode responsePayload) { this.responsePayload = responsePayload; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public String getFhirAuditEventId() { return fhirAuditEventId; }
    public void setFhirAuditEventId(String fhirAuditEventId) { this.fhirAuditEventId = fhirAuditEventId; }

    public boolean isEncrypted() { return encrypted; }
    public void setEncrypted(boolean encrypted) { this.encrypted = encrypted; }
}
