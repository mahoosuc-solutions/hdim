package com.healthdata.cql.models;

import static org.assertj.core.api.Assertions.assertThat;

import com.healthdata.common.validation.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CqlLibraryDescriptorTest {

    private static final String VALID_CQL = """
            library HEDIS_CBP version '1.0.0'

            using FHIR version '4.0.1'

            context Patient

            define IsAdult:
              AgeInYears() >= 18
            """;

    @Test
    @DisplayName("validate should succeed when header and content are well formed")
    void validate_shouldSucceedForWellFormedContent() {
        CqlLibraryDescriptor descriptor = new CqlLibraryDescriptor("HEDIS_CBP", "1.0.0", VALID_CQL);

        ValidationResult result = descriptor.validate();

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("validate should report missing header elements")
    void validate_shouldReportMissingHeaderElements() {
        String malformedCql = """
                define MissingHeader:
                  true
                """;

        CqlLibraryDescriptor descriptor = new CqlLibraryDescriptor("HEDIS_CBP", "1.0.0", malformedCql);

        ValidationResult result = descriptor.validate();

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(error -> error.contains("library header"));
    }

    @Test
    @DisplayName("validate should catch version mismatches")
    void validate_shouldCatchVersionMismatch() {
        String mismatched = """
                library HEDIS_CBP version '2.0.0'

                using FHIR version '4.0.1'
                """;

        CqlLibraryDescriptor descriptor = new CqlLibraryDescriptor("HEDIS_CBP", "1.0.0", mismatched);

        ValidationResult result = descriptor.validate();

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anyMatch(error -> error.contains("version"));
    }
}
