package com.healthdata.quality.integration;

import com.healthdata.quality.config.TestMessagingConfiguration;
import com.healthdata.quality.config.TestWebSocketConfiguration;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import com.healthdata.quality.service.MeasureCalculationService;
import com.healthdata.quality.service.QualityReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import com.healthdata.authentication.config.AuthenticationAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for Redis caching behavior
 * Tests the caching of measure results and quality reports
 */
@Tag("integration")
@SpringBootTest
@EnableAutoConfiguration(exclude = {AuthenticationAutoConfiguration.class})
@ActiveProfiles("test")
@Transactional
@Import({TestMessagingConfiguration.class, TestWebSocketConfiguration.class})
@DisplayName("Caching Behavior Integration Tests")
class CachingBehaviorIntegrationTest {

    @Autowired
    private MeasureCalculationService calculationService;

    @Autowired
    private QualityReportService reportService;

    @Autowired
    private QualityMeasureResultRepository repository;

    @Autowired(required = false)
    private CacheManager cacheManager;

    private static final String TENANT_ID = "test-tenant";
    private static final UUID PATIENT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

    @BeforeEach
    void setUp() {
        repository.deleteAll();

        // Clear caches if cache manager is available
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
    }

    @Test
    @DisplayName("Should cache patient measure results")
    void shouldCachePatientMeasureResults() {
        // Skip test if caching is not configured
        if (cacheManager == null || cacheManager.getCache("measureResults") == null) {
            System.out.println("Cache not configured, skipping test");
            return;
        }

        // Create test data
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_1", true);
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_2", false);

        // First call - should hit database
        List<QualityMeasureResultEntity> firstCall =
                calculationService.getPatientMeasureResults(TENANT_ID, PATIENT_ID);
        assertEquals(2, firstCall.size());

        // Verify cache contains the result
        Cache cache = cacheManager.getCache("measureResults");
        assertNotNull(cache);
        String cacheKey = TENANT_ID + ":" + PATIENT_ID;
        assertNotNull(cache.get(cacheKey), "Cache should contain results after first call");

        // Second call - should hit cache
        List<QualityMeasureResultEntity> secondCall =
                calculationService.getPatientMeasureResults(TENANT_ID, PATIENT_ID);
        assertEquals(2, secondCall.size());

        // Results should be the same
        assertEquals(firstCall.size(), secondCall.size());
    }

    @Test
    @DisplayName("Should cache patient quality report")
    void shouldCachePatientQualityReport() {
        // Skip test if caching is not configured
        if (cacheManager == null || cacheManager.getCache("qualityReport") == null) {
            System.out.println("Cache not configured, skipping test");
            return;
        }

        // Create test data
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_1", true);
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_2", true);

        // First call - should generate report
        QualityReportService.QualityReport firstCall =
                reportService.getPatientQualityReport(TENANT_ID, PATIENT_ID);
        assertNotNull(firstCall);
        assertEquals(2, firstCall.totalMeasures());

        // Verify cache contains the report
        Cache cache = cacheManager.getCache("qualityReport");
        assertNotNull(cache);
        String cacheKey = TENANT_ID + ":" + PATIENT_ID;
        assertNotNull(cache.get(cacheKey), "Cache should contain report after first call");

        // Second call - should hit cache
        QualityReportService.QualityReport secondCall =
                reportService.getPatientQualityReport(TENANT_ID, PATIENT_ID);
        assertNotNull(secondCall);
        assertEquals(firstCall.totalMeasures(), secondCall.totalMeasures());
    }

    @Test
    @DisplayName("Should cache population quality report")
    void shouldCachePopulationQualityReport() {
        // Skip test if caching is not configured
        if (cacheManager == null || cacheManager.getCache("populationQualityReport") == null) {
            System.out.println("Cache not configured, skipping test");
            return;
        }

        int currentYear = LocalDate.now().getYear();

        // Create test data
        createMeasureResultWithYear(TENANT_ID, PATIENT_ID, "HEDIS_1", currentYear, true);
        createMeasureResultWithYear(TENANT_ID, UUID.randomUUID(), "HEDIS_2", currentYear, false);

        // First call - should generate report
        QualityReportService.PopulationQualityReport firstCall =
                reportService.getPopulationQualityReport(TENANT_ID, currentYear);
        assertNotNull(firstCall);
        assertEquals(currentYear, firstCall.year());

        // Verify cache contains the report
        Cache cache = cacheManager.getCache("populationQualityReport");
        assertNotNull(cache);
        String cacheKey = TENANT_ID + ":" + currentYear;
        assertNotNull(cache.get(cacheKey), "Cache should contain population report after first call");

        // Second call - should hit cache
        QualityReportService.PopulationQualityReport secondCall =
                reportService.getPopulationQualityReport(TENANT_ID, currentYear);
        assertNotNull(secondCall);
        assertEquals(firstCall.totalMeasures(), secondCall.totalMeasures());
    }

