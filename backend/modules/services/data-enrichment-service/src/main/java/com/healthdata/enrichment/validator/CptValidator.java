package com.healthdata.enrichment.validator;

import com.healthdata.enrichment.model.CodeValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Set;

@Component
@Slf4j
public class CptValidator {

    private static final Map<String, String> CPT_CODES = Map.of(
        "99213", "Office visit, established patient, moderate complexity",
        "99214", "Office visit, established patient, high complexity",
        "99215", "Office visit, established patient, very high complexity",
        "99203", "Office visit, new patient, moderate complexity"
    );

    // Deleted CPT codes
    private static final Set<String> DELETED_CODES = Set.of("99999", "00000");

    @Cacheable(value = "cpt-validation", key = "#code")
    public CodeValidationResult validate(String code) {
        if (!code.matches("\\d{5}")) {
            return CodeValidationResult.builder()
                .code(code)
                .valid(false)
                .errors(List.of("Invalid CPT code format (must be 5 digits)"))
                .build();
        }

        // Check for deleted codes
        if (DELETED_CODES.contains(code)) {
            return CodeValidationResult.builder()
                .code(code)
                .valid(false)
                .errors(List.of("CPT code has been deleted"))
                .build();
        }

        boolean exists = CPT_CODES.containsKey(code);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("category", "Evaluation and Management");

        return CodeValidationResult.builder()
            .code(code)
            .valid(exists)
            .description(CPT_CODES.getOrDefault(code, "Unknown procedure"))
            .metadata(metadata)
            .build();
    }

    public CodeValidationResult validateHcpcs(String code) {
        return CodeValidationResult.builder()
            .code(code)
            .valid(code.matches("[A-Z]\\d{4}"))
            .description("HCPCS code")
            .build();
    }

    public boolean isModifierCompatible(String code, String modifier) {
        return true; // Simplified
    }

    public Map<String, Object> getModifierRequirements(String code) {
        return Map.of("required", false);
    }

    public CodeValidationResult validateWithYear(String code, String year) {
        return validate(code);
    }

    public Map<String, Object> getRvuInformation(String code) {
        return Map.of("work", 1.5, "practice", 1.0, "malpractice", 0.5);
    }
}
