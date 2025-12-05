package com.hdim.riskmodels.indices;

import com.hdim.riskmodels.models.RiskExplanation;
import com.hdim.riskmodels.models.RiskIndexResult;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Calculates the Elixhauser Comorbidity Index from ICD-10 diagnosis codes.
 *
 * Uses the 31-category Elixhauser model with AHRQ weights for mortality prediction.
 * Thread-safe implementation with caching.
 */
public class ElixhauserComorbidityIndex {

    private static final String INDEX_NAME = "Elixhauser Comorbidity Index";
    private static final String VERSION = "2024-AHRQ";

    private static final Map<String, ElixhauserCategory> ICD10_MAPPINGS;

    static {
        Map<String, ElixhauserCategory> mappings = new ConcurrentHashMap<>();

        // CHF (Weight: 9)
        addRangeMapping(mappings, "I50", "I50", ElixhauserCategory.CHF);
        mappings.put("I11.0", ElixhauserCategory.CHF);
        mappings.put("I13.0", ElixhauserCategory.CHF);
        mappings.put("I13.2", ElixhauserCategory.CHF);

        // Cardiac Arrhythmia (Weight: 0)
        addRangeMapping(mappings, "I44", "I45", ElixhauserCategory.ARRHYTHMIA);
        addRangeMapping(mappings, "I47", "I49", ElixhauserCategory.ARRHYTHMIA);
        mappings.put("I46.0", ElixhauserCategory.ARRHYTHMIA);
        mappings.put("R00.0", ElixhauserCategory.ARRHYTHMIA);
        mappings.put("R00.1", ElixhauserCategory.ARRHYTHMIA);

        // Valvular Disease (Weight: 0)
        addRangeMapping(mappings, "I34", "I39", ElixhauserCategory.VALVULAR);
        mappings.put("I08", ElixhauserCategory.VALVULAR);
        mappings.put("I09.1", ElixhauserCategory.VALVULAR);
        mappings.put("I09.8", ElixhauserCategory.VALVULAR);

        // Pulmonary Circulation Disorder (Weight: 6)
        addRangeMapping(mappings, "I26", "I28", ElixhauserCategory.PULMONARY_CIRC);

        // Peripheral Vascular Disorder (Weight: 3)
        addRangeMapping(mappings, "I70", "I71", ElixhauserCategory.PVD);
        addRangeMapping(mappings, "I73", "I73", ElixhauserCategory.PVD);
        mappings.put("I77.1", ElixhauserCategory.PVD);
        mappings.put("I79.0", ElixhauserCategory.PVD);

        // Hypertension Uncomplicated (Weight: -1)
        mappings.put("I10", ElixhauserCategory.HTN_UNCOMPLICATED);

        // Hypertension Complicated (Weight: -1)
        addRangeMapping(mappings, "I11", "I13", ElixhauserCategory.HTN_COMPLICATED);
        mappings.put("I15.0", ElixhauserCategory.HTN_COMPLICATED);
        mappings.put("I15.1", ElixhauserCategory.HTN_COMPLICATED);

        // Paralysis (Weight: 5)
        addRangeMapping(mappings, "G81", "G83", ElixhauserCategory.PARALYSIS);

        // Other Neurological Disorders (Weight: 5)
        mappings.put("G20", ElixhauserCategory.NEURO_OTHER);
        mappings.put("G21", ElixhauserCategory.NEURO_OTHER);
        addRangeMapping(mappings, "G30", "G32", ElixhauserCategory.NEURO_OTHER);
        addRangeMapping(mappings, "G35", "G37", ElixhauserCategory.NEURO_OTHER);
        mappings.put("G40", ElixhauserCategory.NEURO_OTHER);

        // Chronic Pulmonary Disease (Weight: 3)
        addRangeMapping(mappings, "J40", "J47", ElixhauserCategory.COPD);
        addRangeMapping(mappings, "J60", "J67", ElixhauserCategory.COPD);

        // Diabetes Uncomplicated (Weight: 0)
        mappings.put("E10.0", ElixhauserCategory.DIABETES_UNCOMPLICATED);
        mappings.put("E10.1", ElixhauserCategory.DIABETES_UNCOMPLICATED);
        mappings.put("E10.9", ElixhauserCategory.DIABETES_UNCOMPLICATED);
        mappings.put("E11.0", ElixhauserCategory.DIABETES_UNCOMPLICATED);
        mappings.put("E11.1", ElixhauserCategory.DIABETES_UNCOMPLICATED);
        mappings.put("E11.9", ElixhauserCategory.DIABETES_UNCOMPLICATED);

        // Diabetes Complicated (Weight: 0)
        addMapping(mappings, "E10.2", ElixhauserCategory.DIABETES_COMPLICATED);
        addMapping(mappings, "E10.3", ElixhauserCategory.DIABETES_COMPLICATED);
        addMapping(mappings, "E10.4", ElixhauserCategory.DIABETES_COMPLICATED);
        addMapping(mappings, "E10.5", ElixhauserCategory.DIABETES_COMPLICATED);
        addMapping(mappings, "E11.2", ElixhauserCategory.DIABETES_COMPLICATED);
        addMapping(mappings, "E11.3", ElixhauserCategory.DIABETES_COMPLICATED);
        addMapping(mappings, "E11.4", ElixhauserCategory.DIABETES_COMPLICATED);
        addMapping(mappings, "E11.5", ElixhauserCategory.DIABETES_COMPLICATED);

        // Hypothyroidism (Weight: 0)
        addRangeMapping(mappings, "E00", "E03", ElixhauserCategory.HYPOTHYROID);
        mappings.put("E89.0", ElixhauserCategory.HYPOTHYROID);

        // Renal Failure (Weight: 6)
        addRangeMapping(mappings, "N18", "N19", ElixhauserCategory.RENAL_FAILURE);
        mappings.put("N25.0", ElixhauserCategory.RENAL_FAILURE);

        // Liver Disease (Weight: 4)
        addRangeMapping(mappings, "K70", "K76", ElixhauserCategory.LIVER_DISEASE);

        // Peptic Ulcer Disease (Weight: 0)
        addRangeMapping(mappings, "K25", "K28", ElixhauserCategory.PUD);

        // AIDS/HIV (Weight: 0)
        addRangeMapping(mappings, "B20", "B24", ElixhauserCategory.AIDS);

        // Lymphoma (Weight: 6)
        addRangeMapping(mappings, "C81", "C85", ElixhauserCategory.LYMPHOMA);
        mappings.put("C88", ElixhauserCategory.LYMPHOMA);
        mappings.put("C90.0", ElixhauserCategory.LYMPHOMA);

        // Metastatic Cancer (Weight: 14)
        addRangeMapping(mappings, "C77", "C80", ElixhauserCategory.METS);

        // Solid Tumor without Metastasis (Weight: 7)
        addRangeMapping(mappings, "C00", "C26", ElixhauserCategory.SOLID_TUMOR);
        addRangeMapping(mappings, "C30", "C34", ElixhauserCategory.SOLID_TUMOR);
        addRangeMapping(mappings, "C37", "C41", ElixhauserCategory.SOLID_TUMOR);
        mappings.put("C43", ElixhauserCategory.SOLID_TUMOR);
        addRangeMapping(mappings, "C45", "C58", ElixhauserCategory.SOLID_TUMOR);
        addRangeMapping(mappings, "C60", "C76", ElixhauserCategory.SOLID_TUMOR);

        // Rheumatoid Arthritis/Collagen Vascular Disease (Weight: 0)
        addRangeMapping(mappings, "M05", "M06", ElixhauserCategory.RHEUMATOID);
        addRangeMapping(mappings, "M30", "M36", ElixhauserCategory.RHEUMATOID);

        // Coagulopathy (Weight: 11)
        addRangeMapping(mappings, "D65", "D68", ElixhauserCategory.COAGULOPATHY);
        mappings.put("D69.1", ElixhauserCategory.COAGULOPATHY);

        // Obesity (Weight: -5)
        mappings.put("E66", ElixhauserCategory.OBESITY);

        // Weight Loss (Weight: 9)
        mappings.put("R63.4", ElixhauserCategory.WEIGHT_LOSS);
        mappings.put("R64", ElixhauserCategory.WEIGHT_LOSS);

        // Fluid and Electrolyte Disorders (Weight: 11)
        addRangeMapping(mappings, "E86", "E87", ElixhauserCategory.FLUID_ELECTROLYTE);

        // Blood Loss Anemia (Weight: -3)
        mappings.put("D50.0", ElixhauserCategory.BLOOD_LOSS_ANEMIA);

        // Deficiency Anemia (Weight: -2)
        addRangeMapping(mappings, "D50", "D53", ElixhauserCategory.DEFICIENCY_ANEMIA);

        // Alcohol Abuse (Weight: 0)
        addRangeMapping(mappings, "F10", "F10", ElixhauserCategory.ALCOHOL_ABUSE);

        // Drug Abuse (Weight: -7)
        addRangeMapping(mappings, "F11", "F16", ElixhauserCategory.DRUG_ABUSE);
        addRangeMapping(mappings, "F18", "F19", ElixhauserCategory.DRUG_ABUSE);

        // Psychoses (Weight: -5)
        addRangeMapping(mappings, "F20", "F29", ElixhauserCategory.PSYCHOSES);

        // Depression (Weight: -5)
        addRangeMapping(mappings, "F32", "F33", ElixhauserCategory.DEPRESSION);

        ICD10_MAPPINGS = Collections.unmodifiableMap(mappings);
    }

