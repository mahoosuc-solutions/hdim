package com.healthdata.payer.controller;

import com.healthdata.payer.dto.RoiCalculationRequest;
import com.healthdata.payer.dto.RoiCalculationResponse;
import com.healthdata.payer.service.RoiCalculationService;
import com.healthdata.payer.service.RoiPdfExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<RoiCalculationResponse> getById(@PathVariable String id) {
        return roiCalculationService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'EVALUATOR')")
    @Operation(summary = "List recent calculations — auth required",
               description = "Returns paginated list of recent ROI calculations for the tenant.")
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
    public ResponseEntity<byte[]> downloadPdf(@PathVariable String id) {
        return roiPdfExportService.generatePdf(id)
                .map(bytes -> ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"hdim-roi-report-" + id + ".pdf\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body(bytes))
                .orElse(ResponseEntity.notFound().build());
    }
}
