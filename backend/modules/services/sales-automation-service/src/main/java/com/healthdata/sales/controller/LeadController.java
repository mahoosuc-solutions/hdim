package com.healthdata.sales.controller;

import com.healthdata.sales.dto.LeadCaptureRequest;
import com.healthdata.sales.dto.LeadConversionRequest;
import com.healthdata.sales.dto.LeadDTO;
import com.healthdata.sales.entity.LeadSource;
import com.healthdata.sales.entity.LeadStatus;
import com.healthdata.sales.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Lead Management Controller
 *
 * REST API for managing sales leads through their lifecycle.
 */
@RestController
@RequestMapping("/api/sales/leads")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Leads",
    description = """
        APIs for managing sales leads through their lifecycle.

        Provides comprehensive lead management including:
        - CRUD operations for leads
        - Lead scoring and qualification
        - Lead conversion to contacts/opportunities
        - Filtering by status and source
        - High-score lead identification

        All endpoints require JWT authentication and X-Tenant-ID header.
        Leads are isolated by tenant for multi-tenancy support.
        """
)
@SecurityRequirement(name = "bearer-jwt")
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    @Operation(
        summary = "List all leads",
        description = """
            Retrieves a paginated list of all leads for the specified tenant.

            Results are sorted by creation date (newest first) by default.
            Use pagination parameters to navigate through large result sets.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved leads",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class)
            )
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    public ResponseEntity<Page<LeadDTO>> findAll(
        @Parameter(description = "Tenant identifier for multi-tenancy isolation", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Pagination parameters (page, size, sort)")
        Pageable pageable
    ) {
        return ResponseEntity.ok(leadService.findAll(tenantId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get lead by ID",
        description = "Retrieves detailed information for a specific lead by its unique identifier."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved lead",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LeadDTO.class)
            )
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Lead not found")
    })
    public ResponseEntity<LeadDTO> findById(
        @Parameter(description = "Tenant identifier for multi-tenancy isolation", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Unique lead identifier", required = true, example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(leadService.findById(tenantId, id));
    }

    @PostMapping
    @Operation(
        summary = "Create a new lead",
        description = """
            Creates a new lead in the sales pipeline.

            The lead will be assigned an initial score based on the provided information
            and source. Required fields include first name, last name, and email.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Lead successfully created",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LeadDTO.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request body - validation failed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
        @ApiResponse(responseCode = "409", description = "Conflict - Lead with same email already exists")
    })
    public ResponseEntity<LeadDTO> create(
        @Parameter(description = "Tenant identifier for multi-tenancy isolation", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Lead details to create", required = true)
        @Valid @RequestBody LeadDTO dto
    ) {
        LeadDTO created = leadService.create(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update an existing lead",
        description = """
            Updates an existing lead's information.

            All fields in the request body will be used to update the lead.
            The lead score may be recalculated based on updated information.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lead successfully updated",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LeadDTO.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid request body - validation failed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Lead not found")
    })
    public ResponseEntity<LeadDTO> update(
        @Parameter(description = "Tenant identifier for multi-tenancy isolation", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Unique lead identifier", required = true, example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        @PathVariable UUID id,
        @Parameter(description = "Updated lead details", required = true)
        @Valid @RequestBody LeadDTO dto
    ) {
        return ResponseEntity.ok(leadService.update(tenantId, id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete a lead",
        description = """
            Permanently deletes a lead from the system.

            This action cannot be undone. Consider converting the lead to a contact
            before deletion if you want to preserve the relationship data.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Lead successfully deleted"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Lead not found")
    })
    public ResponseEntity<Void> delete(
        @Parameter(description = "Tenant identifier for multi-tenancy isolation", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Unique lead identifier", required = true, example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        @PathVariable UUID id
    ) {
        leadService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/convert")
    @Operation(
        summary = "Convert lead to contact and opportunity",
        description = """
            Converts a qualified lead into a contact and optionally creates an opportunity.

            This is a key sales milestone that marks the lead as ready for active engagement.
            The lead status will be updated to CONVERTED and a new contact record will be
            created. If opportunity details are provided, an opportunity will also be created
            and linked to the new contact.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Lead successfully converted",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = LeadDTO.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid conversion request - validation failed or lead not qualified"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions"),
        @ApiResponse(responseCode = "404", description = "Lead not found"),
        @ApiResponse(responseCode = "409", description = "Conflict - Lead has already been converted")
    })
    public ResponseEntity<LeadDTO> convert(
        @Parameter(description = "Tenant identifier for multi-tenancy isolation", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Unique lead identifier", required = true, example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        @PathVariable UUID id,
        @Parameter(description = "Conversion details including contact and optional opportunity information", required = true)
        @Valid @RequestBody LeadConversionRequest request
    ) {
        return ResponseEntity.ok(leadService.convertLead(tenantId, id, request));
    }

    @GetMapping("/status/{status}")
    @Operation(
        summary = "Get leads by status",
        description = """
            Retrieves leads filtered by their current status in the sales pipeline.

            Available statuses: NEW, CONTACTED, QUALIFIED, UNQUALIFIED, CONVERTED, LOST.
            Results are paginated and sorted by last activity date.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved leads by status",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid status value"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    public ResponseEntity<Page<LeadDTO>> findByStatus(
        @Parameter(description = "Tenant identifier for multi-tenancy isolation", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Lead status to filter by", required = true, example = "QUALIFIED",
            schema = @Schema(allowableValues = {"NEW", "CONTACTED", "QUALIFIED", "UNQUALIFIED", "CONVERTED", "LOST"}))
        @PathVariable LeadStatus status,
        @Parameter(description = "Pagination parameters (page, size, sort)")
        Pageable pageable
    ) {
        return ResponseEntity.ok(leadService.findByStatus(tenantId, status, pageable));
    }

    @GetMapping("/source/{source}")
    @Operation(
        summary = "Get leads by source",
        description = """
            Retrieves leads filtered by their acquisition source.

            Available sources: WEBSITE, LINKEDIN, REFERRAL, EVENT, COLD_CALL, EMAIL_CAMPAIGN, OTHER.
            Useful for analyzing lead quality and ROI by acquisition channel.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved leads by source",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid source value"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    public ResponseEntity<Page<LeadDTO>> findBySource(
        @Parameter(description = "Tenant identifier for multi-tenancy isolation", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Lead source to filter by", required = true, example = "LINKEDIN",
            schema = @Schema(allowableValues = {"WEBSITE", "LINKEDIN", "REFERRAL", "EVENT", "COLD_CALL", "EMAIL_CAMPAIGN", "OTHER"}))
        @PathVariable LeadSource source,
        @Parameter(description = "Pagination parameters (page, size, sort)")
        Pageable pageable
    ) {
        return ResponseEntity.ok(leadService.findBySource(tenantId, source, pageable));
    }

    @GetMapping("/high-score")
    @Operation(
        summary = "Get high-scoring leads",
        description = """
            Retrieves leads with a score at or above the specified minimum threshold.

            Lead scores range from 0-100 and are calculated based on factors such as:
            - Profile completeness
            - Engagement level (email opens, website visits)
            - Company fit (industry, size)
            - Decision-maker status

            Default minimum score is 70, which typically indicates a qualified lead.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved high-scoring leads",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Page.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "Invalid minScore value (must be 0-100)"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing JWT token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
    })
    public ResponseEntity<Page<LeadDTO>> findHighScoreLeads(
        @Parameter(description = "Tenant identifier for multi-tenancy isolation", required = true, example = "550e8400-e29b-41d4-a716-446655440000")
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Minimum lead score threshold (0-100)", example = "70",
            schema = @Schema(minimum = "0", maximum = "100", defaultValue = "70"))
        @RequestParam(defaultValue = "70") Integer minScore,
        @Parameter(description = "Pagination parameters (page, size, sort)")
        Pageable pageable
    ) {
        return ResponseEntity.ok(leadService.findHighScoreLeads(tenantId, minScore, pageable));
    }
}
