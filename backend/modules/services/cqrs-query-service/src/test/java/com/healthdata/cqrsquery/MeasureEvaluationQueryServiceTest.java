package com.healthdata.cqrsquery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for MeasureEvaluationQueryService
 *
 * Tests quality measure evaluation queries:
 * - Retrieve measure scores for patient
 * - Filter measures by status (met/not met/numerator/denominator)
 * - Aggregated scoring across cohorts
 * - Measure performance comparisons
 * - Temporal measure snapshots
 * - Caching with HIPAA compliance
 *
 * HEDIS measures are healthcare quality indicators tracked by payers.
 * Examples: colorectal cancer screening, diabetes control, medication adherence.
 */
@DisplayName("MeasureEvaluationQueryService Tests")
class MeasureEvaluationQueryServiceTest {

    private MeasureEvaluationQueryService measureQueryService;
    private MockMeasureProjectionStore mockMeasureStore;
    private MockCacheStore mockCacheStore;

    @BeforeEach
    void setup() {
        mockMeasureStore = new MockMeasureProjectionStore();
        mockCacheStore = new MockCacheStore();
        measureQueryService = new MeasureEvaluationQueryService(mockMeasureStore, mockCacheStore);
    }

    // ===== Individual Patient Measure Tests =====

    @Test
    @DisplayName("Should retrieve measure scores for patient")
    void testGetMeasureScoresForPatient() {
        // Given: Patient with evaluated measures
        String tenantId = "TENANT-001";
        String patientId = "PATIENT-123";

        mockMeasureStore.addMeasureEvaluation(tenantId, patientId, "COL", "MET", 1.0f);
        mockMeasureStore.addMeasureEvaluation(tenantId, patientId, "DM", "NOT_MET", 0.0f);
        mockMeasureStore.addMeasureEvaluation(tenantId, patientId, "HTN", "MET", 1.0f);

        // When: Retrieving measure scores
        List<MeasureEvaluationResult> scores = measureQueryService.getMeasureScores(tenantId, patientId);

        // Then: Should return all measures for patient
        assertThat(scores).hasSize(3);
    }

    @Test
    @DisplayName("Should indicate measure met status")
    void testMeasureMet() {
        // Given: Measure evaluation
        String tenantId = "TENANT-001";
        String patientId = "PATIENT-123";

        mockMeasureStore.addMeasureEvaluation(tenantId, patientId, "COL", "MET", 1.0f);

        // When: Retrieving measure
        MeasureEvaluationResult measure = measureQueryService.getMeasure(tenantId, patientId, "COL");

        // Then: Should show as met
        assertThat(measure.getStatus()).isEqualTo("MET");
        assertThat(measure.isCompliant()).isTrue();
    }

    @Test
    @DisplayName("Should indicate measure not met status")
    void testMeasureNotMet() {
        // Given: Measure not met
        String tenantId = "TENANT-001";
        String patientId = "PATIENT-123";

        mockMeasureStore.addMeasureEvaluation(tenantId, patientId, "DM", "NOT_MET", 0.0f);

        // When: Retrieving measure
        MeasureEvaluationResult measure = measureQueryService.getMeasure(tenantId, patientId, "DM");

        // Then: Should show as not met
        assertThat(measure.getStatus()).isEqualTo("NOT_MET");
        assertThat(measure.isCompliant()).isFalse();
    }

    // ===== Filtering by Status =====

    @Test
    @DisplayName("Should filter measures by met status")
    void testFilterByMet() {
        // Given: Mix of met and not-met measures
        String tenantId = "TENANT-001";
        String patientId = "PATIENT-123";

        mockMeasureStore.addMeasureEvaluation(tenantId, patientId, "COL", "MET", 1.0f);
        mockMeasureStore.addMeasureEvaluation(tenantId, patientId, "DM", "NOT_MET", 0.0f);
        mockMeasureStore.addMeasureEvaluation(tenantId, patientId, "HTN", "MET", 1.0f);

        // When: Filtering for met measures
        List<MeasureEvaluationResult> metMeasures = measureQueryService.getMeasuresByStatus(tenantId, patientId, "MET");

        // Then: Should return only met measures
        assertThat(metMeasures).hasSize(2).allMatch(MeasureEvaluationResult::isCompliant);
    }

    @Test
    @DisplayName("Should filter measures by not-met status")
    void testFilterByNotMet() {
        // Given: Mix of met and not-met measures
        String tenantId = "TENANT-001";
        String patientId = "PATIENT-123";

        mockMeasureStore.addMeasureEvaluation(tenantId, patientId, "COL", "MET", 1.0f);
        mockMeasureStore.addMeasureEvaluation(tenantId, patientId, "DM", "NOT_MET", 0.0f);

        // When: Filtering for not-met
        List<MeasureEvaluationResult> unmetMeasures = measureQueryService.getMeasuresByStatus(tenantId, patientId, "NOT_MET");

        // Then: Should return only not-met
        assertThat(unmetMeasures).hasSize(1).allMatch(m -> !m.isCompliant());
    }

