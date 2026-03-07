package com.healthdata.payer.controller;

import com.healthdata.payer.dto.RoiCalculationRequest;
import com.healthdata.payer.dto.RoiCalculationResponse;
import com.healthdata.payer.service.RoiCalculationService;
import com.healthdata.payer.service.RoiPdfExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payer/roi")
@RequiredArgsConstructor
@Tag(name = "ROI Calculator", description = "Calculate and save HDIM ROI projections for sales")
public class RoiCalculatorController {

    private final RoiCalculationService roiCalculationService;
    private final RoiPdfExportService roiPdfExportService;

    @PostMapping("/calculate")
    @Operation(summary = "Calculate ROI — public endpoint for lead capture",
               description = "Runs the ROI calculation and optionally saves results. " +
                             "No authentication required to enable landing page integration.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "ROI calculation completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input (bad org type, out-of-range values)")
    })
    public ResponseEntity<RoiCalculationResponse> calculate(
            @Valid @RequestBody RoiCalculationRequest request,
            @RequestHeader(value = "X-Tenant-ID", required = false) String tenantId) {
        RoiCalculationResponse response = roiCalculationService.calculate(request, tenantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve saved calculation — shareable link",
               description = "Returns a previously saved ROI calculation by ID. " +
                             "Public endpoint for sharing results.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Calculation found"),
            @ApiResponse(responseCode = "404", description = "Calculation not found")
    })
    public ResponseEntity<RoiCalculationResponse> getById(
            @Parameter(description = "Saved calculation UUID") @PathVariable String id) {
        return roiCalculationService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'EVALUATOR')")
    @Operation(summary = "List recent calculations — auth required",
               description = "Returns paginated list of recent ROI calculations for the tenant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated list of recent calculations"),
            @ApiResponse(responseCode = "401", description = "Unauthorized — authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden — insufficient role")
    })
    public ResponseEntity<Page<RoiCalculationResponse>> getRecent(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<RoiCalculationResponse> results = roiCalculationService.getRecent(
                tenantId, PageRequest.of(page, Math.min(size, 100)));
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Download ROI report as PDF",
               description = "Generates a branded PDF report for a saved ROI calculation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF generated successfully"),
            @ApiResponse(responseCode = "404", description = "Calculation not found")
    })
    public ResponseEntity<byte[]> downloadPdf(
            @Parameter(description = "Saved calculation UUID") @PathVariable String id) {
        return roiPdfExportService.generatePdf(id)
                .map(bytes -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"hdim-roi-report-" + id + ".pdf\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(bytes))
                .orElse(ResponseEntity.notFound().build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