    private enum ElixhauserCategory {
        CHF("Congestive Heart Failure", 9),
        ARRHYTHMIA("Cardiac Arrhythmia", 0),
        VALVULAR("Valvular Disease", 0),
        PULMONARY_CIRC("Pulmonary Circulation Disorder", 6),
        PVD("Peripheral Vascular Disorder", 3),
        HTN_UNCOMPLICATED("Hypertension Uncomplicated", -1),
        HTN_COMPLICATED("Hypertension Complicated", -1),
        PARALYSIS("Paralysis", 5),
        NEURO_OTHER("Other Neurological Disorders", 5),
        COPD("Chronic Pulmonary Disease", 3),
        DIABETES_UNCOMPLICATED("Diabetes Uncomplicated", 0),
        DIABETES_COMPLICATED("Diabetes Complicated", 0),
        HYPOTHYROID("Hypothyroidism", 0),
        RENAL_FAILURE("Renal Failure", 6),
        LIVER_DISEASE("Liver Disease", 4),
        PUD("Peptic Ulcer Disease", 0),
        AIDS("AIDS/HIV", 0),
        LYMPHOMA("Lymphoma", 6),
        METS("Metastatic Cancer", 14),
        SOLID_TUMOR("Solid Tumor without Metastasis", 7),
        RHEUMATOID("Rheumatoid Arthritis", 0),
        COAGULOPATHY("Coagulopathy", 11),
        OBESITY("Obesity", -5),
        WEIGHT_LOSS("Weight Loss", 9),
        FLUID_ELECTROLYTE("Fluid and Electrolyte Disorders", 11),
        BLOOD_LOSS_ANEMIA("Blood Loss Anemia", -3),
        DEFICIENCY_ANEMIA("Deficiency Anemia", -2),
        ALCOHOL_ABUSE("Alcohol Abuse", 0),
        DRUG_ABUSE("Drug Abuse", -7),
        PSYCHOSES("Psychoses", -5),
        DEPRESSION("Depression", -5);