    @Test
    @DisplayName("Should identify numerator/denominator status")
    void testNumeratorDenominatorStatus() {
        // Given: Measure evaluation with numerator/denominator
        String tenantId = "TENANT-001";
        String patientId = "PATIENT-123";

        mockMeasureStore.addMeasureEvaluation(tenantId, patientId, "COL", "NUMERATOR", 1.0f);
        mockMeasureStore.addMeasureEvaluation(tenantId, patientId, "DM", "DENOMINATOR", 0.5f);

        // When: Retrieving measures
        MeasureEvaluationResult colResult = measureQueryService.getMeasure(tenantId, patientId, "COL");
        MeasureEvaluationResult dmResult = measureQueryService.getMeasure(tenantId, patientId, "DM");

        // Then: Should indicate status
        assertThat(colResult.isInNumerator()).isTrue();
        assertThat(dmResult.isInDenominator()).isTrue();
    }

    // ===== Cohort Aggregation Tests =====

    @Test
    @DisplayName("Should calculate measure rates across cohort")
    void testCohortMeasureRate() {
        // Given: 10 patients, 7 compliant with COL
        String tenantId = "TENANT-001";

        for (int i = 0; i < 7; i++) {
            mockMeasureStore.addMeasureEvaluation(tenantId, "PATIENT-" + i, "COL", "MET", 1.0f);
        }
        for (int i = 7; i < 10; i++) {
            mockMeasureStore.addMeasureEvaluation(tenantId, "PATIENT-" + i, "COL", "NOT_MET", 0.0f);
        }

        // When: Calculating cohort rate
        float colRate = measureQueryService.getMeasureRate(tenantId, "COL");

        // Then: Rate should be 70%
        assertThat(colRate).isCloseTo(0.70f, within(0.01f));
    }

    @Test
    @DisplayName("Should calculate numerator/denominator percentages")
    void testNumeratorPercentage() {
        // Given: 100 patients in denominator, 80 in numerator
        String tenantId = "TENANT-001";

        for (int i = 0; i < 80; i++) {
            mockMeasureStore.addMeasureEvaluation(tenantId, "PATIENT-" + i, "DM", "NUMERATOR", 1.0f);
        }
        for (int i = 80; i < 100; i++) {
            mockMeasureStore.addMeasureEvaluation(tenantId, "PATIENT-" + i, "DM", "DENOMINATOR", 0.0f);
        }

        // When: Getting numerator rate
        float numeratorRate = measureQueryService.getNumeratorPercentage(tenantId, "DM");

        // Then: Should be 80%
        assertThat(numeratorRate).isCloseTo(0.80f, within(0.01f));
    }

    // ===== Measure Comparison Tests =====

    @Test
    @DisplayName("Should compare measure performance across measures")
    void testMeasureComparison() {
        // Given: Patient with different measure rates
        String tenantId = "TENANT-001";

        mockMeasureStore.addMeasureEvaluation(tenantId, "COHORT", "COL", "MET", 0.85f);
        mockMeasureStore.addMeasureEvaluation(tenantId, "COHORT", "DM", "MET", 0.72f);
        mockMeasureStore.addMeasureEvaluation(tenantId, "COHORT", "HTN", "MET", 0.68f);

        // When: Getting measures sorted by rate
        List<MeasureEvaluationResult> sorted = measureQueryService.getMeasuresSortedByRate(tenantId);

        // Then: Should be in descending order
        assertThat(sorted).hasSizeGreaterThan(0);
        for (int i = 0; i < sorted.size() - 1; i++) {
            assertThat(sorted.get(i).getScore()).isGreaterThanOrEqualTo(sorted.get(i + 1).getScore());
        }
    }

    @Test
    @DisplayName("Should identify measures below performance target")
    void testBelowTargetMeasures() {
        // Given: Measures with target rates
        String tenantId = "TENANT-001";
        float target = 0.80f;

        mockMeasureStore.addMeasureEvaluation(tenantId, "COHORT", "COL", "MET", 0.85f);
        mockMeasureStore.addMeasureEvaluation(tenantId, "COHORT", "DM", "MET", 0.72f);
        mockMeasureStore.addMeasureEvaluation(tenantId, "COHORT", "HTN", "MET", 0.68f);

        // When: Getting measures below target
        List<MeasureEvaluationResult> below = measureQueryService.getMeasuresBelowTarget(tenantId, target);

        // Then: Should return DM and HTN
        assertThat(below).hasSizeGreaterThanOrEqualTo(2);
        assertThat(below).allMatch(m -> m.getScore() < target);
    }

    // ===== Temporal Measure Snapshots =====

    @Test
    @DisplayName("Should retrieve measure snapshot as of date")
    void testMeasureSnapshotAtDate() {
        // Given: Measure evaluations at different dates
        String tenantId = "TENANT-001";
        LocalDate date1 = LocalDate.of(2025, 12, 31);
        LocalDate date2 = LocalDate.of(2026, 1, 15);

        mockMeasureStore.addMeasureEvaluationAtDate(tenantId, "COHORT", "COL", "MET", 0.80f, date1);
        mockMeasureStore.addMeasureEvaluationAtDate(tenantId, "COHORT", "COL", "MET", 0.85f, date2);

        // When: Getting snapshot at date1
        float scoreAtDate1 = measureQueryService.getMeasureScoreAsOf(tenantId, "COL", date1);

        // Then: Should return score at that date
        assertThat(scoreAtDate1).isCloseTo(0.80f, within(0.01f));
    }

