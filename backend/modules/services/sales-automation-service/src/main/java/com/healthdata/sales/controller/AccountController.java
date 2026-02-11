package com.healthdata.sales.controller;

import com.healthdata.sales.dto.AccountDTO;
import com.healthdata.sales.entity.AccountStage;
import com.healthdata.sales.entity.OrganizationType;
import com.healthdata.sales.service.AccountService;
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

@RestController
@RequestMapping("/api/sales/accounts")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Accounts",
    description = """
        APIs for managing healthcare organization accounts.

        Accounts represent healthcare organizations that are potential or existing customers:
        - Healthcare systems and hospital networks
        - Payers and insurance companies
        - ACOs (Accountable Care Organizations)
        - Clinics and medical practices

        Account stages track the sales progression:
        - PROSPECT: Initial target organization
        - QUALIFIED: Verified fit and interest
        - ACTIVE: Engaged in sales process
        - CUSTOMER: Signed contract
        - CHURNED: Former customer

        All endpoints require JWT authentication and X-Tenant-ID header.
        """
)
@SecurityRequirement(name = "bearer-jwt")
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    @Operation(
        summary = "List all accounts",
        description = "Retrieves a paginated list of all healthcare organization accounts for the tenant."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved accounts"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing JWT token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - insufficient permissions")
    })
    public ResponseEntity<Page<AccountDTO>> findAll(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Pagination parameters (page, size, sort)")
        Pageable pageable
    ) {
        return ResponseEntity.ok(accountService.findAll(tenantId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get account by ID",
        description = "Retrieves detailed information about a specific healthcare organization account."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved account"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<AccountDTO> findById(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Account ID", required = true)
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(accountService.findById(tenantId, id));
    }

    @PostMapping
    @Operation(
        summary = "Create a new account",
        description = """
            Creates a new healthcare organization account.

            Required fields: name, organizationType
            Optional fields: website, industry, employeeCount, annualRevenue, address, etc.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Account created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation errors"),
        @ApiResponse(responseCode = "409", description = "Conflict - account with this name already exists")
    })
    public ResponseEntity<AccountDTO> create(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Account details", required = true)
        @Valid @RequestBody AccountDTO dto
    ) {
        AccountDTO created = accountService.create(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Update an existing account",
        description = "Updates an existing healthcare organization account with new information."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation errors"),
        @ApiResponse(responseCode = "404", description = "Account not found"),
        @ApiResponse(responseCode = "409", description = "Conflict - account with this name already exists")
    })
    public ResponseEntity<AccountDTO> update(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Account ID", required = true)
        @PathVariable UUID id,
        @Parameter(description = "Updated account details", required = true)
        @Valid @RequestBody AccountDTO dto
    ) {
        return ResponseEntity.ok(accountService.update(tenantId, id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete an account",
        description = "Deletes a healthcare organization account. This will also remove associated contacts and opportunities."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Account deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Account not found"),
        @ApiResponse(responseCode = "409", description = "Conflict - cannot delete account with active opportunities")
    })
    public ResponseEntity<Void> delete(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Account ID", required = true)
        @PathVariable UUID id
    ) {
        accountService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stage/{stage}")
    @Operation(
        summary = "Get accounts by stage",
        description = "Retrieves accounts filtered by their sales pipeline stage."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved accounts by stage"),
        @ApiResponse(responseCode = "400", description = "Invalid stage value")
    })
    public ResponseEntity<Page<AccountDTO>> findByStage(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Account stage", required = true,
            schema = @Schema(allowableValues = {"PROSPECT", "QUALIFIED", "ACTIVE", "CUSTOMER", "CHURNED"}))
        @PathVariable AccountStage stage,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(accountService.findByStage(tenantId, stage, pageable));
    }

    @GetMapping("/type/{type}")
    @Operation(
        summary = "Get accounts by organization type",
        description = "Retrieves accounts filtered by healthcare organization type."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved accounts by type"),
        @ApiResponse(responseCode = "400", description = "Invalid organization type")
    })
    public ResponseEntity<Page<AccountDTO>> findByOrganizationType(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Organization type", required = true,
            schema = @Schema(allowableValues = {"HEALTH_SYSTEM", "PAYER", "ACO", "CLINIC", "OTHER"}))
        @PathVariable OrganizationType type,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(accountService.findByOrganizationType(tenantId, type, pageable));
    }

    @GetMapping("/search")
    @Operation(
        summary = "Search accounts by name",
        description = "Searches for accounts by name using partial matching (case-insensitive)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully retrieved search results")
    })
    public ResponseEntity<Page<AccountDTO>> search(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Parameter(description = "Search query (partial name match)", required = true, example = "Health")
        @RequestParam String query,
        @Parameter(description = "Pagination parameters")
        Pageable pageable
    ) {
        return ResponseEntity.ok(accountService.search(tenantId, query, pageable));
    }
}
