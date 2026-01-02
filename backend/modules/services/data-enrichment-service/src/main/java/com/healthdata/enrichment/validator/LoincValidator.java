package com.healthdata.enrichment.validator;

import com.healthdata.enrichment.model.CodeValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class LoincValidator {

    private static final Map<String, String> LOINC_CODES = Map.of(
        "4548-4", "Hemoglobin A1c/Hemoglobin.total in Blood",
        "2345-7", "Glucose [Mass/volume] in Serum or Plasma",
        "2160-0", "Creatinine [Mass/volume] in Serum or Plasma"
    );

    @Cacheable(value = "loinc-validation", key = "#code")
    public CodeValidationResult validate(String code) {
        if (!code.matches("\\d+-\\d")) {
            return CodeValidationResult.builder()
                .code(code)
                .valid(false)
                .errors(List.of("Invalid LOINC code format"))
                .build();
        }

        boolean exists = LOINC_CODES.containsKey(code);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("component", "Hemoglobin A1c");
        metadata.put("system", "Blood");
        metadata.put("scaleType", "Qn");

        return CodeValidationResult.builder()
            .code(code)
            .valid(exists)
            .description(LOINC_CODES.getOrDefault(code, "Unknown test"))
            .metadata(metadata)
            .warnings(exists ? List.of() : List.of())
            .build();
    }

    public Map<String, Object> getReferenceRange(String code) {
        return Map.of("low", 4.0, "high", 5.6, "unit", "%");
    }

    public List<String> getUnitsOfMeasure(String code) {
        return List.of("%", "mmol/mol");
    }

    public List<String> getRelatedCodes(String code) {
        return List.of("17856-6", "41995-2");
    }
}