    // ===== Caching Tests =====

    @Test
    @DisplayName("Should cache measure rate queries")
    void testMeasureRateCaching() {
        // Given: Calculated measure rate
        String tenantId = "TENANT-001";
        mockMeasureStore.addMeasureEvaluation(tenantId, "PATIENT-1", "COL", "MET", 1.0f);

        // When: Querying same measure rate twice
        float rate1 = measureQueryService.getMeasureRate(tenantId, "COL");
        float rate2 = measureQueryService.getMeasureRate(tenantId, "COL");

        // Then: Second should be cached
        assertThat(rate1).isEqualTo(rate2);
        assertThat(mockCacheStore.getCacheHitCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should enforce cache TTL for HIPAA compliance")
    void testCacheTTLCompliance() {
        // Given: Cached measure data
        String tenantId = "TENANT-001";

        // When: Accessing cached data
        float rate = measureQueryService.getMeasureRate(tenantId, "COL");

        // Then: Cache TTL should be <= 5 minutes
        long ttl = mockCacheStore.getTTL("measure:COL:" + tenantId);
        assertThat(ttl).isGreaterThan(0).isLessThanOrEqualTo(300);
    }

    // ===== Multi-Tenant Tests =====

    @Test
    @DisplayName("Should isolate measure evaluations by tenant")
    void testMultiTenantMeasures() {
        // Given: Same measure in different tenants
        String tenant1 = "TENANT-001";
        String tenant2 = "TENANT-002";

        mockMeasureStore.addMeasureEvaluation(tenant1, "COHORT", "COL", "MET", 0.85f);
        mockMeasureStore.addMeasureEvaluation(tenant2, "COHORT", "COL", "MET", 0.65f);

        // When: Querying each tenant
        float rate1 = measureQueryService.getMeasureRate(tenant1, "COL");
        float rate2 = measureQueryService.getMeasureRate(tenant2, "COL");

        // Then: Should return different rates
        assertThat(rate1).isNotEqualTo(rate2);
    }

    // ===== Error Handling Tests =====

    @Test
    @DisplayName("Should handle missing measure gracefully")
    void testMissingMeasure() {
        // When: Querying non-existent measure
        float rate = measureQueryService.getMeasureRate("TENANT-001", "UNKNOWN");

        // Then: Should return 0 or handle gracefully
        assertThat(rate).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(1.0f);
    }

    @Test
    @DisplayName("Should handle zero denominator")
    void testZeroDenominator() {
        // Given: No patients in denominator
        String tenantId = "TENANT-001";

        // When: Calculating rate with zero denominator
        float rate = measureQueryService.getMeasureRate(tenantId, "UNKNOWN");

        // Then: Should return 0 or handle gracefully
        assertThat(rate).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(1.0f);
    }

    // ===== Mock Classes =====

    static class MockMeasureProjectionStore {
        private final Map<String, Map<String, MeasureEvaluation>> data = new HashMap<>();

        void addMeasureEvaluation(String tenantId, String cohortId, String measureCode, String status, float score) {
            String key = tenantId + ":" + cohortId + ":" + measureCode;
            data.put(key, new MeasureEvaluation(measureCode, status, score));
        }

        void addMeasureEvaluationAtDate(String tenantId, String cohortId, String measureCode, String status, float score, LocalDate date) {
            String key = tenantId + ":" + cohortId + ":" + measureCode + ":" + date;
            data.put(key, new MeasureEvaluation(measureCode, status, score));
        }

        MeasureEvaluation getMeasureEvaluation(String tenantId, String cohortId, String measureCode) {
            return data.get(tenantId + ":" + cohortId + ":" + measureCode);
        }

        Collection<MeasureEvaluation> getAllForTenant(String tenantId) {
            return data.values().stream()
                .filter(m -> data.keySet().stream().anyMatch(k -> k.startsWith(tenantId)))
                .toList();
        }
    }

    static class MeasureEvaluation {
        private final String measureCode;
        private final String status;
        private final float score;

        MeasureEvaluation(String measureCode, String status, float score) {
            this.measureCode = measureCode;
            this.status = status;
            this.score = score;
        }

        String getMeasureCode() { return measureCode; }
        String getStatus() { return status; }
        float getScore() { return score; }
    }

    static class MockCacheStore {
        private final Map<String, Object> cache = new HashMap<>();
        private final Map<String, Long> ttls = new HashMap<>();
        private int cacheHitCount = 0;

        void put(String key, Object value, long ttlSeconds) {
            cache.put(key, value);
            ttls.put(key, ttlSeconds);
        }

        Object get(String key) {
            if (cache.containsKey(key)) {
                cacheHitCount++;
                return cache.get(key);
            }
            return null;
        }

        long getTTL(String key) {
            return ttls.getOrDefault(key, 0L);
        }

        int getCacheHitCount() {
            return cacheHitCount;
        }
    }
}
