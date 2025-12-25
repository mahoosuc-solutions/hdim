package com.healthdata.sales.controller;

import com.healthdata.sales.dto.OpportunityDTO;
import com.healthdata.sales.dto.PipelineMetricsDTO;
import com.healthdata.sales.entity.OpportunityStage;
import com.healthdata.sales.service.OpportunityService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "Opportunities", description = "Sales opportunity/deal management")
public class OpportunityController {

    private final OpportunityService opportunityService;

    @GetMapping
    @Operation(summary = "List all opportunities", description = "Get paginated list of opportunities")
    public ResponseEntity<Page<OpportunityDTO>> findAll(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(opportunityService.findAll(tenantId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get opportunity by ID")
    public ResponseEntity<OpportunityDTO> findById(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(opportunityService.findById(tenantId, id));
    }

    @PostMapping
    @Operation(summary = "Create a new opportunity")
    public ResponseEntity<OpportunityDTO> create(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Valid @RequestBody OpportunityDTO dto
    ) {
        OpportunityDTO created = opportunityService.create(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing opportunity")
    public ResponseEntity<OpportunityDTO> update(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id,
        @Valid @RequestBody OpportunityDTO dto
    ) {
        return ResponseEntity.ok(opportunityService.update(tenantId, id, dto));
    }

    @PatchMapping("/{id}/stage")
    @Operation(summary = "Update opportunity stage", description = "Move opportunity to a new stage")
    public ResponseEntity<OpportunityDTO> updateStage(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id,
        @RequestParam OpportunityStage stage
    ) {
        return ResponseEntity.ok(opportunityService.updateStage(tenantId, id, stage));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an opportunity")
    public ResponseEntity<Void> delete(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id
    ) {
        opportunityService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stage/{stage}")
    @Operation(summary = "Get opportunities by stage")
    public ResponseEntity<Page<OpportunityDTO>> findByStage(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable OpportunityStage stage,
        Pageable pageable
    ) {
        return ResponseEntity.ok(opportunityService.findByStage(tenantId, stage, pageable));
    }

    @GetMapping("/open")
    @Operation(summary = "Get open opportunities", description = "Opportunities not closed-won or closed-lost")
    public ResponseEntity<Page<OpportunityDTO>> findOpenOpportunities(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(opportunityService.findOpenOpportunities(tenantId, pageable));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get opportunities by account")
    public ResponseEntity<Page<OpportunityDTO>> findByAccount(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID accountId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(opportunityService.findByAccount(tenantId, accountId, pageable));
    }
}
