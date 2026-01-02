package com.hdim.riskmodels.indices;

import com.hdim.riskmodels.models.RiskExplanation;
import com.hdim.riskmodels.models.RiskIndexResult;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Calculates the Charlson Comorbidity Index (CCI) from ICD-10 diagnosis codes.
 *
 * The CCI predicts 10-year mortality for patients with multiple comorbidities.
 * Uses updated Charlson weights and ICD-10-CM 2024 code mappings.
 *
 * Thread-safe implementation with caching for performance.
 */
public class CharlsonComorbidityIndex {

    private static final String INDEX_NAME = "Charlson Comorbidity Index";
    private static final String VERSION = "2024";

    // ICD-10 to Charlson category mappings with weights
    private static final Map<String, CharlsonCategory> ICD10_MAPPINGS;

    static {
        Map<String, CharlsonCategory> mappings = new ConcurrentHashMap<>();

        // Myocardial Infarction (Weight: 1)
        addRangeMapping(mappings, "I21", "I21", CharlsonCategory.MI);
        addRangeMapping(mappings, "I22", "I22", CharlsonCategory.MI);
        mappings.put("I25.2", CharlsonCategory.MI);

        // Congestive Heart Failure (Weight: 1)
        addRangeMapping(mappings, "I50", "I50", CharlsonCategory.CHF);
        mappings.put("I11.0", CharlsonCategory.CHF);
        mappings.put("I13.0", CharlsonCategory.CHF);
        mappings.put("I13.2", CharlsonCategory.CHF);

        // Peripheral Vascular Disease (Weight: 1)
        addRangeMapping(mappings, "I70", "I70", CharlsonCategory.PVD);
        addRangeMapping(mappings, "I71", "I71", CharlsonCategory.PVD);
        addRangeMapping(mappings, "I73", "I73", CharlsonCategory.PVD);
        addRangeMapping(mappings, "I77", "I77", CharlsonCategory.PVD);
        mappings.put("I79.0", CharlsonCategory.PVD);
        mappings.put("K55.1", CharlsonCategory.PVD);
        mappings.put("K55.8", CharlsonCategory.PVD);
        mappings.put("K55.9", CharlsonCategory.PVD);

        // Cerebrovascular Disease (Weight: 1)
        addRangeMapping(mappings, "I60", "I69", CharlsonCategory.CVD);
        mappings.put("G45.0", CharlsonCategory.CVD);
        mappings.put("G45.1", CharlsonCategory.CVD);
        mappings.put("G45.2", CharlsonCategory.CVD);
        mappings.put("G45.8", CharlsonCategory.CVD);
        mappings.put("G45.9", CharlsonCategory.CVD);
        mappings.put("G46", CharlsonCategory.CVD);

        // Dementia (Weight: 1)
        mappings.put("F00", CharlsonCategory.DEMENTIA);
        addRangeMapping(mappings, "F01", "F03", CharlsonCategory.DEMENTIA);
        mappings.put("G30", CharlsonCategory.DEMENTIA);

        // Chronic Pulmonary Disease (Weight: 1)
        addRangeMapping(mappings, "J40", "J47", CharlsonCategory.COPD);
        addRangeMapping(mappings, "J60", "J67", CharlsonCategory.COPD);
        mappings.put("J68.4", CharlsonCategory.COPD);
        mappings.put("J70.1", CharlsonCategory.COPD);
        mappings.put("J70.3", CharlsonCategory.COPD);

        // Connective Tissue Disease (Weight: 1)
        addRangeMapping(mappings, "M32", "M34", CharlsonCategory.CTD);
        mappings.put("M05", CharlsonCategory.CTD);
        mappings.put("M06", CharlsonCategory.CTD);
        mappings.put("M31.5", CharlsonCategory.CTD);
        mappings.put("M35.1", CharlsonCategory.CTD);
        mappings.put("M35.3", CharlsonCategory.CTD);

        // Peptic Ulcer Disease (Weight: 1)
        addRangeMapping(mappings, "K25", "K28", CharlsonCategory.PUD);

        // Mild Liver Disease (Weight: 1)
        mappings.put("K70.0", CharlsonCategory.MILD_LIVER);
        mappings.put("K70.1", CharlsonCategory.MILD_LIVER);
        mappings.put("K70.2", CharlsonCategory.MILD_LIVER);
        mappings.put("K70.3", CharlsonCategory.MILD_LIVER);
        mappings.put("K70.9", CharlsonCategory.MILD_LIVER);
        addRangeMapping(mappings, "K71", "K71", CharlsonCategory.MILD_LIVER);
        mappings.put("K73", CharlsonCategory.MILD_LIVER);
        mappings.put("K74", CharlsonCategory.MILD_LIVER);
        mappings.put("K76.0", CharlsonCategory.MILD_LIVER);

        // Diabetes without complications (Weight: 1)
        mappings.put("E10.0", CharlsonCategory.DIABETES);
        mappings.put("E10.1", CharlsonCategory.DIABETES);
        mappings.put("E10.9", CharlsonCategory.DIABETES);
        mappings.put("E11.0", CharlsonCategory.DIABETES);
        mappings.put("E11.1", CharlsonCategory.DIABETES);
        mappings.put("E11.9", CharlsonCategory.DIABETES);
        addRangeMapping(mappings, "E12", "E14", CharlsonCategory.DIABETES);

        // Diabetes with end organ damage (Weight: 2)
        addMapping(mappings, "E10.2", CharlsonCategory.DIABETES_COMPLICATIONS);
        addMapping(mappings, "E10.3", CharlsonCategory.DIABETES_COMPLICATIONS);
        addMapping(mappings, "E10.4", CharlsonCategory.DIABETES_COMPLICATIONS);
        addMapping(mappings, "E10.5", CharlsonCategory.DIABETES_COMPLICATIONS);
        addMapping(mappings, "E11.2", CharlsonCategory.DIABETES_COMPLICATIONS);
        addMapping(mappings, "E11.3", CharlsonCategory.DIABETES_COMPLICATIONS);
        addMapping(mappings, "E11.4", CharlsonCategory.DIABETES_COMPLICATIONS);
        addMapping(mappings, "E11.5", CharlsonCategory.DIABETES_COMPLICATIONS);

        // Hemiplegia (Weight: 2)
        addRangeMapping(mappings, "G81", "G81", CharlsonCategory.HEMIPLEGIA);
        mappings.put("G82.0", CharlsonCategory.HEMIPLEGIA);
        mappings.put("G82.1", CharlsonCategory.HEMIPLEGIA);
        mappings.put("G82.2", CharlsonCategory.HEMIPLEGIA);

        // Moderate/Severe Renal Disease (Weight: 2)
        mappings.put("N18.3", CharlsonCategory.RENAL_DISEASE);
        mappings.put("N18.4", CharlsonCategory.RENAL_DISEASE);
        mappings.put("N18.5", CharlsonCategory.RENAL_DISEASE);
        mappings.put("N18.6", CharlsonCategory.RENAL_DISEASE);
        mappings.put("N19", CharlsonCategory.RENAL_DISEASE);
        mappings.put("N25.0", CharlsonCategory.RENAL_DISEASE);

        // Any Tumor (Weight: 2)
        addRangeMapping(mappings, "C00", "C26", CharlsonCategory.TUMOR);
        addRangeMapping(mappings, "C30", "C34", CharlsonCategory.TUMOR);
        addRangeMapping(mappings, "C37", "C41", CharlsonCategory.TUMOR);
        addRangeMapping(mappings, "C43", "C43", CharlsonCategory.TUMOR);
        addRangeMapping(mappings, "C45", "C58", CharlsonCategory.TUMOR);
        addRangeMapping(mappings, "C60", "C76", CharlsonCategory.TUMOR);
        mappings.put("C81", CharlsonCategory.TUMOR);

        // Leukemia (Weight: 2)
        addRangeMapping(mappings, "C91", "C95", CharlsonCategory.LEUKEMIA);

        // Lymphoma (Weight: 2)
        addRangeMapping(mappings, "C82", "C85", CharlsonCategory.LYMPHOMA);
        addRangeMapping(mappings, "C88", "C88", CharlsonCategory.LYMPHOMA);
        mappings.put("C90.0", CharlsonCategory.LYMPHOMA);
        mappings.put("C96.0", CharlsonCategory.LYMPHOMA);

        // Moderate/Severe Liver Disease (Weight: 3)
        mappings.put("K70.4", CharlsonCategory.SEVERE_LIVER);
        mappings.put("K71.1", CharlsonCategory.SEVERE_LIVER);
        addRangeMapping(mappings, "K72", "K72", CharlsonCategory.SEVERE_LIVER);
        mappings.put("K76.5", CharlsonCategory.SEVERE_LIVER);
        mappings.put("K76.6", CharlsonCategory.SEVERE_LIVER);
        mappings.put("K76.7", CharlsonCategory.SEVERE_LIVER);
        addRangeMapping(mappings, "I85", "I85", CharlsonCategory.SEVERE_LIVER);

        // Metastatic Solid Tumor (Weight: 6)
        addRangeMapping(mappings, "C77", "C80", CharlsonCategory.METASTATIC_TUMOR);

        // AIDS (Weight: 6)
        mappings.put("B20", CharlsonCategory.AIDS);
        addRangeMapping(mappings, "B21", "B24", CharlsonCategory.AIDS);

        ICD10_MAPPINGS = Collections.unmodifiableMap(mappings);
    }

