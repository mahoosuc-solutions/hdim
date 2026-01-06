package com.healthdata.quality.controller;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.quality.dto.BulkSignRequest;
import com.healthdata.quality.dto.BulkSignResponse;
import com.healthdata.quality.persistence.ResultSignatureEntity;
import com.healthdata.quality.service.ResultSigningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Result Signing Controller - API for signing quality measure results.
 *
 * Provides bulk signing capabilities with safety checks:
 * - Normal results can be signed in bulk
 * - Abnormal results require individual acknowledgment
 * - All operations create immutable audit trail
 *
 * HIPAA Compliance:
 * - @Audited annotation captures all PHI modifications
 * - No caching of signing operations
 * - Immutable signature records for regulatory compliance
 */
@RestController
@RequestMapping("/api/v1/results")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Result Signing", description = "APIs for signing quality measure results")
public class ResultSigningController {

    private final ResultSigningService signingService;

    /**
     * Bulk sign multiple quality measure results.
     *
     * Normal results are signed immediately. Abnormal results (non-compliant with
     * high severity) require explicit acknowledgment via the acknowledgments array.
     *
     * @param tenantId Tenant ID from header
     * @param userId User ID from authentication header
     * @param username Username from authentication header
     * @param request Bulk sign request with result IDs
     * @return BulkSignResponse with counts and pending abnormals
     */
    @Operation(
        summary = "Bulk sign results",
        description = "Sign multiple quality measure results at once. Abnormal results require individual acknowledgment."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bulk sign completed",
                     content = @Content(mediaType = "application/json",
                     schema = @Schema(implementation = BulkSignResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    @PreAuthorize("hasAnyRole('EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = true, includeResponsePayload = true)
    @PostMapping(value = "/bulk-sign", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BulkSignResponse> bulkSignResults(
            @RequestHeader("X-Tenant-ID")
            @NotBlank(message = "Tenant ID is required")
            @Parameter(description = "Tenant ID", required = true)
            String tenantId,

            @RequestHeader("X-Auth-User-Id")
            @NotBlank(message = "User ID is required")
            @Parameter(description = "User ID from authentication", required = true)
            String userId,

            @RequestHeader(value = "X-Auth-Username", defaultValue = "unknown")
            @Parameter(description = "Username from authentication")
            String username,

            @Valid @RequestBody
            @Parameter(description = "Bulk sign request", required = true)
            BulkSignRequest request
    ) {
        log.info("POST /api/v1/results/bulk-sign - {} results by user {} for tenant {}",
                 request.getResultIds().size(), userId, tenantId);

        BulkSignResponse response = signingService.bulkSignResults(
            tenantId, request, userId, username);

        return ResponseEntity.ok(response);
    }

    /**
     * Get signature for a specific result.
     *
     * @param tenantId Tenant ID
     * @param resultId Result UUID
     * @return Signature entity if signed, 404 if not signed
     */
    @Operation(
        summary = "Get result signature",
        description = "Retrieve signature details for a specific result"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Signature found"),
        @ApiResponse(responseCode = "404", description = "Result not signed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PreAuthorize("hasAnyRole('ANALYST', 'EVALUATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/{resultId}/signature", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ResultSignatureEntity> getSignature(
            @RequestHeader("X-Tenant-ID")
            @NotBlank(message = "Tenant ID is required")
            String tenantId,

            @PathVariable
            @Parameter(description = "Result UUID", required = true)
            UUID resultId
    ) {
        log.info("GET /api/v1/results/{}/signature for tenant {}", resultId, tenantId);

        return signingService.getSignature(resultId, tenantId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get signatures by user within a date range.
     *
     * @param tenantId Tenant ID
     * @param userId User ID to filter by
     * @param from Start date/time
     * @param to End date/time
     * @return List of signatures
     */
    @Operation(
        summary = "Get signatures by user",
        description = "Retrieve all signatures made by a specific user within a date range"
    )
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping(value = "/signatures", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ResultSignatureEntity>> getSignaturesByUser(
            @RequestHeader("X-Tenant-ID")
            @NotBlank(message = "Tenant ID is required")
            String tenantId,

            @RequestParam("userId")
            @Parameter(description = "User ID to filter by", required = true)
            String userId,

            @RequestParam("from")
            @Parameter(description = "Start date/time (ISO-8601)", required = true)
            LocalDateTime from,

            @RequestParam("to")
            @Parameter(description = "End date/time (ISO-8601)", required = true)
            LocalDateTime to
    ) {
        log.info("GET /api/v1/results/signatures - user: {}, from: {}, to: {}", userId, from, to);

        List<ResultSignatureEntity> signatures = signingService.getSignaturesByUser(
            tenantId, userId, from, to);

        return ResponseEntity.ok(signatures);
    }
}
