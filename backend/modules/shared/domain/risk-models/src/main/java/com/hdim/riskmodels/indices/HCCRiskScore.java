package com.hdim.riskmodels.indices;

import com.hdim.riskmodels.models.RiskExplanation;
import com.hdim.riskmodels.models.RiskIndexResult;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calculates CMS Hierarchical Condition Category (HCC) Risk Score using Model V28.
 * Predicts Medicare costs and adjusts capitation payments.
 * Thread-safe implementation.
 */
public class HCCRiskScore {

    private static final String INDEX_NAME = "HCC Risk Score";
    private static final String VERSION = "CMS-HCC-V28";

    private static final Map<String, Integer> ICD10_TO_HCC;
    private static final Map<Integer, Double> HCC_WEIGHTS;

    static {
        // Initialize ICD-10 to HCC mappings (simplified for key conditions)
        Map<String, Integer> icd10Map = new ConcurrentHashMap<>();

        // HCC 8: Metastatic Cancer
        addRange(icd10Map, "C77", "C80", 8);

        // HCC 9: Lung and Other Severe Cancers
        addRange(icd10Map, "C30", "C34", 9);

        // HCC 10: Lymphoma and Other Cancers
        addRange(icd10Map, "C81", "C85", 10);
        addRange(icd10Map, "C00", "C26", 10);

        // HCC 18: Diabetes with Chronic Complications
        addMapping(icd10Map, "E10.2", 18);
        addMapping(icd10Map, "E10.3", 18);
        addMapping(icd10Map, "E10.4", 18);
        addMapping(icd10Map, "E10.5", 18);
        addMapping(icd10Map, "E11.2", 18);
        addMapping(icd10Map, "E11.3", 18);
        addMapping(icd10Map, "E11.4", 18);
        addMapping(icd10Map, "E11.5", 18);

        // HCC 19: Diabetes without Complication
        icd10Map.put("E10.0", 19);
        icd10Map.put("E10.1", 19);
        icd10Map.put("E10.9", 19);
        icd10Map.put("E11.0", 19);
        icd10Map.put("E11.1", 19);
        icd10Map.put("E11.9", 19);

        // HCC 85: CHF
        addRange(icd10Map, "I50", "I50", 85);
        icd10Map.put("I11.0", 85);
        icd10Map.put("I13.0", 85);
        icd10Map.put("I13.2", 85);

        // HCC 96: Ischemic Heart Disease
        addRange(icd10Map, "I20", "I22", 96);
        addRange(icd10Map, "I24", "I25", 96);

        // HCC 111: COPD
        addRange(icd10Map, "J40", "J47", 111);

        // HCC 134: Kidney Disease Stage 4
        icd10Map.put("N18.4", 134);

        // HCC 135: Kidney Disease Stage 5/ESRD
        icd10Map.put("N18.5", 135);
        icd10Map.put("N18.6", 135);

        // HCC 136: Kidney Disease Stage 3
        icd10Map.put("N18.3", 136);

        // HCC 28: Cirrhosis
        addRange(icd10Map, "K70", "K70", 28);
        addRange(icd10Map, "K72", "K72", 28);
        icd10Map.put("K76.6", 28);

        ICD10_TO_HCC = Collections.unmodifiableMap(icd10Map);

        // HCC Weights (community, non-dual, age 65+)
        Map<Integer, Double> weights = new HashMap<>();
        weights.put(8, 2.659);   // Metastatic Cancer
        weights.put(9, 1.323);   // Lung Cancer
        weights.put(10, 0.968);  // Lymphoma
        weights.put(18, 0.318);  // Diabetes with complications
        weights.put(19, 0.104);  // Diabetes without complications
        weights.put(85, 0.323);  // CHF
        weights.put(96, 0.184);  // Ischemic Heart Disease
        weights.put(111, 0.328); // COPD
        weights.put(134, 0.237); // CKD Stage 4
        weights.put(135, 0.415); // CKD Stage 5
        weights.put(136, 0.237); // CKD Stage 3
        weights.put(28, 0.396);  // Cirrhosis

        HCC_WEIGHTS = Collections.unmodifiableMap(weights);
    }

