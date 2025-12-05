package com.healthdata.audit.entity;

import com.healthdata.audit.models.AuditAction;
import com.healthdata.audit.models.AuditOutcome;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity for persisting HIPAA-compliant audit events.
 * Maps to the audit_events table in PostgreSQL.
 *
 * Meets requirements:
 * - 45 CFR § 164.312(b) - Audit Controls
 * - 45 CFR § 164.308(a)(1)(ii)(D) - Information System Activity Review
 * - 7-year retention requirement
 */
@Entity
@Table(name = "audit_events", indexes = {
    @Index(name = "idx_audit_tenant_timestamp", columnList = "tenant_id,timestamp"),
    @Index(name = "idx_audit_user_timestamp", columnList = "user_id,timestamp"),
    @Index(name = "idx_audit_resource", columnList = "resource_type,resource_id,timestamp"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
public class AuditEventEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    // Who - User performing the action
    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(length = 255)
    private String username;

    @Column(length = 100)
    private String role;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // What - Action performed
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private AuditAction action;

    @Column(name = "resource_type", length = 100)
    private String resourceType;

    @Column(name = "resource_id", length = 255)
    private String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private AuditOutcome outcome;

    // Where - System context
    @Column(name = "service_name", length = 100)
    private String serviceName;

    @Column(name = "method_name", length = 100)
    private String methodName;

    @Column(name = "request_path", length = 500)
    private String requestPath;

    // Why - Purpose of use (HIPAA requirement)
    @Column(name = "purpose_of_use", length = 100)
    private String purposeOfUse;

    // Additional context - JSON fields stored as JSONB in PostgreSQL
    @Column(name = "request_payload", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String responsePayload;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "duration_ms")
    private Long durationMs;

    // FHIR AuditEvent reference (optional)
    @Column(name = "fhir_audit_event_id", length = 255)
    private String fhirAuditEventId;

    // Encryption flag
    @Column(nullable = false)
    private boolean encrypted;

    // Constructors
    public AuditEventEntity() {
        this.id = UUID.randomUUID();
        this.timestamp = Instant.now();
        this.encrypted = false;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public AuditOutcome getOutcome() {
        return outcome;
    }

    public void setOutcome(AuditOutcome outcome) {
        this.outcome = outcome;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public String getPurposeOfUse() {
        return purposeOfUse;
    }

    public void setPurposeOfUse(String purposeOfUse) {
        this.purposeOfUse = purposeOfUse;
    }

    public String getRequestPayload() {
        return requestPayload;
    }

    public void setRequestPayload(String requestPayload) {
        this.requestPayload = requestPayload;
    }

    public String getResponsePayload() {
        return responsePayload;
    }

    public void setResponsePayload(String responsePayload) {
        this.responsePayload = responsePayload;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getFhirAuditEventId() {
        return fhirAuditEventId;
    }

    public void setFhirAuditEventId(String fhirAuditEventId) {
        this.fhirAuditEventId = fhirAuditEventId;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }
}
