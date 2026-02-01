package com.healthdata.auditquery.dto;

import com.healthdata.audit.models.AuditAction;
import com.healthdata.audit.models.AuditOutcome;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

import java.time.Instant;

/**
 * Request DTO for complex audit log searches with multiple combinable criteria.
 *
 * <p>All filters are optional and combined with AND logic. Supports:
 * <ul>
 *   <li>User-based filtering (userId, username, role)</li>
 *   <li>Resource-based filtering (resourceType, resourceId)</li>
 *   <li>Action-based filtering (action, outcome)</li>
 *   <li>Time-based filtering (startTime, endTime)</li>
 *   <li>Service-based filtering (serviceName)</li>
 * </ul>
 *
 * <p>Example: Find all failed patient access attempts by a specific user in the last 7 days.
 */
@Schema(description = "Multi-criteria audit log search request")
public record AuditSearchRequest(

    @Schema(description = "Filter by user ID", example = "user-123")
    String userId,

    @Schema(description = "Filter by username (partial match supported)", example = "john.doe")
    String username,

    @Schema(description = "Filter by user role", example = "EVALUATOR")
    String role,

    @Schema(description = "Filter by resource type", example = "Patient")
    String resourceType,

    @Schema(description = "Filter by resource ID", example = "pat-456")
    String resourceId,

    @Schema(description = "Filter by audit action", example = "READ")
    AuditAction action,

    @Schema(description = "Filter by outcome", example = "SUCCESS")
    AuditOutcome outcome,

    @Schema(description = "Filter by service name", example = "patient-service")
    String serviceName,

    @Schema(description = "Filter by IP address", example = "192.168.1.100")
    String ipAddress,

    @Schema(description = "Start of time range (inclusive)", example = "2026-01-15T00:00:00Z")
    Instant startTime,

    @Schema(description = "End of time range (inclusive)", example = "2026-01-22T23:59:59Z")
    Instant endTime,

    @Schema(description = "Page number (0-indexed)", example = "0", defaultValue = "0")
    @Min(0)
    Integer page,

    @Schema(description = "Page size", example = "20", defaultValue = "20")
    @Min(1)
    Integer size,

    @Schema(description = "Sort field", example = "timestamp", defaultValue = "timestamp")
    String sortBy,

    @Schema(description = "Sort direction (ASC or DESC)", example = "DESC", defaultValue = "DESC")
    String sortDirection
) {

    /**
     * Constructor with defaults for pagination and sorting.
     */
    public AuditSearchRequest {
        if (page == null) page = 0;
        if (size == null) size = 20;
        if (sortBy == null) sortBy = "timestamp";
        if (sortDirection == null) sortDirection = "DESC";

        // Limit page size to prevent memory issues
        if (size > 1000) size = 1000;
    }

    /**
     * Check if any search criteria are specified.
     * @return true if at least one filter is non-null
     */
    public boolean hasFilters() {
        return userId != null
            || username != null
            || role != null
            || resourceType != null
            || resourceId != null
            || action != null
            || outcome != null
            || serviceName != null
            || ipAddress != null
            || startTime != null
            || endTime != null;
    }
}
