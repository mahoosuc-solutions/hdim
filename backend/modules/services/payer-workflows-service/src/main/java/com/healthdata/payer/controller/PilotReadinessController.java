package com.healthdata.payer.controller;

import com.healthdata.payer.dto.PilotOnboardRequest;
import com.healthdata.payer.dto.PilotReadinessResponse;
import com.healthdata.payer.service.PilotReadinessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pilot")
@RequiredArgsConstructor
@Tag(name = "Pilot Readiness", description = "Manage pilot customer onboarding and readiness")
public class PilotReadinessController {

    private final PilotReadinessService pilotReadinessService;

    @PostMapping("/onboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Initialize new pilot customer",
               description = "Creates a new pilot readiness record for customer onboarding.")
    public ResponseEntity<PilotReadinessResponse> onboard(
            @Valid @RequestBody PilotOnboardRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(pilotReadinessService.onboard(request, tenantId));
    }

    @GetMapping("/readiness/{customerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'EVALUATOR')")
    @Operation(summary = "Check readiness score for a customer")
    public ResponseEntity<PilotReadinessResponse> getReadiness(
            @PathVariable String customerId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return pilotReadinessService.getReadiness(customerId, tenantId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/readiness")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'EVALUATOR')")
    @Operation(summary = "List all pilot customers with readiness scores")
    public ResponseEntity<List<PilotReadinessResponse>> listAll(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(pilotReadinessService.listAll(tenantId));
    }

    @PostMapping("/validate-integration")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Validate EHR/FHIR connectivity",
               description = "Tests FHIR endpoint reachability with 5-second timeout. " +
                             "Updates integration status and readiness score.")
    public ResponseEntity<PilotReadinessResponse> validateIntegration(
            @RequestParam String customerId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        return ResponseEntity.ok(pilotReadinessService.validateIntegration(customerId, tenantId));
    }
}