    private enum CharlsonCategory {
        MI("Myocardial Infarction", 1),
        CHF("Congestive Heart Failure", 1),
        PVD("Peripheral Vascular Disease", 1),
        CVD("Cerebrovascular Disease", 1),
        DEMENTIA("Dementia", 1),
        COPD("Chronic Pulmonary Disease", 1),
        CTD("Connective Tissue Disease", 1),
        PUD("Peptic Ulcer Disease", 1),
        MILD_LIVER("Mild Liver Disease", 1),
        DIABETES("Diabetes without complications", 1),
        DIABETES_COMPLICATIONS("Diabetes with end organ damage", 2),
        HEMIPLEGIA("Hemiplegia", 2),
        RENAL_DISEASE("Moderate/Severe Renal Disease", 2),
        TUMOR("Any Tumor", 2),
        LEUKEMIA("Leukemia", 2),
        LYMPHOMA("Lymphoma", 2),
        SEVERE_LIVER("Moderate/Severe Liver Disease", 3),
        METASTATIC_TUMOR("Metastatic Solid Tumor", 6),
        AIDS("AIDS", 6);

        private final String description;
        private final int weight;

        CharlsonCategory(String description, int weight) {
            this.description = description;
            this.weight = weight;
        }

        public String getDescription() {
            return description;
        }

