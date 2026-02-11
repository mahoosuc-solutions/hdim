package com.healthdata.sales.controller;

import com.healthdata.sales.dto.LeadCaptureRequest;
import com.healthdata.sales.dto.LeadDTO;
import com.healthdata.sales.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(
    name = "Lead Capture",
    description = """
        Public APIs for capturing leads from external sources.

        These endpoints do NOT require authentication and are designed for:
        - Website contact forms
        - Demo request forms
        - ROI calculator submissions
        - Landing page conversions

        Leads are automatically:
        - Created in the sales system
        - Tagged with their source
        - Assigned based on tenant rules
        - Enrolled in nurture sequences (if configured)

        CORS is enabled for cross-origin requests from marketing websites.
        """
)
public class LeadCaptureController {

    private final LeadService leadService;

    // Default tenant for public captures - should be configured
    private static final UUID DEFAULT_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @PostMapping("/capture")
    @Operation(
        summary = "Capture a new lead",
        description = """
            Public endpoint for capturing leads from website forms and integrations.

            Required fields: firstName, lastName, email
            Optional: company, phone, title, source, message, utmParams

            If X-Tenant-ID is not provided, uses the default tenant.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Lead captured successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation errors"),
        @ApiResponse(responseCode = "500", description = "Server error - lead not captured")
    })
    public ResponseEntity<LeadCaptureResponse> captureLead(
        @Parameter(description = "Lead information", required = true)
        @Valid @RequestBody LeadCaptureRequest request,
        @Parameter(description = "Optional tenant identifier (uses default if not provided)")
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
    @Operation(
        summary = "Request a demo",
        description = """
            Captures a demo request lead with DEMO_REQUEST source.

            Use this endpoint for dedicated demo request forms.
            The lead will be prioritized and typically assigned to a sales rep immediately.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Demo request captured successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation errors")
    })
    public ResponseEntity<LeadCaptureResponse> requestDemo(
        @Parameter(description = "Demo request information", required = true)
        @Valid @RequestBody LeadCaptureRequest request,
        @Parameter(description = "Optional tenant identifier")
        @RequestHeader(value = "X-Tenant-ID", required = false) UUID tenantId
    ) {
        // Set source to DEMO_REQUEST
        request.setSource(com.healthdata.sales.entity.LeadSource.DEMO_REQUEST);
        return captureLead(request, tenantId);
    }

    @PostMapping("/roi-calculator")
    @Operation(
        summary = "ROI calculator submission",
        description = """
            Captures a lead from the ROI calculator tool.

            Leads from the ROI calculator are high-intent and include
            calculated ROI data in the request metadata.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "ROI calculator submission captured successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - validation errors")
    })
    public ResponseEntity<LeadCaptureResponse> roiCalculatorSubmission(
        @Parameter(description = "ROI calculator submission information", required = true)
        @Valid @RequestBody LeadCaptureRequest request,
        @Parameter(description = "Optional tenant identifier")
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
