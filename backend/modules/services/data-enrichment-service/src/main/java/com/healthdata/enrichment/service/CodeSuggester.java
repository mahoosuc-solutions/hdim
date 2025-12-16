package com.healthdata.enrichment.service;

import com.healthdata.enrichment.model.CodeSuggestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CodeSuggester {

    private static final Map<String, List<CodeMapping>> ICD10_MAPPINGS = new HashMap<>();
    private static final Map<String, List<CodeMapping>> CPT_MAPPINGS = new HashMap<>();
    private static final Map<String, List<CodeMapping>> SNOMED_MAPPINGS = new HashMap<>();
    private static final Map<String, List<CodeMapping>> LOINC_MAPPINGS = new HashMap<>();

    static {
        // ICD-10 mappings
        ICD10_MAPPINGS.put("diabetes", List.of(
            new CodeMapping("E11.9", "Type 2 diabetes mellitus without complications", 0.95, true),
            new CodeMapping("E11.65", "Type 2 diabetes mellitus with hyperglycemia", 0.90, true),
            new CodeMapping("E11.2", "Type 2 diabetes mellitus with kidney complications", 0.85, true)
        ));
        // Abbreviations for diabetes
        ICD10_MAPPINGS.put("dm type 2", List.of(
            new CodeMapping("E11.9", "Type 2 diabetes mellitus without complications", 0.95, true)
        ));
        ICD10_MAPPINGS.put("dm2", List.of(
            new CodeMapping("E11.9", "Type 2 diabetes mellitus without complications", 0.95, true)
        ));
        ICD10_MAPPINGS.put("hypertension", List.of(
            new CodeMapping("I10", "Essential (primary) hypertension", 0.95, true)
        ));
        ICD10_MAPPINGS.put("htn", List.of(
            new CodeMapping("I10", "Essential (primary) hypertension", 0.95, true)
        ));
        ICD10_MAPPINGS.put("myocardial infarction", List.of(
            new CodeMapping("I21.9", "Acute myocardial infarction, unspecified", 0.95, true),
            new CodeMapping("I21.4", "Non-ST elevation (NSTEMI) myocardial infarction", 0.90, true)
        ));

        // CPT mappings
        CPT_MAPPINGS.put("office visit", List.of(
            new CodeMapping("99213", "Office visit, established patient, moderate", 0.85, true),
            new CodeMapping("99214", "Office visit, established patient, high", 0.80, true)
        ));

        // SNOMED mappings
        SNOMED_MAPPINGS.put("diabetes", List.of(
            new CodeMapping("73211009", "Diabetes mellitus", 0.95, true)
        ));

        // LOINC mappings
        LOINC_MAPPINGS.put("hemoglobin a1c", List.of(
            new CodeMapping("4548-4", "Hemoglobin A1c/Hemoglobin.total in Blood", 0.98, true)
        ));
    }

    @Cacheable(value = "code-suggestions", key = "'icd10-' + #text")
    public List<CodeSuggestion> suggestIcd10(String text) {
        return suggestIcd10(text, 10);
    }

    public List<CodeSuggestion> suggestIcd10(String text, int maxSuggestions) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedText = text.toLowerCase().trim();
        List<CodeSuggestion> suggestions = new ArrayList<>();

        for (Map.Entry<String, List<CodeMapping>> entry : ICD10_MAPPINGS.entrySet()) {
            if (normalizedText.contains(entry.getKey()) || entry.getKey().contains(normalizedText)) {
                for (CodeMapping mapping : entry.getValue()) {
                    suggestions.add(CodeSuggestion.builder()
                        .code(mapping.code)
                        .description(mapping.description)
                        .codeSystem("ICD10")
                        .confidence(mapping.confidence)
                        .billable(mapping.billable)
                        .build());
                }
            }
        }

        return suggestions.stream()
            .sorted(Comparator.comparingDouble(CodeSuggestion::getConfidence).reversed())
            .limit(maxSuggestions)
            .collect(Collectors.toList());
    }

    public List<CodeSuggestion> suggestCpt(String text) {
        String normalizedText = text.toLowerCase().trim();
        List<CodeSuggestion> suggestions = new ArrayList<>();

        for (Map.Entry<String, List<CodeMapping>> entry : CPT_MAPPINGS.entrySet()) {
            if (normalizedText.contains(entry.getKey())) {
                for (CodeMapping mapping : entry.getValue()) {
                    suggestions.add(CodeSuggestion.builder()
                        .code(mapping.code)
                        .description(mapping.description)
                        .codeSystem("CPT")
                        .confidence(mapping.confidence)
                        .billable(mapping.billable)
                        .build());
                }
            }
        }

        return suggestions.stream()
            .sorted(Comparator.comparingDouble(CodeSuggestion::getConfidence).reversed())
            .collect(Collectors.toList());
    }

    public List<CodeSuggestion> suggestSnomed(String text) {
        String normalizedText = text.toLowerCase().trim();
        List<CodeSuggestion> suggestions = new ArrayList<>();

        for (Map.Entry<String, List<CodeMapping>> entry : SNOMED_MAPPINGS.entrySet()) {
            if (normalizedText.contains(entry.getKey())) {
                for (CodeMapping mapping : entry.getValue()) {
                    suggestions.add(CodeSuggestion.builder()
                        .code(mapping.code)
                        .description(mapping.description)
                        .codeSystem("SNOMED")
                        .confidence(mapping.confidence)
                        .build());
                }
            }
        }

        return suggestions;
    }

    public List<CodeSuggestion> suggestLoinc(String text) {
        String normalizedText = text.toLowerCase().trim();
        List<CodeSuggestion> suggestions = new ArrayList<>();

        for (Map.Entry<String, List<CodeMapping>> entry : LOINC_MAPPINGS.entrySet()) {
            if (normalizedText.contains(entry.getKey())) {
                for (CodeMapping mapping : entry.getValue()) {
                    suggestions.add(CodeSuggestion.builder()
                        .code(mapping.code)
                        .description(mapping.description)
                        .codeSystem("LOINC")
                        .confidence(mapping.confidence)
                        .build());
                }
            }
        }

        return suggestions;
    }

    public List<CodeSuggestion> suggestIcd10WithContext(String text, String context) {
        List<CodeSuggestion> suggestions = suggestIcd10(text);
        // Adjust confidence based on context (simplified)
        if (context.toLowerCase().contains("kidney")) {
            suggestions.forEach(s -> {
                if (s.getCode().contains("E11.2")) {
                    s.setConfidence(Math.min(1.0, s.getConfidence() + 0.1));
                }
            });
        }
        return suggestions.stream()
            .sorted(Comparator.comparingDouble(CodeSuggestion::getConfidence).reversed())
            .collect(Collectors.toList());
    }

    public Map<String, List<CodeSuggestion>> suggestAllCodes(String text) {
        Map<String, List<CodeSuggestion>> allSuggestions = new HashMap<>();
        allSuggestions.put("ICD10", suggestIcd10(text));
        allSuggestions.put("CPT", suggestCpt(text));
        allSuggestions.put("SNOMED", suggestSnomed(text));
        allSuggestions.put("LOINC", suggestLoinc(text));
        return allSuggestions;
    }

    private static class CodeMapping {
        String code;
        String description;
        double confidence;
        boolean billable;

        CodeMapping(String code, String description, double confidence, boolean billable) {
            this.code = code;
            this.description = description;
            this.confidence = confidence;
            this.billable = billable;
        }
    }
}
