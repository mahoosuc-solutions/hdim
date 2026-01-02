package com.healthdata.enrichment.validator;

import com.healthdata.enrichment.model.CodeValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD Tests for ICD10Validator.
 *
 * Tests validation of ICD-10-CM and ICD-10-PCS codes.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ICD10Validator TDD Tests")
class ICD10ValidatorTest {

    private ICD10Validator validator;

    @BeforeEach
    void setUp() {
        validator = new ICD10Validator();
    }

    @Test
    @DisplayName("Should validate correct ICD-10-CM code")
    void testValidICD10CM() {
        // Given
        String code = "E11.9";

        // When
        CodeValidationResult result = validator.validate(code);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getCode()).isEqualTo("E11.9");
    }

    @Test
    @DisplayName("Should invalidate incorrect ICD-10-CM code")
    void testInvalidICD10CM() {
        // Given
        String code = "INVALID";

        // When
        CodeValidationResult result = validator.validate(code);

        // Then
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).isNotEmpty();
    }

    @Test
    @DisplayName("Should validate ICD-10-CM with full precision")
    void testICD10CMWithPrecision() {
        // Given
        String code = "E11.65";

        // When
        CodeValidationResult result = validator.validate(code);

        // Then
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should suggest similar codes for invalid code")
    void testSuggestSimilarCodes() {
        // Given
        String code = "E11.99"; // Invalid

        // When
        CodeValidationResult result = validator.validate(code);

        // Then
        assertThat(result.getSuggestions()).isNotEmpty();
        assertThat(result.getSuggestions()).anyMatch(s -> s.startsWith("E11"));
    }

    @Test
    @DisplayName("Should validate ICD-10-PCS code")
    void testValidICD10PCS() {
        // Given
        String code = "0DT60ZZ"; // PCS procedure code

        // When
        CodeValidationResult result = validator.validatePCS(code);

        // Then
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Should check code format")
    void testCodeFormat() {
        // Given
        String validFormat = "E11.9";
        String invalidFormat = "E11-9";

        // When
        CodeValidationResult result1 = validator.validate(validFormat);
        CodeValidationResult result2 = validator.validate(invalidFormat);

        // Then
        assertThat(result1.isValid()).isTrue();
        assertThat(result2.isValid()).isFalse();
    }

    @Test
    @DisplayName("Should handle null code")
    void testNullCode() {
        // Given/When/Then
        assertThatThrownBy(() -> validator.validate(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should handle empty code")
    void testEmptyCode() {
        // Given
        String code = "";

        // When
        CodeValidationResult result = validator.validate(code);

        // Then
        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("Should validate billable codes")
    void testBillableCodes() {
        // Given
        String billableCode = "E11.65";
        String nonBillableCode = "E11";

        // When
        CodeValidationResult result1 = validator.validate(billableCode);
        CodeValidationResult result2 = validator.validate(nonBillableCode);

        // Then
        assertThat(result1.isBillable()).isTrue();
        assertThat(result2.isBillable()).isFalse();
    }

    @Test
    @DisplayName("Should provide code description")
    void testCodeDescription() {
        // Given
        String code = "E11.9";

        // When
        CodeValidationResult result = validator.validate(code);

        // Then
        assertThat(result.getDescription()).isNotEmpty();
        assertThat(result.getDescription()).containsIgnoringCase("diabetes");
    }

    @Test
    @DisplayName("Should validate code hierarchy")
    void testCodeHierarchy() {
        // Given
        String code = "E11.65";

        // When
        CodeValidationResult result = validator.validate(code);

        // Then
        assertThat(result.getHierarchy()).isNotEmpty();
        assertThat(result.getHierarchy()).contains("E11");
    }

    @Test
    @DisplayName("Should check code existence in code set")
    void testCodeExistence() {
        // Given
        String existingCode = "E11.9";
        String nonExistingCode = "Z99.999";

        // When
        boolean exists1 = validator.codeExists(existingCode);
        boolean exists2 = validator.codeExists(nonExistingCode);

        // Then
        assertThat(exists1).isTrue();
        assertThat(exists2).isFalse();
    }

    @Test
    @DisplayName("Should validate with version")
    void testValidateWithVersion() {
        // Given
        String code = "E11.9";
        String version = "2024";

        // When
        CodeValidationResult result = validator.validateWithVersion(code, version);

        // Then
        assertThat(result.isValid()).isTrue();
        assertThat(result.getVersion()).isEqualTo(version);
    }

    @Test
    @DisplayName("Should handle case insensitivity")
    void testCaseInsensitivity() {
        // Given
        String upperCase = "E11.9";
        String lowerCase = "e11.9";

        // When
        CodeValidationResult result1 = validator.validate(upperCase);
        CodeValidationResult result2 = validator.validate(lowerCase);

        // Then
        assertThat(result1.isValid()).isEqualTo(result2.isValid());
    }

    @Test
    @DisplayName("Should validate batch of codes")
    void testBatchValidation() {
        // Given
        String[] codes = {"E11.9", "I10", "E78.5"};

        // When
        var results = validator.validateBatch(codes);

        // Then
        assertThat(results).hasSize(3);
        assertThat(results).allMatch(CodeValidationResult::isValid);
    }
}
