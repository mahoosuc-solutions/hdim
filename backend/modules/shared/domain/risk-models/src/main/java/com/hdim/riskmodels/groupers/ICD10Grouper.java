package com.hdim.riskmodels.groupers;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Groups ICD-10-CM codes into major disease categories.
 * Thread-safe implementation with caching.
 */
public class ICD10Grouper {

    private static final Map<String, String> CHAPTER_RANGES;

    static {
        Map<String, String> ranges = new LinkedHashMap<>();
        ranges.put("A00-B99", "Infectious and Parasitic Diseases");
        ranges.put("C00-D49", "Neoplasms");
        ranges.put("D50-D89", "Blood and Immune Disorders");
        ranges.put("E00-E89", "Endocrine, Nutritional and Metabolic Diseases");
        ranges.put("F01-F99", "Mental and Behavioral Disorders");
        ranges.put("G00-G99", "Nervous System Diseases");
        ranges.put("H00-H59", "Eye and Adnexa Diseases");
        ranges.put("H60-H95", "Ear and Mastoid Diseases");
        ranges.put("I00-I99", "Circulatory System Diseases");
        ranges.put("J00-J99", "Respiratory System Diseases");
        ranges.put("K00-K95", "Digestive System Diseases");
        ranges.put("L00-L99", "Skin and Subcutaneous Tissue Diseases");
        ranges.put("M00-M99", "Musculoskeletal Diseases");
        ranges.put("N00-N99", "Genitourinary System Diseases");
        ranges.put("O00-O9A", "Pregnancy and Childbirth");
        ranges.put("P00-P96", "Perinatal Conditions");
        ranges.put("Q00-Q99", "Congenital Malformations");
        ranges.put("R00-R99", "Symptoms and Signs");
        ranges.put("S00-T88", "Injury and Poisoning");
        ranges.put("V00-Y99", "External Causes");
        ranges.put("Z00-Z99", "Health Services Encounters");
        CHAPTER_RANGES = Collections.unmodifiableMap(ranges);
    }

    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String getCategory(String icd10Code) {
        Objects.requireNonNull(icd10Code, "ICD-10 code cannot be null");

        if (icd10Code.trim().isEmpty()) {
            return "Unknown";
        }

        return cache.computeIfAbsent(icd10Code, this::computeCategory);
    }

    public String getSubcategory(String icd10Code) {
        Objects.requireNonNull(icd10Code, "ICD-10 code cannot be null");

        String cleanCode = icd10Code.trim().toUpperCase();
        if (cleanCode.isEmpty() || cleanCode.length() < 3) {
            return "Unknown";
        }

        String prefix = cleanCode.substring(0, 3);

        // Diabetes
        if (prefix.startsWith("E1")) {
            return "Diabetes Mellitus";
        }

        // Heart disease
        if (prefix.startsWith("I2") || prefix.startsWith("I5")) {
            return "Heart Disease";
        }

        // COPD
        if (prefix.startsWith("J4")) {
            return "Chronic Lower Respiratory Disease";
        }

        // CKD
        if (prefix.startsWith("N18")) {
            return "Chronic Kidney Disease";
        }

        return "Other";
    }

    public Map<String, String> groupMultiple(List<String> icd10Codes) {
        Map<String, String> results = new LinkedHashMap<>();
        for (String code : icd10Codes) {
            results.put(code, getCategory(code));
        }
        return results;
    }

    private String computeCategory(String icd10Code) {
        String cleanCode = icd10Code.trim().toUpperCase();

        if (cleanCode.isEmpty()) {
            return "Unknown";
        }

        char firstChar = cleanCode.charAt(0);

        for (Map.Entry<String, String> entry : CHAPTER_RANGES.entrySet()) {
            String range = entry.getKey();
            String[] parts = range.split("-");
            String start = parts[0];
            String end = parts[1];

            if (isInRange(cleanCode, start, end)) {
                return entry.getValue();
            }
        }

        return "Unknown";
    }

    private boolean isInRange(String code, String start, String end) {
        if (code.isEmpty() || start.isEmpty() || end.isEmpty()) {
            return false;
        }

        char codeFirst = code.charAt(0);
        char startFirst = start.charAt(0);
        char endFirst = end.charAt(0);

        if (codeFirst < startFirst || codeFirst > endFirst) {
            return false;
        }

        if (codeFirst > startFirst && codeFirst < endFirst) {
            return true;
        }

        // Same first character, compare numbers
        try {
            int codeNum = extractNumber(code);
            int startNum = extractNumber(start);
            int endNum = extractNumber(end);

            return codeNum >= startNum && codeNum <= endNum;
        } catch (Exception e) {
            return false;
        }
    }

    private int extractNumber(String code) {
        StringBuilder num = new StringBuilder();
        for (int i = 1; i < Math.min(code.length(), 4); i++) {
            char c = code.charAt(i);
            if (Character.isDigit(c)) {
                num.append(c);
            } else {
                break;
            }
        }
        return num.length() > 0 ? Integer.parseInt(num.toString()) : 0;
    }
}
