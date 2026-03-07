package com.healthdata.corehiveadapter.integration;

import com.healthdata.corehiveadapter.model.CareGapScoringRequest;
import com.healthdata.corehiveadapter.service.PhiDeIdentificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end PHI de-identification validation.
 * Simulates the full pipeline: real patient data → de-identification →
 * scoring request → re-identification. Ensures no PHI leaks into
 * any field of the outbound request.
 */
@Tag("unit")
@DisplayName("E2E PHI De-Identification Pipeline")
class PhiDeIdentificationEndToEndTest {

    private PhiDeIdentificationService deIdService;

    @BeforeEach
    void setUp() {
        deIdService = new PhiDeIdentificationService();
    }

    @Test
    @DisplayName("Full pipeline: real IDs → synthetic → scoring request → re-identify")
    void fullPipeline_shouldDeIdentifyAndReIdentify() {
        // Real patient data (would come from HDIM patient-service)
        String realMrn = "MRN-JOHN-DOE-12345";
        String realGapId = "gap-HBA1C-patient123-2024";

        // Step 1: De-identify
        String syntheticPatient = deIdService.toSyntheticId(realMrn);
        String syntheticGap = deIdService.toSyntheticId(realGapId);

        // Step 2: Build scoring request with ONLY synthetic IDs
        CareGapScoringRequest request = CareGapScoringRequest.builder()
                .syntheticPatientId(syntheticPatient)
                .tenantId("tenant-acme-health")
                .careGaps(List.of(
                        CareGapScoringRequest.CareGapItem.builder()
                                .syntheticGapId(syntheticGap)
                                .measureId("HBA1C")
                                .measureCode("HEDIS-HBA1C")
                                .gapStatus("OPEN")
                                .daysSinceIdentified(45)
                                .complianceScore(0.35)
                                .build()))
                .build();

        // Step 3: Verify NO real identifiers in request
        String requestJson = request.toString();
        assertThat(requestJson).doesNotContain("JOHN");
        assertThat(requestJson).doesNotContain("DOE");
        assertThat(requestJson).doesNotContain("12345");
        assertThat(requestJson).doesNotContain("patient123");

        // Step 4: Verify synthetic IDs are UUID format
        assertThat(syntheticPatient).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(syntheticGap).matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

        // Step 5: Re-identify (happens in adapter after CoreHive responds)
        assertThat(deIdService.toRealId(syntheticPatient)).isEqualTo(realMrn);
        assertThat(deIdService.toRealId(syntheticGap)).isEqualTo(realGapId);
    }

    @Test
    @DisplayName("Batch de-identification maintains consistency across multiple patients")
    void batchDeIdentification_shouldMaintainConsistency() {
        String[] realIds = {"MRN-001", "MRN-002", "MRN-003", "MRN-001"};
        String[] syntheticIds = new String[4];

        for (int i = 0; i < realIds.length; i++) {
            syntheticIds[i] = deIdService.toSyntheticId(realIds[i]);
        }

        // Same real ID → same synthetic ID
        assertThat(syntheticIds[0]).isEqualTo(syntheticIds[3]);

        // Different real IDs → different synthetic IDs
        assertThat(syntheticIds[0]).isNotEqualTo(syntheticIds[1]);
        assertThat(syntheticIds[1]).isNotEqualTo(syntheticIds[2]);

        // All can be resolved back
        for (int i = 0; i < realIds.length; i++) {
            assertThat(deIdService.toRealId(syntheticIds[i])).isEqualTo(realIds[i]);
        }
    }

    @Test
    @DisplayName("PHI detector catches common healthcare identifiers")
    void phiDetector_shouldCatchHealthcareIdentifiers() {
        // These should all be flagged as potential PHI
        assertThat(deIdService.containsPotentialPhi("SSN: 123-45-6789")).isTrue();
        assertThat(deIdService.containsPotentialPhi("patient@clinic.org")).isTrue();
        assertThat(deIdService.containsPotentialPhi("(800) 555-1234")).isTrue();
        assertThat(deIdService.containsPotentialPhi("DOB: 03/15/1990")).isTrue();

        // These should NOT be flagged
        assertThat(deIdService.containsPotentialPhi("HEDIS-BCS-2024")).isFalse();
        assertThat(deIdService.containsPotentialPhi("ICD-10:E11.9")).isFalse();
        assertThat(deIdService.containsPotentialPhi("OPEN")).isFalse();
    }
}
