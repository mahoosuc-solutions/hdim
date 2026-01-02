package com.hdim.riskmodels.groupers;

import java.util.*;

/**
 * Groups ICD-10 codes into HCC categories for risk adjustment.
 */
public class HCCGrouper {

    private final Map<String, Integer> icd10ToHCC = new HashMap<>();

    public HCCGrouper() {
        initializeMappings();
    }

    public Integer getHCC(String icd10Code) {
        if (icd10Code == null || icd10Code.trim().isEmpty()) {
            return null;
        }

        String cleanCode = icd10Code.trim().toUpperCase();
        Integer hcc = icd10ToHCC.get(cleanCode);

        if (hcc != null) {
            return hcc;
        }

        // Try progressive substring matching
        for (int i = cleanCode.length(); i >= 3; i--) {
            String substring = cleanCode.substring(0, i);
            hcc = icd10ToHCC.get(substring);
            if (hcc != null) {
                return hcc;
            }
        }

        return null;
    }

    public Map<String, Integer> groupMultiple(List<String> icd10Codes) {
        Map<String, Integer> results = new LinkedHashMap<>();
        for (String code : icd10Codes) {
            Integer hcc = getHCC(code);
            if (hcc != null) {
                results.put(code, hcc);
            }
        }
        return results;
    }

    private void initializeMappings() {
        // HCC 8: Metastatic Cancer
        addRange("C77", "C80", 8);

        // HCC 18: Diabetes with complications
        addMapping("E10.2", 18);
        addMapping("E10.3", 18);
        addMapping("E11.2", 18);
        addMapping("E11.3", 18);

        // HCC 19: Diabetes without complications
        icd10ToHCC.put("E10.9", 19);
        icd10ToHCC.put("E11.9", 19);

        // HCC 85: CHF
        addRange("I50", "I50", 85);

        // HCC 111: COPD
        addRange("J40", "J47", 111);

        // HCC 134-136: CKD
        icd10ToHCC.put("N18.3", 136);
        icd10ToHCC.put("N18.4", 134);
        icd10ToHCC.put("N18.5", 135);
    }

    private void addRange(String start, String end, int hcc) {
        String prefix = start.substring(0, 1);
        int startNum = Integer.parseInt(start.substring(1));
        int endNum = Integer.parseInt(end.substring(1));

        for (int i = startNum; i <= endNum; i++) {
            icd10ToHCC.put(prefix + i, hcc);
        }
    }

    private void addMapping(String prefix, int hcc) {
        icd10ToHCC.put(prefix, hcc);
    }
}