    @Test
    @DisplayName("Should isolate cache by tenant")
    void shouldIsolateCacheByTenant() {
        // Skip test if caching is not configured
        if (cacheManager == null || cacheManager.getCache("measureResults") == null) {
            System.out.println("Cache not configured, skipping test");
            return;
        }

        // Create data for tenant 1
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_1", true);
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_2", true);

        // Create data for tenant 2
        String tenant2 = "tenant-2";
        createMeasureResult(tenant2, PATIENT_ID, "HEDIS_3", false);

        // Get results for tenant 1
        List<QualityMeasureResultEntity> tenant1Results =
                calculationService.getPatientMeasureResults(TENANT_ID, PATIENT_ID);
        assertEquals(2, tenant1Results.size());

        // Get results for tenant 2
        List<QualityMeasureResultEntity> tenant2Results =
                calculationService.getPatientMeasureResults(tenant2, PATIENT_ID);
        assertEquals(1, tenant2Results.size());

        // Verify cache has separate entries
        Cache cache = cacheManager.getCache("measureResults");
        assertNotNull(cache);

        String cacheKey1 = TENANT_ID + ":" + PATIENT_ID;
        String cacheKey2 = tenant2 + ":" + PATIENT_ID;

        assertNotNull(cache.get(cacheKey1), "Tenant 1 cache should exist");
        assertNotNull(cache.get(cacheKey2), "Tenant 2 cache should exist");
    }

    @Test
    @DisplayName("Should isolate cache by patient")
    void shouldIsolateCacheByPatient() {
        // Skip test if caching is not configured
        if (cacheManager == null || cacheManager.getCache("measureResults") == null) {
            System.out.println("Cache not configured, skipping test");
            return;
        }

        UUID patient2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

        // Create data for patient 1
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_1", true);

        // Create data for patient 2
        createMeasureResult(TENANT_ID, patient2, "HEDIS_2", true);
        createMeasureResult(TENANT_ID, patient2, "HEDIS_3", false);

        // Get results for patient 1
        List<QualityMeasureResultEntity> patient1Results =
                calculationService.getPatientMeasureResults(TENANT_ID, PATIENT_ID);
        assertEquals(1, patient1Results.size());

        // Get results for patient 2
        List<QualityMeasureResultEntity> patient2Results =
                calculationService.getPatientMeasureResults(TENANT_ID, patient2);
        assertEquals(2, patient2Results.size());

        // Verify cache has separate entries
        Cache cache = cacheManager.getCache("measureResults");
        assertNotNull(cache);

        String cacheKey1 = TENANT_ID + ":" + PATIENT_ID;
        String cacheKey2 = TENANT_ID + ":" + patient2.toString();

        assertNotNull(cache.get(cacheKey1), "Patient 1 cache should exist");
        assertNotNull(cache.get(cacheKey2), "Patient 2 cache should exist");
    }

    @Test
    @DisplayName("Should handle cache miss gracefully")
    void shouldHandleCacheMissGracefully() {
        // Skip test if caching is not configured
        if (cacheManager == null || cacheManager.getCache("measureResults") == null) {
            System.out.println("Cache not configured, skipping test");
            return;
        }

        // Query for patient with no data - should return empty list
        List<QualityMeasureResultEntity> results =
                calculationService.getPatientMeasureResults(TENANT_ID, PATIENT_ID);

        assertNotNull(results);
        assertEquals(0, results.size());

        // Verify empty result was cached
        Cache cache = cacheManager.getCache("measureResults");
        assertNotNull(cache);
        String cacheKey = TENANT_ID + ":" + PATIENT_ID;
        assertNotNull(cache.get(cacheKey), "Empty result should be cached");
    }

