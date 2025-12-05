package com.hdim.riskmodels.groupers;

import java.util.*;

/**
 * Clinical Classifications Software (CCS) grouper for ICD-10 codes.
 * Groups diagnoses into clinically meaningful categories.
 */
public class CCSGrouper {

    private final Map<String, String> icd10ToCCS = new HashMap<>();

    public CCSGrouper() {
        initializeMappings();
    }

    public String getCCSCategory(String icd10Code) {
        if (icd10Code == null || icd10Code.trim().isEmpty()) {
            return "Unknown";
        }

        String cleanCode = icd10Code.trim().toUpperCase();
        String category = icd10ToCCS.get(cleanCode);

        if (category != null) {
            return category;
        }

        for (int i = cleanCode.length(); i >= 3; i--) {
            String substring = cleanCode.substring(0, i);
            category = icd10ToCCS.get(substring);
            if (category != null) {
                return category;
            }
        }

        return "Unknown";
    }

    public Map<String, String> groupMultiple(List<String> icd10Codes) {
        Map<String, String> results = new LinkedHashMap<>();
        for (String code : icd10Codes) {
            results.put(code, getCCSCategory(code));
        }
        return results;
    }

    private void initializeMappings() {
        // Cardiovascular
        addRange("I20", "I25", "Coronary Artery Disease");
        addRange("I50", "I50", "Congestive Heart Failure");
        addRange("I60", "I69", "Cerebrovascular Disease");

        // Respiratory
        addRange("J40", "J47", "Chronic Obstructive Pulmonary Disease");
        addRange("J45", "J46", "Asthma");

        // Endocrine
        addRange("E10", "E14", "Diabetes Mellitus");
        addRange("E66", "E66", "Obesity");

        // Renal
        addRange("N18", "N19", "Chronic Kidney Disease");

        // Neoplasms
        addRange("C00", "C97", "Cancer");

        // Mental Health
        addRange("F20", "F29", "Schizophrenia");
        addRange("F32", "F33", "Depression");
    }

    private void addRange(String start, String end, String category) {
        String prefix = start.substring(0, 1);
        int startNum = Integer.parseInt(start.substring(1));
        int endNum = Integer.parseInt(end.substring(1));

        for (int i = startNum; i <= endNum; i++) {
            icd10ToCCS.put(prefix + i, category);
        }
    }
}