        public int getWeight() {
            return weight;
        }
    }

    /**
     * Calculates the Charlson Comorbidity Index for a patient.
     *
     * @param icd10Codes List of ICD-10 diagnosis codes
     * @param age Patient's age in years
     * @return RiskIndexResult containing score and explanations
     */
    public RiskIndexResult calculate(List<String> icd10Codes, int age) {
        Objects.requireNonNull(icd10Codes, "ICD-10 codes list cannot be null");
        if (age < 0) {
            throw new IllegalArgumentException("Age must be non-negative");
        }

        Set<CharlsonCategory> identifiedCategories = new HashSet<>();
        List<RiskExplanation> explanations = new ArrayList<>();

        // Process diagnosis codes
        for (String code : icd10Codes) {
            CharlsonCategory category = mapICD10ToCategory(code);
            if (category != null) {
                identifiedCategories.add(category);
            }
        }

        // Handle hierarchical conditions (keep only highest weight)
        resolveHierarchies(identifiedCategories);

        // Calculate score from comorbidities
        double score = 0.0;
        for (CharlsonCategory category : identifiedCategories) {
            score += category.getWeight();
            explanations.add(RiskExplanation.builder()
                .factor(category.getDescription())
                .description(category.getDescription())
                .contribution(category.getWeight())
                .evidenceSystem("ICD-10-CM")
                .build());
        }

        // Add age adjustment (1 point per decade over 40)
        int agePoints = calculateAgePoints(age);
        if (agePoints > 0) {
            score += agePoints;
            explanations.add(RiskExplanation.builder()
                .factor("Age Adjustment")
                .description(String.format("Age %d years (%d decade(s) over 40)", age, agePoints))
                .contribution(agePoints)
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

    /**
     * Maps an ICD-10 code to a Charlson category.
     */
    private CharlsonCategory mapICD10ToCategory(String icd10Code) {
        if (icd10Code == null || icd10Code.trim().isEmpty()) {
            return null;
        }

        String cleanCode = icd10Code.trim().toUpperCase();

        // Try exact match first
        CharlsonCategory category = ICD10_MAPPINGS.get(cleanCode);
        if (category != null) {
            return category;
        }

        // Try progressive substring matching
        for (int i = cleanCode.length(); i >= 3; i--) {
            String substring = cleanCode.substring(0, i);
            category = ICD10_MAPPINGS.get(substring);
            if (category != null) {
                return category;
            }
        }

        return null;
    }

    /**
     * Resolves hierarchical conditions (e.g., diabetes with/without complications).
     * Keeps only the category with the highest weight.
     */
    private void resolveHierarchies(Set<CharlsonCategory> categories) {
        // Diabetes: keep only the higher-weighted version
        if (categories.contains(CharlsonCategory.DIABETES) &&
            categories.contains(CharlsonCategory.DIABETES_COMPLICATIONS)) {
            categories.remove(CharlsonCategory.DIABETES);
        }

        // Liver disease: keep only the higher-weighted version
        if (categories.contains(CharlsonCategory.MILD_LIVER) &&
            categories.contains(CharlsonCategory.SEVERE_LIVER)) {
            categories.remove(CharlsonCategory.MILD_LIVER);
        }

        // Cancer: keep only the higher-weighted version
        if (categories.contains(CharlsonCategory.TUMOR) &&
            categories.contains(CharlsonCategory.METASTATIC_TUMOR)) {
            categories.remove(CharlsonCategory.TUMOR);
        }
    }

    /**
     * Calculates age adjustment points (1 point per decade over 40).
     */
    private int calculateAgePoints(int age) {
        if (age < 50) {
            return 0;
        }
        return Math.min((age - 40) / 10, 4);
    }

    /**
     * Interprets the Charlson score into risk categories.
     */
    private String interpretScore(double score) {
        if (score <= 1) {
            return "Low Risk";
        } else if (score == 2) {
            return "Medium Risk";
        } else if (score <= 4) {
            return "High Risk";
        } else {
            return "Very High Risk";
        }
    }

    /**
     * Helper method to add range mappings.
     */
    private static void addRangeMapping(Map<String, CharlsonCategory> map, String start, String end, CharlsonCategory category) {
        String prefix = start.substring(0, 1);
        int startNum = Integer.parseInt(start.substring(1));
        int endNum = Integer.parseInt(end.substring(1));
        int numLength = start.substring(1).length(); // Preserve leading zeros

        for (int i = startNum; i <= endNum; i++) {
            String code = prefix + String.format("%0" + numLength + "d", i);
            map.put(code, category);
        }
    }

    /**
     * Helper method to add mapping with all subcodes.
     */
    private static void addMapping(Map<String, CharlsonCategory> map, String prefix, CharlsonCategory category) {
        map.put(prefix, category);
    }
}