    public RiskIndexResult calculate(List<String> icd10Codes, int age, boolean isMale,
                                     boolean isDisabled, boolean isDualEligible) {
        Objects.requireNonNull(icd10Codes, "ICD-10 codes list cannot be null");
        if (age < 0) {
            throw new IllegalArgumentException("Age must be non-negative");
        }

        List<RiskExplanation> explanations = new ArrayList<>();
        double score = 0.0;

        // Age-sex demographic coefficient
        double demographicScore = calculateDemographicScore(age, isMale, isDisabled, isDualEligible);
        score += demographicScore;
        explanations.add(RiskExplanation.builder()
            .factor(String.format("Age-Sex: %d years, %s", age, isMale ? "Male" : "Female"))
            .description(getAgeSexCategory(age, isMale))
            .contribution(demographicScore)
            .build());

        // Identify HCCs from diagnoses
        Set<Integer> identifiedHCCs = new HashSet<>();
        for (String code : icd10Codes) {
            Integer hcc = mapICD10ToHCC(code);
            if (hcc != null) {
                identifiedHCCs.add(hcc);
            }
        }

        // Apply hierarchies
        applyHierarchies(identifiedHCCs);

        // Calculate HCC contributions
        for (Integer hcc : identifiedHCCs) {
            Double weight = HCC_WEIGHTS.get(hcc);
            if (weight != null) {
                score += weight;
                explanations.add(RiskExplanation.builder()
                    .factor("HCC " + hcc)
                    .description(getHCCDescription(hcc))
                    .contribution(weight)
                    .evidenceSystem("CMS-HCC-V28")
                    .build());
            }
        }

        // Disease interactions
        double interactionScore = calculateInteractions(identifiedHCCs);
        if (interactionScore > 0) {
            score += interactionScore;
            explanations.add(RiskExplanation.builder()
                .factor("Disease Interactions")
                .description("Multiple chronic condition interactions")
                .contribution(interactionScore)
                .build());
        }

        String interpretation = interpretScore(score);

        return RiskIndexResult.builder()
            .indexName(INDEX_NAME)
            .score(score)
            .interpretation(interpretation)
            .explanations(explanations)
            .calculatedAt(Instant.now())
            .version(VERSION)
            .build();
    }

    private double calculateDemographicScore(int age, boolean isMale, boolean isDisabled, boolean isDualEligible) {
        // Simplified demographic coefficients
        if (isDisabled) {
            return 0.409;
        }

        if (age < 65) {
            return 0.320;
        } else if (age <= 69) {
            return isMale ? 0.455 : 0.434;
        } else if (age <= 74) {
            return isMale ? 0.586 : 0.558;
        } else if (age <= 79) {
            return isMale ? 0.700 : 0.664;
        } else {
            return isMale ? 0.806 : 0.758;
        }
    }

    private String getAgeSexCategory(int age, boolean isMale) {
        String gender = isMale ? "Male" : "Female";
        if (age < 65) return "Under 65, " + gender;
        if (age <= 69) return "65-69, " + gender;
        if (age <= 74) return "70-74, " + gender;
        if (age <= 79) return "75-79, " + gender;
        return "80+, " + gender;
    }

    private Integer mapICD10ToHCC(String icd10Code) {
        if (icd10Code == null || icd10Code.trim().isEmpty()) {
            return null;
        }

        String cleanCode = icd10Code.trim().toUpperCase();

        Integer hcc = ICD10_TO_HCC.get(cleanCode);
        if (hcc != null) {
            return hcc;
        }

        for (int i = cleanCode.length(); i >= 3; i--) {
            String substring = cleanCode.substring(0, i);
            hcc = ICD10_TO_HCC.get(substring);
            if (hcc != null) {
                return hcc;
            }
        }

        return null;
    }

    private void applyHierarchies(Set<Integer> hccs) {
        // Diabetes: HCC18 trumps HCC19
        if (hccs.contains(18) && hccs.contains(19)) {
            hccs.remove(19);
        }

        // CKD: Higher stages trump lower stages
        if (hccs.contains(135) && hccs.contains(136)) {
            hccs.remove(136);
        }
        if (hccs.contains(134) && hccs.contains(136)) {
            hccs.remove(136);
        }
        if (hccs.contains(135) && hccs.contains(134)) {
            hccs.remove(134);
        }
    }

    private double calculateInteractions(Set<Integer> hccs) {
        double interaction = 0.0;

        // Diabetes + CHF interaction
        if (hccs.contains(18) && hccs.contains(85)) {
            interaction += 0.154;
        }

        // CHF + CKD interaction
        if (hccs.contains(85) && (hccs.contains(134) || hccs.contains(135) || hccs.contains(136))) {
            interaction += 0.119;
        }

        return interaction;
    }

    private String getHCCDescription(int hcc) {
        switch (hcc) {
            case 8: return "Metastatic Cancer and Acute Leukemia";
            case 9: return "Lung and Other Severe Cancers";
            case 10: return "Lymphoma and Other Cancers";
            case 18: return "Diabetes with Chronic Complications";
            case 19: return "Diabetes without Complication";
            case 85: return "Congestive Heart Failure";
            case 96: return "Ischemic Heart Disease";
            case 111: return "Chronic Obstructive Pulmonary Disease";
            case 134: return "Chronic Kidney Disease Stage 4";
            case 135: return "Chronic Kidney Disease Stage 5";
            case 136: return "Chronic Kidney Disease Stage 3";
            case 28: return "Cirrhosis of Liver";
            default: return "HCC " + hcc;
        }
    }

    private String interpretScore(double score) {
        if (score < 1.0) {
            return "Below Average Risk";
        } else if (score < 1.5) {
            return "Average Risk";
        } else if (score < 2.5) {
            return "Above Average Risk";
        } else {
            return "High Risk";
        }
    }

    private static void addRange(Map<String, Integer> map, String start, String end, int hcc) {
        String prefix = start.substring(0, 1);
        int startNum = Integer.parseInt(start.substring(1));
        int endNum = Integer.parseInt(end.substring(1));

        for (int i = startNum; i <= endNum; i++) {
            String code = prefix + i;
            map.put(code, hcc);
        }
    }

    private static void addMapping(Map<String, Integer> map, String prefix, int hcc) {
        map.put(prefix, hcc);
    }
}
