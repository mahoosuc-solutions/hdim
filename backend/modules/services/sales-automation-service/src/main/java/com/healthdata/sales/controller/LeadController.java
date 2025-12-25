package com.healthdata.sales.controller;

import com.healthdata.sales.dto.LeadCaptureRequest;
import com.healthdata.sales.dto.LeadConversionRequest;
import com.healthdata.sales.dto.LeadDTO;
import com.healthdata.sales.entity.LeadSource;
import com.healthdata.sales.entity.LeadStatus;
import com.healthdata.sales.service.LeadService;
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
@RequestMapping("/api/sales/leads")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Leads", description = "Lead management endpoints")
public class LeadController {

    private final LeadService leadService;

    @GetMapping
    @Operation(summary = "List all leads", description = "Get paginated list of leads")
    public ResponseEntity<Page<LeadDTO>> findAll(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        Pageable pageable
    ) {
        return ResponseEntity.ok(leadService.findAll(tenantId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get lead by ID")
    public ResponseEntity<LeadDTO> findById(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id
    ) {
        return ResponseEntity.ok(leadService.findById(tenantId, id));
    }

    @PostMapping
    @Operation(summary = "Create a new lead")
    public ResponseEntity<LeadDTO> create(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @Valid @RequestBody LeadDTO dto
    ) {
        LeadDTO created = leadService.create(tenantId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing lead")
    public ResponseEntity<LeadDTO> update(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id,
        @Valid @RequestBody LeadDTO dto
    ) {
        return ResponseEntity.ok(leadService.update(tenantId, id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a lead")
    public ResponseEntity<Void> delete(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id
    ) {
        leadService.delete(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/convert")
    @Operation(summary = "Convert lead to contact and opportunity")
    public ResponseEntity<LeadDTO> convert(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable UUID id,
        @Valid @RequestBody LeadConversionRequest request
    ) {
        return ResponseEntity.ok(leadService.convertLead(tenantId, id, request));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get leads by status")
    public ResponseEntity<Page<LeadDTO>> findByStatus(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable LeadStatus status,
        Pageable pageable
    ) {
        return ResponseEntity.ok(leadService.findByStatus(tenantId, status, pageable));
    }

    @GetMapping("/source/{source}")
    @Operation(summary = "Get leads by source")
    public ResponseEntity<Page<LeadDTO>> findBySource(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @PathVariable LeadSource source,
        Pageable pageable
    ) {
        return ResponseEntity.ok(leadService.findBySource(tenantId, source, pageable));
    }

    @GetMapping("/high-score")
    @Operation(summary = "Get high-scoring leads")
    public ResponseEntity<Page<LeadDTO>> findHighScoreLeads(
        @RequestHeader("X-Tenant-ID") UUID tenantId,
        @RequestParam(defaultValue = "70") Integer minScore,
        Pageable pageable
    ) {
        return ResponseEntity.ok(leadService.findHighScoreLeads(tenantId, minScore, pageable));
    }
}