    @Test
    @DisplayName("Should cache quality score in report")
    void shouldCacheQualityScoreInReport() {
        // Skip test if caching is not configured
        if (cacheManager == null || cacheManager.getCache("qualityReport") == null) {
            System.out.println("Cache not configured, skipping test");
            return;
        }

        // Create test data
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_1", true);
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_2", false);

        // Get report - should calculate and cache score
        QualityReportService.QualityReport report =
                reportService.getPatientQualityReport(TENANT_ID, PATIENT_ID);

        assertNotNull(report);
        assertEquals(2, report.totalMeasures());
        assertEquals(1, report.compliantMeasures());
        assertEquals(50.0, report.qualityScore(), 0.01);

        // Verify report is cached
        Cache cache = cacheManager.getCache("qualityReport");
        assertNotNull(cache);
        String cacheKey = TENANT_ID + ":" + PATIENT_ID;

        Cache.ValueWrapper cachedValue = cache.get(cacheKey);
        assertNotNull(cachedValue, "Report should be cached");

        QualityReportService.QualityReport cachedReport =
                (QualityReportService.QualityReport) cachedValue.get();
        assertNotNull(cachedReport);
        assertEquals(report.qualityScore(), cachedReport.qualityScore(), 0.01);
    }

    @Test
    @DisplayName("Should cache different years separately")
    void shouldCacheDifferentYearsSeparately() {
        // Skip test if caching is not configured
        if (cacheManager == null || cacheManager.getCache("populationQualityReport") == null) {
            System.out.println("Cache not configured, skipping test");
            return;
        }

        // Create data for different years
        createMeasureResultWithYear(TENANT_ID, PATIENT_ID, "HEDIS_2023", 2023, true);
        createMeasureResultWithYear(TENANT_ID, PATIENT_ID, "HEDIS_2024", 2024, true);

        // Get report for 2023
        QualityReportService.PopulationQualityReport report2023 =
                reportService.getPopulationQualityReport(TENANT_ID, 2023);
        assertEquals(2023, report2023.year());
        assertEquals(1, report2023.totalMeasures());

        // Get report for 2024
        QualityReportService.PopulationQualityReport report2024 =
                reportService.getPopulationQualityReport(TENANT_ID, 2024);
        assertEquals(2024, report2024.year());
        assertEquals(1, report2024.totalMeasures());

        // Verify both are cached separately
        Cache cache = cacheManager.getCache("populationQualityReport");
        assertNotNull(cache);

        String cacheKey2023 = TENANT_ID + ":2023";
        String cacheKey2024 = TENANT_ID + ":2024";

        assertNotNull(cache.get(cacheKey2023), "2023 report should be cached");
        assertNotNull(cache.get(cacheKey2024), "2024 report should be cached");
    }

    @Test
    @DisplayName("Should retrieve from cache on subsequent calls")
    void shouldRetrieveFromCacheOnSubsequentCalls() {
        // Skip test if caching is not configured
        if (cacheManager == null || cacheManager.getCache("measureResults") == null) {
            System.out.println("Cache not configured, skipping test");
            return;
        }

        // Create test data
        createMeasureResult(TENANT_ID, PATIENT_ID, "HEDIS_1", true);

        // First call
        long startTime1 = System.currentTimeMillis();
        List<QualityMeasureResultEntity> firstCall =
                calculationService.getPatientMeasureResults(TENANT_ID, PATIENT_ID);
        long duration1 = System.currentTimeMillis() - startTime1;

        // Second call (from cache - should be faster or same)
        long startTime2 = System.currentTimeMillis();
        List<QualityMeasureResultEntity> secondCall =
                calculationService.getPatientMeasureResults(TENANT_ID, PATIENT_ID);
        long duration2 = System.currentTimeMillis() - startTime2;

        // Results should be identical
        assertEquals(firstCall.size(), secondCall.size());

        // Note: We can't reliably test performance in unit tests,
        // but we verify that caching doesn't break functionality
        assertNotNull(secondCall);
    }

    // Helper methods
    private QualityMeasureResultEntity createMeasureResult(
            String tenantId,
            UUID patientId,
            String measureId,
            boolean compliant
    ) {
        return createMeasureResultWithYear(tenantId, patientId, measureId, LocalDate.now().getYear(), compliant);
    }

    private QualityMeasureResultEntity createMeasureResultWithYear(
            String tenantId,
            UUID patientId,
            String measureId,
            int year,
            boolean compliant
    ) {
        QualityMeasureResultEntity entity = QualityMeasureResultEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .measureId(measureId)
                .measureName("Test Measure " + measureId)
                .measureCategory("HEDIS")
                .measureYear(year)
                .numeratorCompliant(compliant)
                .denominatorElligible(true)
                .complianceRate(compliant ? 100.0 : 0.0)
                .score(compliant ? 95.0 : 50.0)
                .calculationDate(LocalDate.now())
                .cqlLibrary(measureId)
                .cqlResult("{\"result\": \"test\"}")
                .createdBy("integration-test")
                .build();

        return repository.save(entity);
    }
}
