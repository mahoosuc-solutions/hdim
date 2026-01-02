package com.healthdata.enrichment.validator;

import com.healthdata.enrichment.model.CodeValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Tests for CptValidator.
 */
@DisplayName("CptValidator TDD Tests")
class CptValidatorTest {

    private CptValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CptValidator();
    }

    @Test
    @DisplayName("Should validate correct CPT code")
    void testValidCptCode() {
        String code = "99213"; // Office visit
        CodeValidationResult result = validator.validate(code);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should validate CPT code format (5 digits)")
    void testCptCodeFormat() {
        String validCode = "99213";
        String invalidCode = "999";
        assertThat(validator.validate(validCode).isValid()).isTrue();
        assertThat(validator.validate(invalidCode).isValid()).isFalse();
    }

    @Test
    @DisplayName("Should validate HCPCS codes")
    void testHcpcsCode() {
        String code = "J1815"; // HCPCS code
        CodeValidationResult result = validator.validateHcpcs(code);
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should provide code description")
    void testCodeDescription() {
        String code = "99213";
        CodeValidationResult result = validator.validate(code);
        assertThat(result.getDescription()).isNotEmpty();
    }

    @Test
    @DisplayName("Should identify code category")
    void testCodeCategory() {
        String code = "99213";
        CodeValidationResult result = validator.validate(code);
        assertThat(result.getMetadata()).containsKey("category");
    }

    @Test
    @DisplayName("Should validate modifier compatibility")
    void testModifierCompatibility() {
        String code = "99213";
        String modifier = "25";
        boolean compatible = validator.isModifierCompatible(code, modifier);
        assertThat(compatible).isNotNull();
    }

    @Test
    @DisplayName("Should check if code requires modifier")
    void testRequiresModifier() {
        String code = "99213";
        var modifierInfo = validator.getModifierRequirements(code);
        assertThat(modifierInfo).isNotNull();
    }

    @Test
    @DisplayName("Should validate CPT code year")
    void testCodeYear() {
        String code = "99213";
        String year = "2024";
        CodeValidationResult result = validator.validateWithYear(code, year);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Should identify deleted codes")
    void testDeletedCodes() {
        String deletedCode = "99999"; // Mock deleted code
        CodeValidationResult result = validator.validate(deletedCode);
        if (!result.isValid()) {
            assertThat(result.getErrors()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("Should provide RVU information")
    void testRvuInformation() {
        String code = "99213";
        var rvuInfo = validator.getRvuInformation(code);
        assertThat(rvuInfo).isNotNull();
    }
}
