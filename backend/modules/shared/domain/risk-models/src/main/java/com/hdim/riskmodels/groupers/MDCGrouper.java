package com.hdim.riskmodels.groupers;

import java.util.*;

/**
 * Major Diagnostic Category (MDC) grouper for ICD-10 codes.
 * Used in DRG assignment and hospital case mix analysis.
 */
public class MDCGrouper {

    private final Map<String, Integer> icd10ToMDC = new HashMap<>();
    private final Map<Integer, String> mdcDescriptions = new HashMap<>();

    public MDCGrouper() {
        initializeMappings();
        initializeDescriptions();
    }

    public Integer getMDC(String icd10Code) {
        if (icd10Code == null || icd10Code.trim().isEmpty()) {
            return null;
        }

        String cleanCode = icd10Code.trim().toUpperCase();
        Integer mdc = icd10ToMDC.get(cleanCode);

        if (mdc != null) {
            return mdc;
        }

        for (int i = cleanCode.length(); i >= 1; i--) {
            String substring = cleanCode.substring(0, i);
            mdc = icd10ToMDC.get(substring);
            if (mdc != null) {
                return mdc;
            }
        }

        return null;
    }

    public String getMDCDescription(Integer mdc) {
        return mdcDescriptions.getOrDefault(mdc, "Unknown MDC");
    }

    public Map<String, Integer> groupMultiple(List<String> icd10Codes) {
        Map<String, Integer> results = new LinkedHashMap<>();
        for (String code : icd10Codes) {
            Integer mdc = getMDC(code);
            if (mdc != null) {
                results.put(code, mdc);
            }
        }
        return results;
    }

    private void initializeMappings() {
        // MDC 1: Nervous System
        addRange("G", 1);

        // MDC 4: Respiratory System
        addRange("J", 4);

        // MDC 5: Circulatory System
        addRange("I", 5);

        // MDC 6: Digestive System
        addRange("K", 6);

        // MDC 7: Hepatobiliary and Pancreas
        icd10ToMDC.put("K70", 7);
        icd10ToMDC.put("K71", 7);
        icd10ToMDC.put("K72", 7);

        // MDC 8: Musculoskeletal System
        addRange("M", 8);

        // MDC 10: Endocrine, Nutritional and Metabolic
        addRange("E", 10);

        // MDC 11: Kidney and Urinary Tract
        addRange("N", 11);
    }

    private void initializeDescriptions() {
        mdcDescriptions.put(1, "Diseases and Disorders of the Nervous System");
        mdcDescriptions.put(2, "Diseases and Disorders of the Eye");
        mdcDescriptions.put(3, "Diseases and Disorders of the Ear, Nose, Mouth and Throat");
        mdcDescriptions.put(4, "Diseases and Disorders of the Respiratory System");
        mdcDescriptions.put(5, "Diseases and Disorders of the Circulatory System");
        mdcDescriptions.put(6, "Diseases and Disorders of the Digestive System");
        mdcDescriptions.put(7, "Diseases and Disorders of the Hepatobiliary System and Pancreas");
        mdcDescriptions.put(8, "Diseases and Disorders of the Musculoskeletal System");
        mdcDescriptions.put(9, "Diseases and Disorders of the Skin");
        mdcDescriptions.put(10, "Endocrine, Nutritional and Metabolic Diseases");
        mdcDescriptions.put(11, "Diseases and Disorders of the Kidney and Urinary Tract");
    }

    private void addRange(String prefix, int mdc) {
        icd10ToMDC.put(prefix, mdc);
    }
}
