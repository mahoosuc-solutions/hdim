package com.healthdata.sales.controller;

import com.healthdata.sales.dto.OpportunityDTO;
import com.healthdata.sales.dto.PipelineMetricsDTO;
import com.healthdata.sales.entity.OpportunityStage;
import com.healthdata.sales.service.OpportunityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

@RestController
@RequestMapping("/api/sales/opportunities")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Opportunities",
    description = """
        APIs for managing sales opportunities (deals).

        Opportunities represent potential revenue from healthcare organizations:
        - Track deal value, probability, and expected close date
        - Manage through pipeline stages (Discovery → Contract → Won/Lost)
        - Associate with accounts, contacts, and activities
        - Support forecasting and pipeline analytics

        Pipeline stages:
        - DISCOVERY: Initial qualification
        - DEMO: Product demonstration scheduled/completed
        - PROPOSAL: Proposal sent
        - NEGOTIATION: Terms being negotiated
        - CONTRACT: Contract review/signing
        - CLOSED_WON: Deal won
        - CLOSED_LOST: Deal lost

        All endpoints require JWT authentication and X-Tenant-ID header.
        """
)
@SecurityRequirement(name = "bearer-jwt")
public class OpportunityController {

    private final OpportunityService opportunityService;

    @GetMapping
    @Operation(
        summary = "List all opportunities",
        description = "Retrieves a paginated list of all opportunities (deals) for the tenant."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved opportunities"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token")
    })
    public ResponseEntity<Page<OpportunityDTO>> findAll(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Pagination parameters (page, size, sort)")
        Pageable pageable
    ) {
        return ResponseEntity.ok(opportunityService.findAll(tenantId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get opportunity by ID",
        description = "Retrieves detailed information about a specific opportunity including associated account and contacts."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved opportunity"),
        @ApiResponse(responseCode = "404", description = "Opportunity not found")
    })
    public ResponseEntity<OpportunityDTO> findById(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Opportunity ID", required = true)
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(opportunityService.findById(tenantId, id));
    }

    @PostMapping
    @Operation(
        summary = "Create a new opportunity",
        description = """
            Creates a new sales opportunity.

            Required fields: name, accountId, value, expectedCloseDate
            Optional: stage (defaults to DISCOVERY), probability, description, contacts
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Opportunity created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation errors"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<OpportunityDTO> create(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Opportunity details", required = true)
        @Valid @RequestBody OpportunityDTO dto
    ) {
        OpportunityDTO created = opportunityService.create(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update an existing opportunity",
        description = "Updates an existing opportunity with new information."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Opportunity updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation errors"),
        @ApiResponse(responseCode = "404", description = "Opportunity not found")
    })
    public ResponseEntity<OpportunityDTO> update(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Opportunity ID", required = true)
        @PathVariable UUID id,
        @Parameter(description = "Updated opportunity details", required = true)
        @Valid @RequestBody OpportunityDTO dto
    ) {
        return ResponseEntity.ok(opportunityService.update(tenantId, id, dto));
    }

    @PatchMapping("/{id}/stage")
    @Operation(
        summary = "Update opportunity stage",
        description = """
            Moves an opportunity to a new pipeline stage.

            Valid transitions follow the sales pipeline flow.
            Moving to CLOSED_WON or CLOSED_LOST will set the actual close date.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stage updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid stage transition"),
        @ApiResponse(responseCode = "404", description = "Opportunity not found")
    })
    public ResponseEntity<OpportunityDTO> updateStage(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Opportunity ID", required = true)
        @PathVariable UUID id,
        @Parameter(description = "New stage", required = true,
            schema = @Schema(allowableValues = {"DISCOVERY", "DEMO", "PROPOSAL", "NEGOTIATION", "CONTRACT", "CLOSED_WON", "CLOSED_LOST"}))
        @RequestParam OpportunityStage stage
    ) {
        return ResponseEntity.ok(opportunityService.updateStage(tenantId, id, stage));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete an opportunity",
        description = "Deletes an opportunity. This will also remove associated activities."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Opportunity deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Opportunity not found")
    })
    public ResponseEntity<Void> delete(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Opportunity ID", required = true)
        @PathVariable UUID id
    ) {
        opportunityService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stage/{stage}")
    @Operation(
        summary = "Get opportunities by stage",
        description = "Retrieves opportunities filtered by their pipeline stage."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved opportunities by stage"),
        @ApiResponse(responseCode = "400", description = "Invalid stage value")
    })
    public ResponseEntity<Page<OpportunityDTO>> findByStage(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Pipeline stage", required = true,
            schema = @Schema(allowableValues = {"DISCOVERY", "DEMO", "PROPOSAL", "NEGOTIATION", "CONTRACT", "CLOSED_WON", "CLOSED_LOST"}))
        @PathVariable OpportunityStage stage,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(opportunityService.findByStage(tenantId, stage, pageable));
    }

    @GetMapping("/open")
    @Operation(
        summary = "Get open opportunities",
        description = "Retrieves all opportunities that are not closed (neither CLOSED_WON nor CLOSED_LOST)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved open opportunities")
    })
    public ResponseEntity<Page<OpportunityDTO>> findOpenOpportunities(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(opportunityService.findOpenOpportunities(tenantId, pageable));
    }

    @GetMapping("/account/{accountId}")
    @Operation(
        summary = "Get opportunities by account",
        description = "Retrieves all opportunities associated with a specific account."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved account opportunities"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<Page<OpportunityDTO>> findByAccount(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Account ID", required = true)
        @PathVariable UUID accountId,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(opportunityService.findByAccount(tenantId, accountId, pageable));
    }
}
