package com.healthdata.auditquery.dto;

import com.healthdata.audit.models.AuditAction;
import com.healthdata.audit.models.AuditOutcome;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for audit event data.
 *
 * <p>Excludes sensitive payload data unless user has AUDITOR role with decrypt permission.
 * All timestamp fields are in UTC ISO-8601 format.
 *
 * <p>HIPAA Compliance:
 * <ul>
 *   <li>Request/response payloads are redacted by default (requires explicit decrypt permission)</li>
 *   <li>IP addresses are included for forensic analysis</li>
 *   <li>All PHI access is logged with encrypted payloads</li>
 * </ul>
 */
@Schema(description = "Audit event details")
public record AuditEventResponse(

    @Schema(description = "Unique audit event ID", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID id,

    @Schema(description = "Timestamp of the event (UTC)", example = "2026-01-22T14:30:00Z")
    Instant timestamp,

    @Schema(description = "Tenant ID (multi-tenant isolation)", example = "tenant-001")
    String tenantId,

    @Schema(description = "User ID who performed the action", example = "user-123")
    String userId,

    @Schema(description = "Username", example = "john.doe@example.com")
    String username,

    @Schema(description = "User role at time of action", example = "EVALUATOR")
    String role,

    @Schema(description = "IP address of the user", example = "192.168.1.100")
    String ipAddress,

    @Schema(description = "User agent string", example = "Mozilla/5.0...")
    String userAgent,

    @Schema(description = "Action performed", example = "READ")
    AuditAction action,

    @Schema(description = "Resource type accessed", example = "Patient")
    String resourceType,

    @Schema(description = "Resource ID accessed", example = "pat-456")
    String resourceId,

    @Schema(description = "Outcome of the action", example = "SUCCESS")
    AuditOutcome outcome,

    @Schema(description = "Service name that generated the event", example = "patient-service")
    String serviceName,

    @Schema(description = "Method name", example = "getPatient")
    String methodName,

    @Schema(description = "Request path", example = "/api/v1/patients/pat-456")
    String requestPath,

    @Schema(description = "Purpose of use (HIPAA requirement)", example = "Treatment")
    String purposeOfUse,

    @Schema(description = "Request payload (requires decrypt permission)", example = "{redacted}")
    String requestPayload,

    @Schema(description = "Response payload (requires decrypt permission)", example = "{redacted}")
    String responsePayload,

    @Schema(description = "Error message (if outcome was FAILURE)", example = "Patient not found")
    String errorMessage,

    @Schema(description = "Duration in milliseconds", example = "127")
    Long durationMs,

    @Schema(description = "FHIR AuditEvent resource ID (if applicable)", example = "audit-789")
    String fhirAuditEventId,

    @Schema(description = "Whether payloads are encrypted", example = "true")
    boolean encrypted
) {
}
