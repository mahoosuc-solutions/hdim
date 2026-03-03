package com.healthdata.fhir.rest;

import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.auth.context.ScopedTenant;
import com.healthdata.fhir.service.BundleTransactionService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FHIR R4 Bundle Transaction Controller.
 *
 * Accepts FHIR transaction and batch Bundles, routing each entry to the
 * appropriate resource service. Transaction bundles are atomic (all-or-nothing);
 * batch bundles process each entry independently.
 */
@RestController
@RequestMapping(value = "/Bundle", produces = {"application/fhir+json", "application/json"})
@Tag(name = "Bundle", description = "FHIR Bundle transaction and batch processing")
@SecurityRequirement(name = "smart-oauth2")
@Slf4j
public class BundleController {

    private static final String DEFAULT_TENANT = "tenant-1";
    private static final String DEFAULT_ACTOR = "bundle-processor";

    private final BundleTransactionService bundleService;
    private final IParser parser;

    public BundleController(BundleTransactionService bundleService) {
        this.bundleService = bundleService;
        this.parser = FhirContext.forR4().newJsonParser().setPrettyPrint(false);
    }

    @Operation(
        summary = "Process FHIR Bundle",
        description = "Processes a FHIR Bundle with transaction or batch semantics. "
            + "Transaction bundles are atomic — all entries succeed or the entire bundle is rolled back. "
            + "Batch bundles process each entry independently. "
            + "Supported HTTP methods in entries: POST (create), PUT (update), DELETE.",
        operationId = "processBundle"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Bundle processed successfully — returns transaction-response or batch-response Bundle",
            content = @Content(mediaType = "application/fhir+json")
        ),
        @ApiResponse(responseCode = "400", description = "Invalid Bundle format or validation error"),
        @ApiResponse(responseCode = "401", description = "Unauthorized — missing or invalid authentication"),
        @ApiResponse(responseCode = "403", description = "Forbidden — insufficient permissions"),
        @ApiResponse(responseCode = "422", description = "Unprocessable — one or more entries failed (transaction mode)")
    })
    @PostMapping(consumes = {"application/fhir+json", "application/json"})
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    @Audited(
        action = AuditAction.CREATE,
        resourceType = "Bundle",
        purposeOfUse = "TREATMENT",
        description = "Process FHIR Bundle transaction/batch"
    )
    public ResponseEntity<String> processBundle(
            @Parameter(description = "Tenant ID for multi-tenant isolation", example = "tenant-1")
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "FHIR Bundle resource (transaction or batch type) in JSON format",
                required = true,
                content = @Content(mediaType = "application/fhir+json")
            )
            @RequestBody String body) {

        String resolvedTenant = resolveTenant(tenantId);
        String actor = getUserId();

        Bundle bundle = parser.parseResource(Bundle.class, body);
        Bundle result = bundleService.processBundle(resolvedTenant, bundle, actor);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, private")
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(parser.encodeResourceToString(result));
    }

    @ExceptionHandler(BundleTransactionService.BundleValidationException.class)
    public ResponseEntity<String> handleValidation(BundleTransactionService.BundleValidationException ex) {
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.INVALID)
                .setDiagnostics(ex.getMessage());

        return ResponseEntity.badRequest()
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(FhirContext.forR4().newJsonParser().encodeResourceToString(outcome));
    }

    @ExceptionHandler(BundleTransactionService.BundleProcessingException.class)
    public ResponseEntity<String> handleProcessing(BundleTransactionService.BundleProcessingException ex) {
        OperationOutcome outcome = new OperationOutcome();
        outcome.addIssue()
                .setSeverity(OperationOutcome.IssueSeverity.ERROR)
                .setCode(OperationOutcome.IssueType.PROCESSING)
                .setDiagnostics(ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.valueOf("application/fhir+json"))
                .body(FhirContext.forR4().newJsonParser().encodeResourceToString(outcome));
    }

    private String resolveTenant(String tenantId) {
        return ScopedTenant.currentTenant().orElseGet(() ->
                (tenantId == null || tenantId.isBlank()) ? DEFAULT_TENANT : tenantId);
    }

    private String getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : DEFAULT_ACTOR;
    }
}
