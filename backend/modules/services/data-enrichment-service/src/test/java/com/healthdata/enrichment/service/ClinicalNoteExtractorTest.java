package com.healthdata.enrichment.service;

import com.healthdata.enrichment.model.EntityType;
import com.healthdata.enrichment.model.ExtractedEntity;
import com.healthdata.enrichment.model.ExtractionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Tests for ClinicalNoteExtractor.
 *
 * Tests extraction of structured data from clinical notes including:
 * - Diagnoses
 * - Medications
 * - Procedures
 * - Lab results
 * - Vital signs
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClinicalNoteExtractor TDD Tests")
class ClinicalNoteExtractorTest {

    private ClinicalNoteExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ClinicalNoteExtractor(new MedicalEntityRecognizer());
    }

    @Test
    @DisplayName("Should extract diabetes diagnosis from clinical note")
    void testExtractDiabetesDiagnosis() {
        // Given
        String clinicalNote = "Patient presents with Type 2 Diabetes Mellitus. " +
                "HbA1c: 8.5%. Started on Metformin 500mg BID.";

        // When
        ExtractionResult result = extractor.extract(clinicalNote);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEntities()).isNotEmpty();

        List<ExtractedEntity> diagnoses = result.getEntitiesByType(EntityType.DIAGNOSIS);
        assertThat(diagnoses).isNotEmpty();
        assertThat(diagnoses).anyMatch(e ->
            e.getText().toLowerCase().contains("diabetes"));
    }

    @Test
    @DisplayName("Should extract medication with dosage")
    void testExtractMedicationWithDosage() {
        // Given
        String clinicalNote = "Prescribed Lisinopril 10mg daily for hypertension.";

        // When
        ExtractionResult result = extractor.extract(clinicalNote);

        // Then
        List<ExtractedEntity> medications = result.getEntitiesByType(EntityType.MEDICATION);
        assertThat(medications).isNotEmpty();
        assertThat(medications.get(0).getText()).containsIgnoringCase("Lisinopril");
        assertThat(medications.get(0).getMetadata()).containsKey("dosage");
    }

    @Test
    @DisplayName("Should extract multiple medications from note")
    void testExtractMultipleMedications() {
        // Given
        String clinicalNote = "Current medications: Metformin 1000mg BID, " +
                "Lisinopril 20mg daily, Atorvastatin 40mg HS.";

        // When
        ExtractionResult result = extractor.extract(clinicalNote);

        // Then
        List<ExtractedEntity> medications = result.getEntitiesByType(EntityType.MEDICATION);
        assertThat(medications).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should extract procedure from note")
    void testExtractProcedure() {
        // Given
        String clinicalNote = "Patient underwent colonoscopy with biopsy. " +
                "Procedure completed without complications.";

        // When
        ExtractionResult result = extractor.extract(clinicalNote);

        // Then
        List<ExtractedEntity> procedures = result.getEntitiesByType(EntityType.PROCEDURE);
        assertThat(procedures).isNotEmpty();
        assertThat(procedures).anyMatch(e ->
            e.getText().toLowerCase().contains("colonoscopy"));
    }

    @Test
    @DisplayName("Should extract lab results with values")
    void testExtractLabResults() {
        // Given
        String clinicalNote = "Lab results: HbA1c 7.2%, Creatinine 1.1 mg/dL, " +
                "eGFR 68 mL/min.";

        // When
        ExtractionResult result = extractor.extract(clinicalNote);

        // Then
        List<ExtractedEntity> labs = result.getEntitiesByType(EntityType.LAB_RESULT);
        assertThat(labs).hasSizeGreaterThanOrEqualTo(2);
        assertThat(labs).anyMatch(e -> e.getText().contains("HbA1c"));
    }

    @Test
    @DisplayName("Should handle empty clinical note")
    void testEmptyClinicalNote() {
        // Given
        String clinicalNote = "";

        // When
        ExtractionResult result = extractor.extract(clinicalNote);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEntities()).isEmpty();
    }

    @Test
    @DisplayName("Should handle null clinical note")
    void testNullClinicalNote() {
        // Given/When/Then
        assertThatThrownBy(() -> extractor.extract(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Clinical note cannot be null");
    }

    @Test
    @DisplayName("Should extract vital signs")
    void testExtractVitalSigns() {
        // Given
        String clinicalNote = "Vitals: BP 142/88, HR 78, Temp 98.6F, " +
                "O2 Sat 97% on room air.";

        // When
        ExtractionResult result = extractor.extract(clinicalNote);

        // Then
        List<ExtractedEntity> vitals = result.getEntitiesByType(EntityType.VITAL_SIGN);
        assertThat(vitals).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Should assign confidence scores to extracted entities")
    void testConfidenceScores() {
        // Given
        String clinicalNote = "Diagnosis: Type 2 Diabetes Mellitus (E11.9)";

        // When
        ExtractionResult result = extractor.extract(clinicalNote);

        // Then
        assertThat(result.getEntities()).allMatch(e ->
            e.getConfidence() >= 0.0 && e.getConfidence() <= 1.0);
    }

    @Test
    @DisplayName("Should extract ICD-10 codes from note")
    void testExtractICD10Codes() {
        // Given
        String clinicalNote = "1. Type 2 Diabetes Mellitus (E11.9)\n" +
                "2. Essential Hypertension (I10)\n" +
                "3. Hyperlipidemia (E78.5)";

        // When
        ExtractionResult result = extractor.extract(clinicalNote);

        // Then
        List<ExtractedEntity> diagnoses = result.getEntitiesByType(EntityType.DIAGNOSIS);
        assertThat(diagnoses).hasSizeGreaterThanOrEqualTo(3);
        assertThat(diagnoses).anyMatch(e ->
            e.getMetadata().containsKey("icd10Code"));
    }

    @Test
    @DisplayName("Should extract date information")
    void testExtractDateInformation() {
        // Given
        String clinicalNote = "Patient seen on 01/15/2024 for follow-up. " +
                "Last visit was 12/01/2023.";

        // When
        ExtractionResult result = extractor.extract(clinicalNote);

        // Then
        assertThat(result.getMetadata()).containsKey("dates");
    }

    @Test
    @DisplayName("Should handle complex multi-paragraph note")
    void testComplexNote() {
        // Given
        String clinicalNote = """
                Chief Complaint: Poorly controlled diabetes

                History: 58-year-old male with Type 2 DM, HTN, HLD.
                Current meds: Metformin 1000mg BID, Lisinopril 20mg daily.

                Physical Exam: BP 145/92, HR 82, BMI 32.4

                Assessment/Plan:
                1. Type 2 DM - A1c 8.9%, will add Jardiance 10mg daily
                2. HTN - increase Lisinopril to 40mg daily
                3. Labs: Recheck A1c, CMP in 3 months
                """;

        // When
        ExtractionResult result = extractor.extract(clinicalNote);

        // Then
        assertThat(result.getEntities()).hasSizeGreaterThan(5);
        assertThat(result.getEntitiesByType(EntityType.DIAGNOSIS)).isNotEmpty();
        assertThat(result.getEntitiesByType(EntityType.MEDICATION)).isNotEmpty();
        assertThat(result.getEntitiesByType(EntityType.VITAL_SIGN)).isNotEmpty();
    }

    @Test
    @DisplayName("Should extract allergies")
    void testExtractAllergies() {
        // Given
        String clinicalNote = "Allergies: Penicillin (rash), Sulfa drugs (anaphylaxis)";

        // When
        ExtractionResult result = extractor.extract(clinicalNote);

        // Then
        List<ExtractedEntity> allergies = result.getEntitiesByType(EntityType.ALLERGY);
        assertThat(allergies).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Should preserve original text positions")
    void testTextPositions() {
        // Given
        String clinicalNote = "Patient has diabetes mellitus.";

        // When
        ExtractionResult result = extractor.extract(clinicalNote);

        // Then
        assertThat(result.getEntities()).allMatch(e ->
            e.getStartPosition() >= 0 &&
            e.getEndPosition() > e.getStartPosition());
    }

    @Test
    @DisplayName("Should extract dosage frequency")
    void testExtractDosageFrequency() {
        // Given
        String clinicalNote = "Metformin 500mg BID, Aspirin 81mg daily, " +
                "Insulin 10 units TID";

        // When
        ExtractionResult result = extractor.extract(clinicalNote);

        // Then
        List<ExtractedEntity> medications = result.getEntitiesByType(EntityType.MEDICATION);
        assertThat(medications).allMatch(m ->
            m.getMetadata().containsKey("frequency"));
    }

    @Test
    @DisplayName("Should handle misspelled medical terms")
    void testMisspelledTerms() {
        // Given - Note: Real implementation would use fuzzy matching
        String clinicalNote = "Patient has dibetes and hypertension";

        // When
        ExtractionResult result = extractor.extract(clinicalNote);

        // Then
        assertThat(result).isNotNull();
        // In real implementation, should still extract with lower confidence
    }

    @Test
    @DisplayName("Should extract negated findings")
    void testNegatedFindings() {
        // Given
        String clinicalNote = "No evidence of pneumonia. Patient denies chest pain.";

        // When
        ExtractionResult result = extractor.extract(clinicalNote);

        // Then
        List<ExtractedEntity> entities = result.getEntities();
        assertThat(entities).anyMatch(e ->
            Boolean.TRUE.equals(e.getMetadata().get("negated")));
    }

    @Test
    @DisplayName("Should extract family history")
    void testExtractFamilyHistory() {
        // Given
        String clinicalNote = "Family History: Father had MI at age 55, " +
                "mother with Type 2 DM";

        // When
        ExtractionResult result = extractor.extract(clinicalNote);

        // Then
        List<ExtractedEntity> familyHistory = result.getEntitiesByType(EntityType.FAMILY_HISTORY);
        assertThat(familyHistory).isNotEmpty();
    }

    @Test
    @DisplayName("Should calculate overall extraction confidence")
    void testOverallConfidence() {
        // Given
        String clinicalNote = "Type 2 Diabetes Mellitus, Hypertension";

        // When
        ExtractionResult result = extractor.extract(clinicalNote);

        // Then
        assertThat(result.getOverallConfidence()).isBetween(0.0, 1.0);
    }

    @Test
    @DisplayName("Should extract temporal information")
    void testTemporalExtraction() {
        // Given
        String clinicalNote = "Started Metformin 3 months ago. " +
                "Will recheck labs in 6 weeks.";

        // When
        ExtractionResult result = extractor.extract(clinicalNote);

        // Then
        assertThat(result.getMetadata()).containsKey("temporalExpressions");
    }

    @Test
    @DisplayName("Should support tenant isolation")
    void testTenantIsolation() {
        // Given
        String clinicalNote = "Type 2 Diabetes";
        String tenantId = "tenant-123";

        // When
        ExtractionResult result = extractor.extractWithTenant(clinicalNote, tenantId);

        // Then
        assertThat(result.getTenantId()).isEqualTo(tenantId);
    }
}
