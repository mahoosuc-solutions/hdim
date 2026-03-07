package com.healthdata.corehiveadapter.service;

import com.healthdata.corehiveadapter.model.CareGapScoringRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PHI Boundary Tests for CoreHive Adapter.
 *
 * HIPAA CRITICAL: CoreHive must NEVER receive Protected Health Information.
 * These tests validate that the de-identification layer correctly strips
 * all identifiers and replaces them with synthetic IDs.
 */
@Tag("unit")
@DisplayName("PHI Boundary: CoreHive must never receive real patient data")
class PhiBoundaryTest {

    private PhiDeIdentificationService deIdService;

    @BeforeEach
    void setUp() {
        deIdService = new PhiDeIdentificationService();
    }

    @Test
    @DisplayName("Synthetic IDs must be UUID format, not derivable from real IDs")
    void syntheticIds_shouldBeUuidFormat() {
        String syntheticId = deIdService.toSyntheticId("MRN-12345678");
        assertThat(syntheticId).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(syntheticId).doesNotContain("MRN");
        assertThat(syntheticId).doesNotContain("12345678");
    }

    @Test
    @DisplayName("Different patients get different synthetic IDs")
    void differentPatients_getDifferentSyntheticIds() {
        String id1 = deIdService.toSyntheticId("patient-john-doe");
        String id2 = deIdService.toSyntheticId("patient-jane-smith");
        assertThat(id1).isNotEqualTo(id2);
    }

    @Test
    @DisplayName("Same patient always gets same synthetic ID within session")
    void samePatient_getsSameSyntheticId() {
        String id1 = deIdService.toSyntheticId("patient-123");
        String id2 = deIdService.toSyntheticId("patient-123");
        assertThat(id1).isEqualTo(id2);
    }

    @ParameterizedTest
    @DisplayName("PHI patterns must be detected and blocked")
    @ValueSource(strings = {
            "123-45-6789",               // SSN
            "john.doe@hospital.com",     // Email
            "(555) 123-4567",            // Phone
            "DOB: 01/15/1985",           // Date of birth
            "Patient SSN: 999-88-7777"   // SSN in context
    })
    void phiPatterns_shouldBeDetected(String phiValue) {
        assertThat(deIdService.containsPotentialPhi(phiValue)).isTrue();
    }

    @ParameterizedTest
    @DisplayName("Non-PHI values should pass through")
    @ValueSource(strings = {
            "550e8400-e29b-41d4-a716-446655440000",  // UUID
            "HEDIS-BCS-2024",                         // Measure code
            "ICD-10:E11.9",                           // Diagnosis code
            "CPT:99213",                              // Procedure code
            "OPEN",                                   // Status
            "42"                                      // Number
    })
    void nonPhiValues_shouldNotBeDetected(String safeValue) {
        assertThat(deIdService.containsPotentialPhi(safeValue)).isFalse();
    }

    @Test
    @DisplayName("CareGapScoringRequest must use synthetic IDs only")
    void scoringRequest_shouldOnlyContainSyntheticIds() {
        String realPatientId = "MRN-JDOE-123";
        String realGapId = "gap-real-456";

        String syntheticPatientId = deIdService.toSyntheticId(realPatientId);
        String syntheticGapId = deIdService.toSyntheticId(realGapId);

        CareGapScoringRequest request = CareGapScoringRequest.builder()
                .syntheticPatientId(syntheticPatientId)
                .tenantId("tenant-1")
                .careGaps(List.of(
                        CareGapScoringRequest.CareGapItem.builder()
                                .syntheticGapId(syntheticGapId)
                                .measureId("BCS")
                                .measureCode("HEDIS-BCS")
                                .gapStatus("OPEN")
                                .daysSinceIdentified(30)
                                .complianceScore(0.6)
                                .build()))
                .build();

        // Verify no real IDs in the request
        assertThat(request.getSyntheticPatientId()).doesNotContain("MRN");
        assertThat(request.getSyntheticPatientId()).doesNotContain("JDOE");
        assertThat(request.getCareGaps().get(0).getSyntheticGapId()).doesNotContain("real");

        // Verify we can resolve back
        assertThat(deIdService.toRealId(syntheticPatientId)).isEqualTo(realPatientId);
        assertThat(deIdService.toRealId(syntheticGapId)).isEqualTo(realGapId);
    }

    @Test
    @DisplayName("After clearing mappings, old synthetic IDs become unresolvable")
    void clearMappings_shouldInvalidateOldIds() {
        String syntheticId = deIdService.toSyntheticId("patient-sensitive");
        deIdService.clearMappings();

        // Old synthetic ID is now orphaned — cannot be resolved
        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> deIdService.toRealId(syntheticId));
    }
}