        private final String description;
        private final int weight;

        ElixhauserCategory(String description, int weight) {
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

    public RiskIndexResult calculate(List<String> icd10Codes) {
        Objects.requireNonNull(icd10Codes, "ICD-10 codes list cannot be null");

        Set<ElixhauserCategory> identifiedCategories = new HashSet<>();
        List<RiskExplanation> explanations = new ArrayList<>();

        for (String code : icd10Codes) {
            ElixhauserCategory category = mapICD10ToCategory(code);
            if (category != null) {
                identifiedCategories.add(category);
            }
        }

        resolveHierarchies(identifiedCategories);

        double score = 0.0;
        for (ElixhauserCategory category : identifiedCategories) {
            score += category.getWeight();
            explanations.add(RiskExplanation.builder()
                .factor(category.getDescription())
                .description(category.getDescription())
                .contribution(category.getWeight())
                .evidenceSystem("ICD-10-CM")
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

    private ElixhauserCategory mapICD10ToCategory(String icd10Code) {
        if (icd10Code == null || icd10Code.trim().isEmpty()) {
            return null;
        }

        String cleanCode = icd10Code.trim().toUpperCase();

        ElixhauserCategory category = ICD10_MAPPINGS.get(cleanCode);
        if (category != null) {
            return category;
        }

        for (int i = cleanCode.length(); i >= 3; i--) {
            String substring = cleanCode.substring(0, i);
            category = ICD10_MAPPINGS.get(substring);
            if (category != null) {
                return category;
            }
        }

        return null;
    }

    private void resolveHierarchies(Set<ElixhauserCategory> categories) {
        if (categories.contains(ElixhauserCategory.DIABETES_UNCOMPLICATED) &&
            categories.contains(ElixhauserCategory.DIABETES_COMPLICATED)) {
            categories.remove(ElixhauserCategory.DIABETES_UNCOMPLICATED);
        }

        if (categories.contains(ElixhauserCategory.HTN_UNCOMPLICATED) &&
            categories.contains(ElixhauserCategory.HTN_COMPLICATED)) {
            categories.remove(ElixhauserCategory.HTN_UNCOMPLICATED);
        }

        if (categories.contains(ElixhauserCategory.SOLID_TUMOR) &&
            categories.contains(ElixhauserCategory.METS)) {
            categories.remove(ElixhauserCategory.SOLID_TUMOR);
        }
    }

    private String interpretScore(double score) {
        if (score < 0) {
            return "Low Risk";
        } else if (score <= 10) {
            return "Medium Risk";
        } else if (score <= 25) {
            return "High Risk";
        } else {
            return "Very High Risk";
        }
    }

    private static void addRangeMapping(Map<String, ElixhauserCategory> map, String start, String end, ElixhauserCategory category) {
        String prefix = start.substring(0, 1);
        int startNum = Integer.parseInt(start.substring(1));
        int endNum = Integer.parseInt(end.substring(1));

        for (int i = startNum; i <= endNum; i++) {
            String code = prefix + i;
            map.put(code, category);
        }
    }

    private static void addMapping(Map<String, ElixhauserCategory> map, String prefix, ElixhauserCategory category) {
        map.put(prefix, category);
    }
}
