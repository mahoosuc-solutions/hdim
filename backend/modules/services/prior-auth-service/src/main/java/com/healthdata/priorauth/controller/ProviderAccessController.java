package com.healthdata.priorauth.controller;

import com.healthdata.priorauth.service.ProviderAccessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for Provider Access API.
 *
 * Implements CMS Provider Access API requirements, allowing providers to access
 * patient claims, clinical data, and prior authorization information from payers.
 *
 * @see <a href="https://www.cms.gov/regulations-guidance/interoperability">CMS Provider Access API</a>
 */
@RestController
@RequestMapping("/api/v1/provider-access")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Provider Access", description = "Provider Access API for payer data exchange")
public class ProviderAccessController {

    private final ProviderAccessService providerAccessService;

    /**
     * Get patient claims from payer.
     */
    @GetMapping("/claims")
    @Operation(summary = "Get patient claims",
               description = "Retrieve claims history for a patient from the payer")
    public ResponseEntity<Page<Map<String, Object>>> getPatientClaims(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam UUID patientId,
            @RequestParam String payerId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String claimType,
            Pageable pageable) {

        log.info("Fetching claims for patient {} from payer {}", patientId, payerId);

        Page<Map<String, Object>> claims = providerAccessService.getPatientClaims(
            tenantId, patientId, payerId, startDate, endDate, claimType, pageable);

        return ResponseEntity.ok(claims);
    }

    /**
     * Get claim details by ID.
     */
    @GetMapping("/claims/{claimId}")
    @Operation(summary = "Get claim details",
               description = "Retrieve detailed information about a specific claim")
    public ResponseEntity<Map<String, Object>> getClaimDetails(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable String claimId,
            @RequestParam String payerId) {

        log.info("Fetching claim details: {} from payer {}", claimId, payerId);

        Map<String, Object> claim = providerAccessService.getClaimDetails(tenantId, claimId, payerId);
        return ResponseEntity.ok(claim);
    }

    /**
     * Get patient coverage information.
     */
    @GetMapping("/coverage")
    @Operation(summary = "Get patient coverage",
               description = "Retrieve coverage/insurance information for a patient")
    public ResponseEntity<List<Map<String, Object>>> getPatientCoverage(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam UUID patientId,
            @RequestParam String payerId) {

        log.info("Fetching coverage for patient {} from payer {}", patientId, payerId);

        List<Map<String, Object>> coverage = providerAccessService.getPatientCoverage(
            tenantId, patientId, payerId);

        return ResponseEntity.ok(coverage);
    }

    /**
     * Get patient clinical data from payer.
     */
    @GetMapping("/clinical-data")
    @Operation(summary = "Get clinical data",
               description = "Retrieve clinical data (conditions, medications, etc.) from payer")
    public ResponseEntity<Map<String, Object>> getPatientClinicalData(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam UUID patientId,
            @RequestParam String payerId,
            @RequestParam(required = false) List<String> resourceTypes) {

        log.info("Fetching clinical data for patient {} from payer {}", patientId, payerId);

        Map<String, Object> clinicalData = providerAccessService.getPatientClinicalData(
            tenantId, patientId, payerId, resourceTypes);

        return ResponseEntity.ok(clinicalData);
    }

    /**
     * Get prior authorization history from payer.
     */
    @GetMapping("/prior-auth-history")
    @Operation(summary = "Get PA history",
               description = "Retrieve prior authorization history for a patient from payer")
    public ResponseEntity<List<Map<String, Object>>> getPriorAuthHistory(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam UUID patientId,
            @RequestParam String payerId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        log.info("Fetching PA history for patient {} from payer {}", patientId, payerId);

        List<Map<String, Object>> paHistory = providerAccessService.getPriorAuthHistory(
            tenantId, patientId, payerId, startDate, endDate);

        return ResponseEntity.ok(paHistory);
    }

    /**
     * Get Explanation of Benefits (EOB) for a patient.
     */
    @GetMapping("/eob")
    @Operation(summary = "Get EOB",
               description = "Retrieve Explanation of Benefits for a patient from payer")
    public ResponseEntity<Page<Map<String, Object>>> getExplanationOfBenefits(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam UUID patientId,
            @RequestParam String payerId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Pageable pageable) {

        log.info("Fetching EOB for patient {} from payer {}", patientId, payerId);

        Page<Map<String, Object>> eobs = providerAccessService.getExplanationOfBenefits(
            tenantId, patientId, payerId, startDate, endDate, pageable);

        return ResponseEntity.ok(eobs);
    }

    /**
     * Check service coverage eligibility.
     */
    @PostMapping("/eligibility-check")
    @Operation(summary = "Check eligibility",
               description = "Check service coverage eligibility with payer")
    public ResponseEntity<Map<String, Object>> checkEligibility(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestBody EligibilityRequest request) {

        log.info("Checking eligibility for patient {} with payer {}",
            request.patientId, request.payerId);

        Map<String, Object> eligibility = providerAccessService.checkEligibility(
            tenantId, request.patientId, request.payerId,
            request.serviceCode, request.serviceDate);

        return ResponseEntity.ok(eligibility);
    }

    /**
     * Request for eligibility check.
     */
    public record EligibilityRequest(
        UUID patientId,
        String payerId,
        String serviceCode,
        LocalDate serviceDate
    ) {}
}
