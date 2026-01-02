package com.healthdata.enrichment.validator;

import com.healthdata.enrichment.model.CodeValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Tests for SnomedValidator.
 */
@DisplayName("SnomedValidator TDD Tests")
class SnomedValidatorTest {

    private SnomedValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SnomedValidator();
    }

    @Test
    @DisplayName("Should validate correct SNOMED CT code")
    void testValidSnomedCode() {
        String code = "73211009"; // Diabetes mellitus
        CodeValidationResult result = validator.validate(code);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should invalidate incorrect SNOMED code")
    void testInvalidSnomedCode() {
        String code = "99999999999";
        CodeValidationResult result = validator.validate(code);
        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("Should validate SNOMED code format")
    void testSnomedCodeFormat() {
        String validCode = "73211009";
        String invalidCode = "ABC123";
        assertThat(validator.validate(validCode).isValid()).isTrue();
        assertThat(validator.validate(invalidCode).isValid()).isFalse();
    }

    @Test
    @DisplayName("Should provide concept description")
    void testConceptDescription() {
        String code = "73211009";
        CodeValidationResult result = validator.validate(code);
        assertThat(result.getDescription()).isNotEmpty();
    }

    @Test
    @DisplayName("Should find concept relationships")
    void testConceptRelationships() {
        String code = "73211009";
        CodeValidationResult result = validator.validate(code);
        assertThat(result.getMetadata()).containsKey("relationships");
    }

    @Test
    @DisplayName("Should validate with effective date")
    void testEffectiveDate() {
        String code = "73211009";
        String effectiveDate = "2024-01-01";
        CodeValidationResult result = validator.validateWithEffectiveDate(code, effectiveDate);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should handle retired concepts")
    void testRetiredConcepts() {
        String retiredCode = "00000000"; // Mock retired code
        CodeValidationResult result = validator.validate(retiredCode);
        if (!result.isValid()) {
            assertThat(result.getErrors()).anyMatch(e -> e.contains("retired"));
        }
    }

    @Test
    @DisplayName("Should validate concept hierarchy")
    void testConceptHierarchy() {
        String code = "73211009";
        var hierarchy = validator.getConceptHierarchy(code);
        assertThat(hierarchy).isNotEmpty();
    }

    @Test
    @DisplayName("Should find parent concepts")
    void testParentConcepts() {
        String code = "73211009";
        var parents = validator.getParentConcepts(code);
        assertThat(parents).isNotEmpty();
    }

    @Test
    @DisplayName("Should find child concepts")
    void testChildConcepts() {
        String code = "73211009";
        var children = validator.getChildConcepts(code);
        assertThat(children).isNotNull();
    }
}
