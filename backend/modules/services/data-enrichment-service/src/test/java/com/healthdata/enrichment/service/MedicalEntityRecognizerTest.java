package com.healthdata.enrichment.service;

import com.healthdata.enrichment.model.EntityType;
import com.healthdata.enrichment.model.ExtractedEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Tests for MedicalEntityRecognizer.
 *
 * Tests Named Entity Recognition for medical terms.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MedicalEntityRecognizer TDD Tests")
class MedicalEntityRecognizerTest {

    private MedicalEntityRecognizer recognizer;

    @BeforeEach
    void setUp() {
        recognizer = new MedicalEntityRecognizer();
    }

    @Test
    @DisplayName("Should recognize diabetes as diagnosis")
    void testRecognizeDiabetes() {
        // Given
        String text = "Patient has Type 2 Diabetes Mellitus";

        // When
        List<ExtractedEntity> entities = recognizer.recognize(text);

        // Then
        assertThat(entities).isNotEmpty();
        assertThat(entities.get(0).getType()).isEqualTo(EntityType.DIAGNOSIS);
    }

    @Test
    @DisplayName("Should recognize medication names")
    void testRecognizeMedications() {
        // Given
        String text = "Metformin and Lisinopril prescribed";

        // When
        List<ExtractedEntity> entities = recognizer.recognize(text);

        // Then
        assertThat(entities).hasSizeGreaterThanOrEqualTo(2);
        assertThat(entities).allMatch(e -> e.getType() == EntityType.MEDICATION);
    }

    @Test
    @DisplayName("Should recognize lab test names")
    void testRecognizeLabTests() {
        // Given
        String text = "HbA1c and Creatinine ordered";

        // When
        List<ExtractedEntity> entities = recognizer.recognize(text);

        // Then
        assertThat(entities).isNotEmpty();
        assertThat(entities).anyMatch(e -> e.getType() == EntityType.LAB_RESULT);
    }

    @Test
    @DisplayName("Should recognize procedure names")
    void testRecognizeProcedures() {
        // Given
        String text = "Colonoscopy performed";

        // When
        List<ExtractedEntity> entities = recognizer.recognize(text);

        // Then
        assertThat(entities).isNotEmpty();
        assertThat(entities.get(0).getType()).isEqualTo(EntityType.PROCEDURE);
    }

    @Test
    @DisplayName("Should assign confidence scores")
    void testConfidenceScores() {
        // Given
        String text = "Diabetes mellitus";

        // When
        List<ExtractedEntity> entities = recognizer.recognize(text);

        // Then
        assertThat(entities).allMatch(e -> e.getConfidence() > 0.5);
    }

    @Test
    @DisplayName("Should handle abbreviations")
    void testAbbreviations() {
        // Given
        String text = "DM Type 2, HTN, HLD";

        // When
        List<ExtractedEntity> entities = recognizer.recognize(text);

        // Then
        assertThat(entities).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should recognize vital signs")
    void testRecognizeVitals() {
        // Given
        String text = "Blood pressure 120/80, heart rate 72";

        // When
        List<ExtractedEntity> entities = recognizer.recognize(text);

        // Then
        assertThat(entities).anyMatch(e -> e.getType() == EntityType.VITAL_SIGN);
    }

    @Test
    @DisplayName("Should handle case insensitivity")
    void testCaseInsensitivity() {
        // Given
        String text1 = "DIABETES";
        String text2 = "diabetes";

        // When
        List<ExtractedEntity> entities1 = recognizer.recognize(text1);
        List<ExtractedEntity> entities2 = recognizer.recognize(text2);

        // Then
        assertThat(entities1).hasSameSizeAs(entities2);
    }

    @Test
    @DisplayName("Should recognize ICD-10 code patterns")
    void testICD10Patterns() {
        // Given
        String text = "E11.9 and I10";

        // When
        List<ExtractedEntity> entities = recognizer.recognize(text);

        // Then
        assertThat(entities).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should recognize dosage patterns")
    void testDosagePatterns() {
        // Given
        String text = "500mg, 10mg, 1000mcg";

        // When
        List<ExtractedEntity> entities = recognizer.recognizeDosages(text);

        // Then
        assertThat(entities).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should handle empty text")
    void testEmptyText() {
        // Given
        String text = "";

        // When
        List<ExtractedEntity> entities = recognizer.recognize(text);

        // Then
        assertThat(entities).isEmpty();
    }

    @Test
    @DisplayName("Should recognize temporal expressions")
    void testTemporalExpressions() {
        // Given
        String text = "3 months ago, next week, yesterday";

        // When
        List<ExtractedEntity> entities = recognizer.recognizeTemporalExpressions(text);

        // Then
        assertThat(entities).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should recognize negation contexts")
    void testNegationDetection() {
        // Given
        String text = "No diabetes, denies fever";

        // When
        List<ExtractedEntity> entities = recognizer.recognize(text);

        // Then
        assertThat(entities).anyMatch(e ->
            Boolean.TRUE.equals(e.getMetadata().get("negated")));
    }

    @Test
    @DisplayName("Should recognize anatomical locations")
    void testAnatomicalLocations() {
        // Given
        String text = "Left knee pain, right shoulder";

        // When
        List<ExtractedEntity> entities = recognizer.recognize(text);

        // Then
        assertThat(entities).anyMatch(e ->
            e.getMetadata().containsKey("anatomicalLocation"));
    }

    @Test
    @DisplayName("Should recognize severity indicators")
    void testSeverityIndicators() {
        // Given
        String text = "Severe pain, mild hypertension, moderate diabetes";

        // When
        List<ExtractedEntity> entities = recognizer.recognize(text);

        // Then
        assertThat(entities).anyMatch(e ->
            e.getMetadata().containsKey("severity"));
    }
}
