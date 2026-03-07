package com.healthdata.corehiveadapter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class PhiDeIdentificationServiceTest {

    private PhiDeIdentificationService service;

    @BeforeEach
    void setUp() {
        service = new PhiDeIdentificationService();
    }

    @Test
    void toSyntheticId_shouldGenerateConsistentMapping() {
        String realId = "patient-123";
        String syntheticId1 = service.toSyntheticId(realId);
        String syntheticId2 = service.toSyntheticId(realId);

        assertThat(syntheticId1).isNotEqualTo(realId);
        assertThat(syntheticId1).isEqualTo(syntheticId2);
    }

    @Test
    void toSyntheticId_shouldGenerateUniqueIdsForDifferentPatients() {
        String synthetic1 = service.toSyntheticId("patient-1");
        String synthetic2 = service.toSyntheticId("patient-2");

        assertThat(synthetic1).isNotEqualTo(synthetic2);
    }

    @Test
    void toRealId_shouldResolveBackToOriginal() {
        String realId = "patient-abc";
        String syntheticId = service.toSyntheticId(realId);
        String resolved = service.toRealId(syntheticId);

        assertThat(resolved).isEqualTo(realId);
    }

    @Test
    void toRealId_shouldThrowForUnknownSyntheticId() {
        assertThatThrownBy(() -> service.toRealId("unknown-synthetic-id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown synthetic ID");
    }

    @Test
    void containsPotentialPhi_shouldDetectSsn() {
        assertThat(service.containsPotentialPhi("SSN: 123-45-6789")).isTrue();
    }

    @Test
    void containsPotentialPhi_shouldDetectEmail() {
        assertThat(service.containsPotentialPhi("john.doe@example.com")).isTrue();
    }

    @Test
    void containsPotentialPhi_shouldDetectPhone() {
        assertThat(service.containsPotentialPhi("(555) 123-4567")).isTrue();
    }

    @Test
    void containsPotentialPhi_shouldDetectDateOfBirth() {
        assertThat(service.containsPotentialPhi("DOB: 01/15/1985")).isTrue();
    }

    @Test
    void containsPotentialPhi_shouldNotFlagUuids() {
        assertThat(service.containsPotentialPhi("550e8400-e29b-41d4-a716-446655440000")).isFalse();
    }

    @Test
    void containsPotentialPhi_shouldNotFlagMeasureCodes() {
        assertThat(service.containsPotentialPhi("HEDIS-BCS-2024")).isFalse();
    }

    @Test
    void containsPotentialPhi_shouldHandleNull() {
        assertThat(service.containsPotentialPhi(null)).isFalse();
    }

    @Test
    void clearMappings_shouldRemoveAllMappings() {
        String originalSynthetic = service.toSyntheticId("patient-1");
        service.toSyntheticId("patient-2");
        service.clearMappings();

        // Old synthetic ID should no longer resolve
        assertThatThrownBy(() -> service.toRealId(originalSynthetic))
                .isInstanceOf(IllegalArgumentException.class);

        // New mapping should be created with a different synthetic ID
        String newSynthetic = service.toSyntheticId("patient-1");
        assertThat(newSynthetic).isNotEqualTo(originalSynthetic);
        assertThat(service.toRealId(newSynthetic)).isEqualTo("patient-1");
    }
}
