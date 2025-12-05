package com.healthdata.enrichment.service;

import com.healthdata.enrichment.model.EntityType;
import com.healthdata.enrichment.model.ExtractedEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Named Entity Recognition for medical terms using pattern matching and NLP.
 */
@Service
@Slf4j
public class MedicalEntityRecognizer {

    // Common medical term patterns
    private static final Map<EntityType, List<String>> MEDICAL_TERMINOLOGY = new HashMap<>();
    private static final Pattern ICD10_PATTERN = Pattern.compile("[A-Z]\\d{2}\\.?\\d{0,2}");
    private static final Pattern DOSAGE_PATTERN = Pattern.compile("\\d+\\s*(mg|mcg|g|ml|units?)");
    private static final Pattern FREQUENCY_PATTERN = Pattern.compile("\\b(daily|BID|TID|QID|HS|PRN|QD|BID|TID)\\b", Pattern.CASE_INSENSITIVE);

    static {
        // Diagnoses
        MEDICAL_TERMINOLOGY.put(EntityType.DIAGNOSIS, Arrays.asList(
            "diabetes mellitus", "diabetes", "DM", "Type 2 Diabetes", "Type 1 Diabetes",
            "hypertension", "HTN", "hyperlipidemia", "HLD",
            "myocardial infarction", "MI", "stroke", "CVA",
            "pneumonia", "COPD", "asthma", "CHF", "heart failure",
            "chronic kidney disease", "CKD", "renal failure"
        ));

        // Medications
        MEDICAL_TERMINOLOGY.put(EntityType.MEDICATION, Arrays.asList(
            "metformin", "lisinopril", "atorvastatin", "amlodipine",
            "aspirin", "insulin", "jardiance", "farxiga",
            "losartan", "simvastatin", "omeprazole", "levothyroxine"
        ));

        // Lab Results
        MEDICAL_TERMINOLOGY.put(EntityType.LAB_RESULT, Arrays.asList(
            "HbA1c", "A1c", "hemoglobin A1c",
            "creatinine", "eGFR", "GFR",
            "cholesterol", "LDL", "HDL", "triglycerides",
            "glucose", "blood sugar"
        ));

        // Procedures
        MEDICAL_TERMINOLOGY.put(EntityType.PROCEDURE, Arrays.asList(
            "colonoscopy", "endoscopy", "biopsy",
            "mammogram", "X-ray", "CT scan", "MRI",
            "cardiac catheterization", "angioplasty", "stent placement"
        ));

        // Vital Signs
        MEDICAL_TERMINOLOGY.put(EntityType.VITAL_SIGN, Arrays.asList(
            "blood pressure", "BP", "heart rate", "HR", "pulse",
            "temperature", "temp", "oxygen saturation", "O2 sat",
            "respiratory rate", "RR", "BMI", "weight", "height"
        ));
    }

    /**
     * Recognize medical entities in text.
     */
    public List<ExtractedEntity> recognize(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        List<ExtractedEntity> entities = new ArrayList<>();

        // Extract entities by type
        for (Map.Entry<EntityType, List<String>> entry : MEDICAL_TERMINOLOGY.entrySet()) {
            EntityType type = entry.getKey();
            for (String term : entry.getValue()) {
                entities.addAll(findEntities(text, term, type));
            }
        }

        // Extract ICD-10 codes
        entities.addAll(extractIcd10Codes(text));

        return entities;
    }

    /**
     * Recognize dosage patterns.
     */
    public List<ExtractedEntity> recognizeDosages(String text) {
        List<ExtractedEntity> dosages = new ArrayList<>();
        Matcher matcher = DOSAGE_PATTERN.matcher(text);

        while (matcher.find()) {
            ExtractedEntity entity = ExtractedEntity.builder()
                .type(EntityType.MEDICATION)
                .text(matcher.group())
                .startPosition(matcher.start())
                .endPosition(matcher.end())
                .confidence(0.9)
                .build();
            entity.getMetadata().put("dosage", matcher.group());
            dosages.add(entity);
        }

        return dosages;
    }

    /**
     * Recognize temporal expressions.
     */
    public List<ExtractedEntity> recognizeTemporalExpressions(String text) {
        List<ExtractedEntity> temporalEntities = new ArrayList<>();
        Pattern temporalPattern = Pattern.compile(
            "\\b(\\d+\\s*(day|week|month|year)s?\\s*ago|yesterday|today|tomorrow|next\\s*\\w+|last\\s*\\w+)\\b",
            Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = temporalPattern.matcher(text);
        while (matcher.find()) {
            ExtractedEntity entity = ExtractedEntity.builder()
                .type(EntityType.TEMPORAL_EXPRESSION)
                .text(matcher.group())
                .startPosition(matcher.start())
                .endPosition(matcher.end())
                .confidence(0.85)
                .build();
            temporalEntities.add(entity);
        }

        return temporalEntities;
    }

    /**
     * Find entities of a specific term.
     */
    private List<ExtractedEntity> findEntities(String text, String term, EntityType type) {
        List<ExtractedEntity> entities = new ArrayList<>();
        String lowerText = text.toLowerCase();
        String lowerTerm = term.toLowerCase();

        int index = 0;
        while ((index = lowerText.indexOf(lowerTerm, index)) != -1) {
            boolean negated = isNegated(text, index);

            ExtractedEntity entity = ExtractedEntity.builder()
                .type(type)
                .text(text.substring(index, index + term.length()))
                .startPosition(index)
                .endPosition(index + term.length())
                .confidence(negated ? 0.7 : 0.85)
                .build();

            entity.getMetadata().put("negated", negated);

            // Extract additional metadata based on type
            if (type == EntityType.MEDICATION) {
                extractMedicationMetadata(text, entity);
            }

            entities.add(entity);
            index += term.length();
        }

        return entities;
    }

    /**
     * Extract ICD-10 codes.
     */
    private List<ExtractedEntity> extractIcd10Codes(String text) {
        List<ExtractedEntity> codes = new ArrayList<>();
        Matcher matcher = ICD10_PATTERN.matcher(text);

        while (matcher.find()) {
            ExtractedEntity entity = ExtractedEntity.builder()
                .type(EntityType.DIAGNOSIS)
                .text(matcher.group())
                .startPosition(matcher.start())
                .endPosition(matcher.end())
                .confidence(0.95)
                .build();
            entity.getMetadata().put("icd10Code", matcher.group());
            codes.add(entity);
        }

        return codes;
    }

    /**
     * Check if entity is negated.
     */
    private boolean isNegated(String text, int position) {
        String precedingText = text.substring(Math.max(0, position - 30), position).toLowerCase();
        return precedingText.contains("no ") ||
               precedingText.contains("denies") ||
               precedingText.contains("negative for") ||
               precedingText.contains("without");
    }

    /**
     * Extract medication-specific metadata.
     */
    private void extractMedicationMetadata(String text, ExtractedEntity entity) {
        int start = entity.getStartPosition();
        int end = Math.min(entity.getEndPosition() + 50, text.length());
        String context = text.substring(start, end);

        // Extract dosage
        Matcher dosageMatcher = DOSAGE_PATTERN.matcher(context);
        if (dosageMatcher.find()) {
            entity.getMetadata().put("dosage", dosageMatcher.group());
        }

        // Extract frequency
        Matcher freqMatcher = FREQUENCY_PATTERN.matcher(context);
        if (freqMatcher.find()) {
            entity.getMetadata().put("frequency", freqMatcher.group());
        }
    }
}
