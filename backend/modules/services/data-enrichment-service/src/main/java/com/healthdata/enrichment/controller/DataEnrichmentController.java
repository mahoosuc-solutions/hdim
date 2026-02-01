package com.healthdata.enrichment.controller;

import com.healthdata.enrichment.analyzer.DataCompletenessAnalyzer;
import com.healthdata.enrichment.dto.*;
import com.healthdata.enrichment.model.*;
import com.healthdata.enrichment.service.*;
import com.healthdata.enrichment.validator.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for AI Data Enrichment Service.
 *
 * Provides endpoints for:
 * - Clinical note extraction
 * - Code validation (ICD-10, SNOMED, CPT, LOINC)
 * - Code suggestions
 * - Data completeness analysis
 * - Data quality assessment
 */
@RestController
@RequestMapping("/api/v1/enrichment")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Data Enrichment", description = "AI-powered data enrichment and quality assessment")
public class DataEnrichmentController {

    private final ClinicalNoteExtractor clinicalNoteExtractor;
    private final MedicalEntityRecognizer medicalEntityRecognizer;
    private final ICD10Validator icd10Validator;
    private final SnomedValidator snomedValidator;
    private final CptValidator cptValidator;
    private final LoincValidator loincValidator;
    private final CodeSuggester codeSuggester;
    private final DataCompletenessAnalyzer completenessAnalyzer;
    private final DataQualityService qualityService;

    @PostMapping("/extract")
    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Operation(summary = "Extract entities from clinical notes")
    public ResponseEntity<ExtractionResult> extractFromClinicalNote(
            @Valid @RequestBody ExtractionRequest request) {
        log.info("Extracting entities from clinical note");

        if (request.isAsync()) {
            // Return task ID for async processing
            String taskId = UUID.randomUUID().toString();
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ExtractionResult.builder()
                    .id(taskId)
                    .taskId(taskId)
                    .build());
        }

        ExtractionResult result;
        if (request.getTenantId() != null) {
            result = clinicalNoteExtractor.extractWithTenant(
                request.getClinicalNote(),
                request.getTenantId()
            );
        } else {
            result = clinicalNoteExtractor.extract(request.getClinicalNote());
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/validate/icd10")
    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Operation(summary = "Validate ICD-10-CM code")
    public ResponseEntity<CodeValidationResult> validateIcd10Code(
            @Valid @RequestBody CodeValidationRequest request) {
        log.info("Validating ICD-10 code: {}", request.getCode());

        CodeValidationResult result;
        if (request.getVersion() != null) {
            result = icd10Validator.validateWithVersion(request.getCode(), request.getVersion());
        } else {
            result = icd10Validator.validate(request.getCode());
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/validate/snomed")
    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Operation(summary = "Validate SNOMED CT code")
    public ResponseEntity<CodeValidationResult> validateSnomedCode(
            @Valid @RequestBody CodeValidationRequest request) {
        log.info("Validating SNOMED code: {}", request.getCode());

        CodeValidationResult result;
        if (request.getEffectiveDate() != null) {
            result = snomedValidator.validateWithEffectiveDate(
                request.getCode(),
                request.getEffectiveDate()
            );
        } else {
            result = snomedValidator.validate(request.getCode());
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/validate/cpt")
    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Operation(summary = "Validate CPT code")
    public ResponseEntity<CodeValidationResult> validateCptCode(
            @Valid @RequestBody CodeValidationRequest request) {
        log.info("Validating CPT code: {}", request.getCode());

        CodeValidationResult result = cptValidator.validate(request.getCode());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/validate/loinc")
    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Operation(summary = "Validate LOINC code")
    public ResponseEntity<CodeValidationResult> validateLoincCode(
            @Valid @RequestBody CodeValidationRequest request) {
        log.info("Validating LOINC code: {}", request.getCode());

        CodeValidationResult result = loincValidator.validate(request.getCode());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/suggest-codes")
    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Operation(summary = "Suggest codes from text")
    public ResponseEntity<List<CodeSuggestion>> suggestCodes(
            @Valid @RequestBody CodeSuggestionRequest request) {
        log.info("Suggesting codes for text: {}, system: {}",
            request.getText(), request.getCodeSystem());

        List<CodeSuggestion> suggestions;

        switch (request.getCodeSystem().toUpperCase()) {
            case "ICD10":
                if (request.getContext() != null) {
                    suggestions = codeSuggester.suggestIcd10WithContext(
                        request.getText(),
                        request.getContext()
                    );
                } else {
                    suggestions = codeSuggester.suggestIcd10(
                        request.getText(),
                        request.getMaxSuggestions()
                    );
                }
                break;
            case "CPT":
                suggestions = codeSuggester.suggestCpt(request.getText());
                break;
            case "SNOMED":
                suggestions = codeSuggester.suggestSnomed(request.getText());
                break;
            case "LOINC":
                suggestions = codeSuggester.suggestLoinc(request.getText());
                break;
            default:
                return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/completeness/{patientId}")
    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Operation(summary = "Analyze data completeness for patient")
    public ResponseEntity<MissingDataReport> getCompletenessAnalysis(
            @PathVariable String patientId) {
        log.info("Analyzing data completeness for patient: {}", patientId);

        MissingDataReport report = completenessAnalyzer.analyze(patientId);

        if (report == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(report);
    }

    @GetMapping("/quality/report")
    @PreAuthorize("hasPermission('PATIENT_WRITE')")
    @Operation(summary = "Generate data quality report")
    public ResponseEntity<DataQualityReport> getQualityReport(
            @RequestParam String patientId) {
        log.info("Generating data quality report for patient: {}", patientId);

        DataQualityReport report = qualityService.generateQualityReport(patientId);
        return ResponseEntity.ok(report);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Invalid argument: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage());
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Validation failed");
        error.put("details", ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .toList());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception ex) {
        log.error("Error processing request", ex);
        Map<String, String> error = new HashMap<>();
        error.put("error", "Internal server error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
