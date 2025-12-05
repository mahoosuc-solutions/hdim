package com.healthdata.enrichment.service;

import com.healthdata.enrichment.model.EntityType;
import com.healthdata.enrichment.model.ExtractedEntity;
import com.healthdata.enrichment.model.ExtractionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extract structured data from clinical notes using NLP.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ClinicalNoteExtractor {

    private final MedicalEntityRecognizer recognizer;

    private static final Pattern DATE_PATTERN = Pattern.compile(
        "\\d{1,2}/\\d{1,2}/\\d{2,4}|\\d{4}-\\d{2}-\\d{2}"
    );

    private static final Pattern VITAL_PATTERN = Pattern.compile(
        "(?:BP|blood pressure)\\s*:?\\s*(\\d{2,3})/(\\d{2,3})|" +
        "(?:HR|heart rate)\\s*:?\\s*(\\d{2,3})|" +
        "(?:temp|temperature)\\s*:?\\s*(\\d{2,3}\\.?\\d?)|" +
        "(?:O2 sat|oxygen saturation)\\s*:?\\s*(\\d{2,3})%",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Extract entities from clinical note.
     */
    public ExtractionResult extract(String clinicalNote) {
        if (clinicalNote == null) {
            throw new IllegalArgumentException("Clinical note cannot be null");
        }

        if (clinicalNote.isEmpty()) {
            return ExtractionResult.builder()
                .overallConfidence(0.0)
                .extractedAt(LocalDateTime.now())
                .build();
        }

        List<ExtractedEntity> allEntities = new ArrayList<>();

        // Use recognizer for standard entities
        allEntities.addAll(recognizer.recognize(clinicalNote));

        // Extract vital signs
        allEntities.addAll(extractVitalSigns(clinicalNote));

        // Extract allergies
        allEntities.addAll(extractAllergies(clinicalNote));

        // Extract family history
        allEntities.addAll(extractFamilyHistory(clinicalNote));

        // Calculate overall confidence
        double overallConfidence = calculateOverallConfidence(allEntities);

        // Extract metadata
        Map<String, Object> metadata = extractMetadata(clinicalNote);

        return ExtractionResult.builder()
            .entities(allEntities)
            .overallConfidence(overallConfidence)
            .metadata(metadata)
            .extractedAt(LocalDateTime.now())
            .build();
    }

    /**
     * Extract with tenant isolation.
     */
    public ExtractionResult extractWithTenant(String clinicalNote, String tenantId) {
        ExtractionResult result = extract(clinicalNote);
        result.setTenantId(tenantId);
        result.setId(UUID.randomUUID().toString());
        return result;
    }

    /**
     * Extract vital signs from note.
     */
    private List<ExtractedEntity> extractVitalSigns(String text) {
        List<ExtractedEntity> vitals = new ArrayList<>();
        Matcher matcher = VITAL_PATTERN.matcher(text);

        while (matcher.find()) {
            String vitalText = matcher.group();
            ExtractedEntity entity = ExtractedEntity.builder()
                .type(EntityType.VITAL_SIGN)
                .text(vitalText)
                .startPosition(matcher.start())
                .endPosition(matcher.end())
                .confidence(0.9)
                .build();

            // Add specific vital sign type
            if (vitalText.toLowerCase().contains("bp") || vitalText.toLowerCase().contains("blood pressure")) {
                entity.getMetadata().put("vitalType", "blood_pressure");
                if (matcher.group(1) != null && matcher.group(2) != null) {
                    entity.getMetadata().put("systolic", matcher.group(1));
                    entity.getMetadata().put("diastolic", matcher.group(2));
                }
            } else if (vitalText.toLowerCase().contains("hr") || vitalText.toLowerCase().contains("heart rate")) {
                entity.getMetadata().put("vitalType", "heart_rate");
            }

            vitals.add(entity);
        }

        return vitals;
    }

    /**
     * Extract allergies.
     */
    private List<ExtractedEntity> extractAllergies(String text) {
        List<ExtractedEntity> allergies = new ArrayList<>();
        Pattern allergyPattern = Pattern.compile(
            "(?:allergies?|allergic to)\\s*:?\\s*([^\\n\\.]+)",
            Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = allergyPattern.matcher(text);
        while (matcher.find()) {
            String allergyText = matcher.group(1).trim();
            // Split by commas for multiple allergies
            String[] individualAllergies = allergyText.split(",");

            for (String allergy : individualAllergies) {
                allergy = allergy.trim();
                if (!allergy.isEmpty() && !allergy.equalsIgnoreCase("none") && !allergy.equalsIgnoreCase("nkda")) {
                    ExtractedEntity entity = ExtractedEntity.builder()
                        .type(EntityType.ALLERGY)
                        .text(allergy)
                        .startPosition(matcher.start())
                        .endPosition(matcher.end())
                        .confidence(0.85)
                        .build();
                    allergies.add(entity);
                }
            }
        }

        return allergies;
    }

    /**
     * Extract family history.
     */
    private List<ExtractedEntity> extractFamilyHistory(String text) {
        List<ExtractedEntity> familyHistory = new ArrayList<>();
        Pattern fhPattern = Pattern.compile(
            "(?:family history|FH)\\s*:?\\s*([^\\n\\.]+)",
            Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = fhPattern.matcher(text);
        while (matcher.find()) {
            String fhText = matcher.group(1).trim();
            ExtractedEntity entity = ExtractedEntity.builder()
                .type(EntityType.FAMILY_HISTORY)
                .text(fhText)
                .startPosition(matcher.start())
                .endPosition(matcher.end())
                .confidence(0.8)
                .build();
            familyHistory.add(entity);
        }

        return familyHistory;
    }

    /**
     * Calculate overall confidence score.
     */
    private double calculateOverallConfidence(List<ExtractedEntity> entities) {
        if (entities.isEmpty()) {
            return 0.0;
        }

        double sum = entities.stream()
            .mapToDouble(ExtractedEntity::getConfidence)
            .sum();

        return sum / entities.size();
    }

    /**
     * Extract metadata from note.
     */
    private Map<String, Object> extractMetadata(String text) {
        Map<String, Object> metadata = new HashMap<>();

        // Extract dates
        List<String> dates = new ArrayList<>();
        Matcher dateMatcher = DATE_PATTERN.matcher(text);
        while (dateMatcher.find()) {
            dates.add(dateMatcher.group());
        }
        if (!dates.isEmpty()) {
            metadata.put("dates", dates);
        }

        // Extract temporal expressions
        List<ExtractedEntity> temporalExpressions = recognizer.recognizeTemporalExpressions(text);
        if (!temporalExpressions.isEmpty()) {
            metadata.put("temporalExpressions", temporalExpressions);
        }

        // Check data freshness
        metadata.put("dataFreshness", assessDataFreshness(dates));

        return metadata;
    }

    /**
     * Assess data freshness based on dates found.
     */
    private String assessDataFreshness(List<String> dates) {
        if (dates.isEmpty()) {
            return "unknown";
        }
        // Simplified - in production would parse dates and compare to current date
        return "recent";
    }
}
