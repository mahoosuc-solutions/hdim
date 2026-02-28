package com.healthdata.payer.controller;

import com.healthdata.payer.revenue.dto.*;
import com.healthdata.payer.service.RevenueContractService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/revenue")
@RequiredArgsConstructor
@Tag(name = "Revenue Contracts", description = "Wave-1 revenue cycle transaction backbone contract APIs")
public class RevenueContractController {

    private final RevenueContractService revenueContractService;

    @PostMapping("/claims/submissions")
    @Operation(summary = "Submit a claim transaction")
    public ResponseEntity<ClaimSubmissionResponse> submitClaim(
            @Valid @RequestBody ClaimSubmissionRequest request
    ) {
        return ResponseEntity.ok(revenueContractService.submitClaim(request));
    }

    @PostMapping("/eligibility/checks")
    @Operation(summary = "Check patient eligibility")
    public ResponseEntity<EligibilityCheckResponse> checkEligibility(
            @Valid @RequestBody EligibilityCheckRequest request
    ) {
        return ResponseEntity.ok(revenueContractService.checkEligibility(request));
    }

    @PostMapping("/claim-status/checks")
    @Operation(summary = "Check claim status")
    public ResponseEntity<ClaimStatusResponse> checkClaimStatus(
            @Valid @RequestBody ClaimStatusRequest request
    ) {
        return ResponseEntity.ok(revenueContractService.checkClaimStatus(request));
    }

    @PostMapping("/remittance/advice")
    @Operation(summary = "Ingest remittance advice and return reconciliation preview")
    public ResponseEntity<ReconciliationPreviewResponse> ingestRemittanceAdvice(
            @Valid @RequestBody RemittanceAdviceEvent request
    ) {
        return ResponseEntity.ok(revenueContractService.ingestRemittanceAdvice(request));
    }

    @GetMapping("/audit/{correlationId}")
    @Operation(summary = "Get revenue audit envelopes by correlation id")
    public ResponseEntity<List<RevenueAuditEnvelope>> getAuditTrail(@PathVariable String correlationId) {
        return ResponseEntity.ok(revenueContractService.getAuditTrail(correlationId));
    }

    @PostMapping("/price-transparency/rates/publish")
    @Operation(summary = "Publish machine-readable negotiated rates")
    public ResponseEntity<PriceTransparencyRatePublishResponse> publishPriceTransparencyRates(
            @Valid @RequestBody PriceTransparencyRatePublishRequest request
    ) {
        return ResponseEntity.ok(revenueContractService.publishPriceTransparencyRates(request));
    }

    @GetMapping("/price-transparency/rates/current")
    @Operation(summary = "Get current price transparency version for tenant")
    public ResponseEntity<PriceTransparencyRatesViewResponse> getCurrentPriceTransparencyRates(
            @RequestParam String tenantId,
            @RequestParam String correlationId,
            @RequestParam String actor
    ) {
        PriceTransparencyRatesViewResponse response = revenueContractService.getCurrentPriceTransparencyRates(
                tenantId,
                correlationId,
                actor
        );
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/price-transparency/rates/{versionId}")
    @Operation(summary = "Get a specific price transparency rate version")
    public ResponseEntity<PriceTransparencyRatesViewResponse> getPriceTransparencyRatesVersion(
            @PathVariable String versionId,
            @RequestParam String tenantId,
            @RequestParam String correlationId,
            @RequestParam String actor
    ) {
        PriceTransparencyRatesViewResponse response = revenueContractService.getPriceTransparencyRatesVersion(
                tenantId,
                versionId,
                correlationId,
                actor
        );
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/price-transparency/estimates")
    @Operation(summary = "Calculate deterministic price estimate from published rates")
    public ResponseEntity<PriceEstimateResponse> estimatePrice(
            @Valid @RequestBody PriceEstimateRequest request
    ) {
        if (request.getVersionId() != null
                && !request.getVersionId().isBlank()
                && !revenueContractService.hasPriceTransparencyVersion(request.getTenantId(), request.getVersionId())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(revenueContractService.estimatePrice(request));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
