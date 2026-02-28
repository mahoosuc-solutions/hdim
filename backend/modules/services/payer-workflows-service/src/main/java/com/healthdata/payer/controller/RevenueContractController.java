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

    @GetMapping("/audit/{correlationId}")
    @Operation(summary = "Get revenue audit envelopes by correlation id")
    public ResponseEntity<List<RevenueAuditEnvelope>> getAuditTrail(@PathVariable String correlationId) {
        return ResponseEntity.ok(revenueContractService.getAuditTrail(correlationId));
    }
}
