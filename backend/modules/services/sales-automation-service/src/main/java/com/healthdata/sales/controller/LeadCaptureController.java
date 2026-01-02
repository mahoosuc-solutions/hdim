package com.healthdata.sales.controller;

import com.healthdata.sales.dto.LeadCaptureRequest;
import com.healthdata.sales.dto.LeadDTO;
import com.healthdata.sales.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Public endpoint for lead capture from website forms, ROI calculator, etc.
 * This controller does NOT require authentication.
 */
@RestController
@RequestMapping("/api/sales/public")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Lead Capture", description = "Public lead capture endpoints")
public class LeadCaptureController {

    private final LeadService leadService;

    // Default tenant for public captures - should be configured
    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @PostMapping("/capture")
    @Operation(summary = "Capture a new lead", description = "Public endpoint for website forms and integrations")
    public ResponseEntity<LeadCaptureResponse> captureLead(
        @Valid @RequestBody LeadCaptureRequest request,
        @RequestHeader(value = "X-Tenant-ID", required = false) UUID tenantId
    ) {
        UUID effectiveTenantId = tenantId != null ? tenantId : DEFAULT_TENANT_ID;

        try {
            LeadDTO lead = leadService.captureLead(effectiveTenantId, request);
            log.info("Captured lead from source {} for tenant {}",
                request.getSource(), effectiveTenantId);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                LeadCaptureResponse.builder()
                    .success(true)
                    .leadId(lead.getId())
                    .message("Thank you! We'll be in touch shortly.")
                    .build()
            );
        } catch (Exception e) {
            log.error("Failed to capture lead: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                LeadCaptureResponse.builder()
                    .success(false)
                    .message("Something went wrong. Please try again.")
                    .build()
            );
        }
    }

    @PostMapping("/demo-request")
    @Operation(summary = "Request a demo", description = "Capture a demo request lead")
    public ResponseEntity<LeadCaptureResponse> requestDemo(
        @Valid @RequestBody LeadCaptureRequest request,
        @RequestHeader(value = "X-Tenant-ID", required = false) UUID tenantId
    ) {
        // Set source to DEMO_REQUEST
        request.setSource(com.healthdata.sales.entity.LeadSource.DEMO_REQUEST);
        return captureLead(request, tenantId);
    }

    @PostMapping("/roi-calculator")
    @Operation(summary = "ROI calculator submission", description = "Capture lead from ROI calculator")
    public ResponseEntity<LeadCaptureResponse> roiCalculatorSubmission(
        @Valid @RequestBody LeadCaptureRequest request,
        @RequestHeader(value = "X-Tenant-ID", required = false) UUID tenantId
    ) {
        // Set source to ROI_CALCULATOR
        request.setSource(com.healthdata.sales.entity.LeadSource.ROI_CALCULATOR);
        return captureLead(request, tenantId);
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LeadCaptureResponse {
        private boolean success;
        private UUID leadId;
        private String message;
    }
}
