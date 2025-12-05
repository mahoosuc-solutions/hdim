package com.healthdata.enrichment.validator;

import com.healthdata.enrichment.model.CodeValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Validate ICD-10-CM/PCS codes.
 */
@Component
@Slf4j
public class ICD10Validator {

    // ICD-10-CM code format: Letter + 2 digits + optional decimal + up to 2 more digits
    private static final Pattern ICD10_CM_PATTERN = Pattern.compile("^[A-Z]\\d{2}(\\.\\d{1,2})?$");
    // ICD-10-PCS code format: 7 alphanumeric characters
    private static final Pattern ICD10_PCS_PATTERN = Pattern.compile("^[0-9A-HJ-NP-Z]{7}$");

    // Simplified code set for demonstration
    private static final Map<String, String> ICD10_CODES = new HashMap<>();

    static {
        // Diabetes codes
        ICD10_CODES.put("E11", "Type 2 diabetes mellitus");
        ICD10_CODES.put("E11.9", "Type 2 diabetes mellitus without complications");
        ICD10_CODES.put("E11.65", "Type 2 diabetes mellitus with hyperglycemia");
        ICD10_CODES.put("E11.6", "Type 2 diabetes mellitus with other specified complication");
        ICD10_CODES.put("E11.2", "Type 2 diabetes mellitus with kidney complications");
        ICD10_CODES.put("E10", "Type 1 diabetes mellitus");
        ICD10_CODES.put("E10.9", "Type 1 diabetes mellitus without complications");

        // Hypertension codes
        ICD10_CODES.put("I10", "Essential (primary) hypertension");
        ICD10_CODES.put("I11", "Hypertensive heart disease");
        ICD10_CODES.put("I12", "Hypertensive chronic kidney disease");

        // Hyperlipidemia codes
        ICD10_CODES.put("E78", "Disorders of lipoprotein metabolism and other lipidemias");
        ICD10_CODES.put("E78.5", "Hyperlipidemia, unspecified");
        ICD10_CODES.put("E78.0", "Pure hypercholesterolemia");

        // Add more common codes...
    }

    // Billable codes (codes with maximum specificity)
    private static final Set<String> BILLABLE_CODES = new HashSet<>(Arrays.asList(
        "E11.9", "E11.65", "E11.2", "E10.9", "I10", "E78.5", "E78.0"
    ));

    /**
     * Validate ICD-10-CM code.
     */
    @Cacheable(value = "icd10-validation", key = "#code")
    public CodeValidationResult validate(String code) {
        if (code == null) {
            throw new IllegalArgumentException("Code cannot be null");
        }

        if (code.isEmpty()) {
            return CodeValidationResult.builder()
                .code(code)
                .valid(false)
                .errors(List.of("Code cannot be empty"))
                .build();
        }

        // Normalize code (uppercase, remove spaces)
        String normalizedCode = code.toUpperCase().trim();

        // Check format
        if (!ICD10_CM_PATTERN.matcher(normalizedCode).matches()) {
            return CodeValidationResult.builder()
                .code(normalizedCode)
                .valid(false)
                .errors(List.of("Invalid ICD-10-CM code format"))
                .suggestions(getSimilarCodes(normalizedCode))
                .build();
        }

        // Check if code exists
        boolean exists = codeExists(normalizedCode);

        if (!exists) {
            return CodeValidationResult.builder()
                .code(normalizedCode)
                .valid(false)
                .errors(List.of("Code not found in ICD-10-CM code set"))
                .suggestions(getSimilarCodes(normalizedCode))
                .build();
        }

        // Valid code
        return CodeValidationResult.builder()
            .code(normalizedCode)
            .valid(true)
            .description(ICD10_CODES.get(normalizedCode))
            .billable(BILLABLE_CODES.contains(normalizedCode))
            .hierarchy(getHierarchy(normalizedCode))
            .version("2024")
            .build();
    }

    /**
     * Validate ICD-10-PCS code.
     */
    public CodeValidationResult validatePCS(String code) {
        if (code == null || code.isEmpty()) {
            return CodeValidationResult.builder()
                .code(code)
                .valid(false)
                .errors(List.of("Code cannot be empty"))
                .build();
        }

        String normalizedCode = code.toUpperCase().trim();

        if (!ICD10_PCS_PATTERN.matcher(normalizedCode).matches()) {
            return CodeValidationResult.builder()
                .code(normalizedCode)
                .valid(false)
                .errors(List.of("Invalid ICD-10-PCS code format"))
                .build();
        }

        // Simplified validation - in production would check against actual code set
        return CodeValidationResult.builder()
            .code(normalizedCode)
            .valid(true)
            .description("ICD-10-PCS procedure code")
            .billable(true)
            .build();
    }

    /**
     * Validate with specific version.
     */
    public CodeValidationResult validateWithVersion(String code, String version) {
        CodeValidationResult result = validate(code);
        result.setVersion(version);
        return result;
    }

    /**
     * Check if code exists.
     */
    public boolean codeExists(String code) {
        return ICD10_CODES.containsKey(code.toUpperCase());
    }

    /**
     * Validate batch of codes.
     */
    public List<CodeValidationResult> validateBatch(String[] codes) {
        List<CodeValidationResult> results = new ArrayList<>();
        for (String code : codes) {
            results.add(validate(code));
        }
        return results;
    }

    /**
     * Get code hierarchy.
     */
    private List<String> getHierarchy(String code) {
        List<String> hierarchy = new ArrayList<>();

        // Add parent codes
        if (code.length() > 3) {
            hierarchy.add(code.substring(0, 3));
        }
        if (code.length() > 5 && code.contains(".")) {
            hierarchy.add(code.substring(0, 5));
        }

        return hierarchy;
    }

    /**
     * Get similar codes for suggestions.
     */
    private List<String> getSimilarCodes(String code) {
        List<String> suggestions = new ArrayList<>();

        // Find codes with same prefix
        String prefix = code.length() >= 3 ? code.substring(0, 3) : code;

        for (String existingCode : ICD10_CODES.keySet()) {
            if (existingCode.startsWith(prefix)) {
                suggestions.add(existingCode);
            }
        }

        return suggestions.subList(0, Math.min(5, suggestions.size()));
    }
}
