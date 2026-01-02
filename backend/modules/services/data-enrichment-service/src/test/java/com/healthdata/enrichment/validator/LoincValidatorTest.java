package com.healthdata.enrichment.validator;

import com.healthdata.enrichment.model.CodeValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Tests for LoincValidator.
 */
@DisplayName("LoincValidator TDD Tests")
class LoincValidatorTest {

    private LoincValidator validator;

    @BeforeEach
    void setUp() {
        validator = new LoincValidator();
    }

    @Test
    @DisplayName("Should validate correct LOINC code")
    void testValidLoincCode() {
        String code = "4548-4"; // HbA1c
        CodeValidationResult result = validator.validate(code);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should validate LOINC code format")
    void testLoincCodeFormat() {
        String validCode = "4548-4";
        String invalidCode = "INVALID";
        assertThat(validator.validate(validCode).isValid()).isTrue();
        assertThat(validator.validate(invalidCode).isValid()).isFalse();
    }

    @Test
    @DisplayName("Should provide test description")
    void testDescription() {
        String code = "4548-4";
        CodeValidationResult result = validator.validate(code);
        assertThat(result.getDescription()).containsIgnoringCase("hemoglobin");
    }

    @Test
    @DisplayName("Should identify test component")
    void testComponent() {
        String code = "4548-4";
        var metadata = validator.validate(code).getMetadata();
        assertThat(metadata).containsKey("component");
    }

    @Test
    @DisplayName("Should identify test system")
    void testSystem() {
        String code = "4548-4";
        var metadata = validator.validate(code).getMetadata();
        assertThat(metadata).containsKey("system");
    }

    @Test
    @DisplayName("Should identify scale type")
    void testScaleType() {
        String code = "4548-4";
        var metadata = validator.validate(code).getMetadata();
        assertThat(metadata).containsKey("scaleType");
    }

    @Test
    @DisplayName("Should provide reference range")
    void testReferenceRange() {
        String code = "4548-4";
        var rangeInfo = validator.getReferenceRange(code);
        assertThat(rangeInfo).isNotNull();
    }

    @Test
    @DisplayName("Should identify units of measure")
    void testUnitsOfMeasure() {
        String code = "4548-4";
        var units = validator.getUnitsOfMeasure(code);
        assertThat(units).isNotEmpty();
    }

    @Test
    @DisplayName("Should find related LOINC codes")
    void testRelatedCodes() {
        String code = "4548-4";
        var related = validator.getRelatedCodes(code);
        assertThat(related).isNotNull();
    }

    @Test
    @DisplayName("Should validate deprecated codes")
    void testDeprecatedCodes() {
        String deprecatedCode = "00000-0"; // Mock deprecated code
        CodeValidationResult result = validator.validate(deprecatedCode);
        if (!result.isValid()) {
            assertThat(result.getWarnings()).isNotNull();
        }
    }
}
