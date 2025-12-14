package com.healthdata.priorauth.controller;

import com.healthdata.priorauth.dto.PriorAuthRequestDTO;
import com.healthdata.priorauth.persistence.PriorAuthRequestEntity;
import com.healthdata.priorauth.service.PriorAuthService;
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

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for Prior Authorization API.
 *
 * Implements CMS Interoperability and Prior Authorization Rule (CMS-0057-F).
 * Provides endpoints for PA request management and status tracking.
 */
@RestController
@RequestMapping("/api/v1/prior-auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Prior Authorization", description = "Prior Authorization management API")
public class PriorAuthController {

    private final PriorAuthService priorAuthService;

    /**
     * Create a new prior authorization request.
     */
    @PostMapping
    @Operation(summary = "Create PA request", description = "Create a new prior authorization request")
    public ResponseEntity<PriorAuthRequestDTO.Response> createRequest(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody PriorAuthRequestDTO request,
            Principal principal) {

        log.info("Creating PA request for patient: {}", request.getPatientId());

        String requestedBy = principal != null ? principal.getName() : "system";
        PriorAuthRequestDTO.Response response = priorAuthService.createRequest(tenantId, request, requestedBy);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Submit a PA request to the payer.
     */
    @PostMapping("/{requestId}/submit")
    @Operation(summary = "Submit PA request", description = "Submit a PA request to the payer for processing")
    public ResponseEntity<PriorAuthRequestDTO.Response> submitRequest(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID requestId) {

        log.info("Submitting PA request: {}", requestId);

        PriorAuthRequestDTO.Response response = priorAuthService.submitRequest(tenantId, requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get PA request by ID.
     */
    @GetMapping("/{requestId}")
    @Operation(summary = "Get PA request", description = "Get prior authorization request details")
    public ResponseEntity<PriorAuthRequestDTO.Response> getRequest(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID requestId) {

        PriorAuthRequestDTO.Response response = priorAuthService.getRequest(tenantId, requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * Check PA status with payer.
     */
    @PostMapping("/{requestId}/check-status")
    @Operation(summary = "Check PA status", description = "Check current status of PA request with payer")
    public ResponseEntity<PriorAuthRequestDTO.Response> checkStatus(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID requestId) {

        log.info("Checking PA status for request: {}", requestId);

        PriorAuthRequestDTO.Response response = priorAuthService.checkStatus(tenantId, requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a PA request.
     */
    @PostMapping("/{requestId}/cancel")
    @Operation(summary = "Cancel PA request", description = "Cancel a prior authorization request")
    public ResponseEntity<PriorAuthRequestDTO.Response> cancelRequest(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID requestId) {

        log.info("Cancelling PA request: {}", requestId);

        PriorAuthRequestDTO.Response response = priorAuthService.cancelRequest(tenantId, requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get PA requests for a patient.
     */
    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Get patient PA requests", description = "Get all PA requests for a patient")
    public ResponseEntity<Page<PriorAuthRequestDTO.Response>> getPatientRequests(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID patientId,
            Pageable pageable) {

        Page<PriorAuthRequestDTO.Response> response =
            priorAuthService.getPatientRequests(tenantId, patientId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get PA requests by status.
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Get PA requests by status", description = "Get all PA requests with a specific status")
    public ResponseEntity<Page<PriorAuthRequestDTO.Response>> getRequestsByStatus(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable PriorAuthRequestEntity.Status status,
            Pageable pageable) {

        Page<PriorAuthRequestDTO.Response> response =
            priorAuthService.getRequestsByStatus(tenantId, status, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Get PA requests approaching SLA deadline.
     */
    @GetMapping("/sla-alerts")
    @Operation(summary = "Get SLA alerts", description = "Get PA requests approaching their SLA deadline")
    public ResponseEntity<List<PriorAuthRequestDTO.Response>> getSlaAlerts(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(defaultValue = "24") int hoursUntilDeadline) {

        List<PriorAuthRequestDTO.Response> response =
            priorAuthService.getApproachingSlaDeadline(tenantId, hoursUntilDeadline);
        return ResponseEntity.ok(response);
    }

    /**
     * Get PA statistics.
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get PA statistics", description = "Get prior authorization statistics")
    public ResponseEntity<PriorAuthRequestDTO.Statistics> getStatistics(
            @RequestHeader("X-Tenant-Id") String tenantId) {

        PriorAuthRequestDTO.Statistics response = priorAuthService.getStatistics(tenantId);
        return ResponseEntity.ok(response);
    }
}
