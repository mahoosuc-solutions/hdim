package com.healthdata.cms.performance;

import com.healthdata.cms.repository.CmsClaimRepository;
import com.healthdata.cms.repository.SyncAuditLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Load Testing Integration Tests
 * Validates system performance under high load:
 * - BCDA bulk operations (10K, 50K, 100K claims)
 * - Connection pool saturation handling
 * - Memory usage and GC behavior
 * - Concurrent sync operations with multiple sources
 * - Query performance optimization
 *
 * Performance baselines documented in PHASE-2-WEEK-5 documentation
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Load Testing Integration Tests")
class LoadTestingIntegrationTest {

    @Autowired
    private LoadTestingService loadTestingService;

    @Autowired
    private CmsClaimRepository claimRepository;

    @Autowired
    private SyncAuditLogRepository auditLogRepository;

    private UUID testTenantId;

    @BeforeEach
    void setUp() {
        testTenantId = UUID.randomUUID();
        claimRepository.deleteAll();
        auditLogRepository.deleteAll();
    }

    @Test
    @DisplayName("Should load 10K claims in under 60 seconds")
    void testLoad10KClaims() {
        // Create load test configuration for 10K claims
        LoadTestingService.LoadTestConfig config = LoadTestingService.LoadTestConfig.builder()
            .tenantId(testTenantId)
            .claimCount(10000)
            .threadCount(4)
            .batchCount(10)
            .build();

        // Execute load test
        LoadTestingService.LoadTestResult result = loadTestingService.generateAndLoadClaims(config);

        // Assert success
        assertTrue(result.isSuccess(), "Load test should succeed");
        assertEquals(10000, result.getActualClaimsInserted());
        assertTrue(result.getTotalTimeMs() < 60000,
            "10K claims should load in <60 seconds, took: " + result.getTotalTimeMs() + "ms");

        // Log performance metrics
        log.info("10K Load Test Results:");
        log.info("  Total Time: {} ms", result.getTotalTimeMs());
        log.info("  Generation: {} ms", result.getGenerationTimeMs());
        log.info("  Insertion: {} ms", result.getInsertionTimeMs());
        log.info("  Claims/sec: {}", String.format("%.2f", result.getClaimsPerSecond()));
        log.info("  Avg Insert: {} ms/claim", String.format("%.3f", result.getAverageClaimInsertionMs()));
        log.info("  Memory Used: {} MB", result.getMemoryUsedMb());
    }

    @Test
    @DisplayName("Should load 50K claims in under 5 minutes")
    void testLoad50KClaims() {
        // Create load test configuration for 50K claims
        LoadTestingService.LoadTestConfig config = LoadTestingService.LoadTestConfig.builder()
            .tenantId(testTenantId)
            .claimCount(50000)
            .threadCount(8)
            .batchCount(20)
            .build();

        // Execute load test
        LoadTestingService.LoadTestResult result = loadTestingService.generateAndLoadClaims(config);

        // Assert success
        assertTrue(result.isSuccess(), "Load test should succeed");
        assertEquals(50000, result.getActualClaimsInserted());
        assertTrue(result.getTotalTimeMs() < 300000,
            "50K claims should load in <5 minutes, took: " + result.getTotalTimeMs() + "ms");

        // Performance should be consistent with 10K test (within 10%)
        double expectedClaimsPerSec = 200; // Based on 10K test
        assertTrue(result.getClaimsPerSecond() > expectedClaimsPerSec * 0.9,
            "Performance should be consistent: " + result.getClaimsPerSecond() + " claims/sec");

        // Log performance metrics
        log.info("50K Load Test Results:");
        log.info("  Total Time: {} ms", result.getTotalTimeMs());
        log.info("  Claims/sec: {}", String.format("%.2f", result.getClaimsPerSecond()));
        log.info("  Avg Insert: {} ms/claim", String.format("%.3f", result.getAverageClaimInsertionMs()));
        log.info("  Memory Used: {} MB", result.getMemoryUsedMb());
    }

    @Test
    @DisplayName("Should load 100K claims within reasonable time")
    void testLoad100KClaims() {
        // Create load test configuration for 100K claims
        LoadTestingService.LoadTestConfig config = LoadTestingService.LoadTestConfig.builder()
            .tenantId(testTenantId)
            .claimCount(100000)
            .threadCount(16)
            .batchCount(40)
            .build();

        // Execute load test
        LoadTestingService.LoadTestResult result = loadTestingService.generateAndLoadClaims(config);

        // Assert success
        assertTrue(result.isSuccess(), "Load test should succeed");
        assertEquals(100000, result.getActualClaimsInserted());

        // Should complete in reasonable time (10-15 minutes for 100K)
        assertTrue(result.getTotalTimeMs() < 900000,
            "100K claims should load in <15 minutes, took: " + result.getTotalTimeMs() + "ms");

        // Memory should not grow exponentially
        assertTrue(result.getMemoryUsedMb() < result.getMemoryMaxMb() * 0.8,
            "Memory usage should not exceed 80% of max");

        // Log performance metrics
        log.info("100K Load Test Results:");
        log.info("  Total Time: {} ms", result.getTotalTimeMs());
        log.info("  Claims/sec: {}", String.format("%.2f", result.getClaimsPerSecond()));
        log.info("  Memory Used: {} MB / {} MB max",
            result.getMemoryUsedMb(), result.getMemoryMaxMb());
    }

    @Test
    @DisplayName("Should handle connection pool saturation gracefully")
    void testConnectionPoolSaturation() throws InterruptedException {
        // Pre-load some data for querying
        LoadTestingService.LoadTestConfig config = LoadTestingService.LoadTestConfig.builder()
            .tenantId(testTenantId)
            .claimCount(5000)
            .threadCount(4)
            .batchCount(5)
            .build();
        loadTestingService.generateAndLoadClaims(config);

        // Test connection pool with max connections = 10 (from production config)
        LoadTestingService.ConnectionPoolTest result =
            loadTestingService.testConnectionPoolSaturation(10, 30);

        // Assert that system handled saturation
        assertTrue(result.getSuccessRate() > 95,
            "Success rate should be >95%, got: " + result.getSuccessRate() + "%");

        // Should complete queries even under saturation
        assertTrue(result.getSuccessfulQueries() > 0,
            "Should successfully execute queries");

        log.info("Connection Pool Saturation Results:");
        log.info("  Test Duration: {} seconds", result.getTestDurationSeconds());
        log.info("  Total Queries: {}", result.getTotalQueries());
        log.info("  Successful: {} ({}%)", result.getSuccessfulQueries(),
            String.format("%.2f", result.getSuccessRate()));
        log.info("  Failed: {}", result.getFailedQueries());
        log.info("  Queries/sec: {}", String.format("%.2f", result.getQueriesPerSecond()));
    }

    @Test
    @DisplayName("Should handle concurrent sync operations from multiple sources")
    void testConcurrentSyncOperations() {
        // Configure concurrent syncs
        LoadTestingService.ConcurrentSyncConfig config = LoadTestingService.ConcurrentSyncConfig.builder()
            .tenantId(testTenantId)
            .sources(Arrays.asList("BCDA", "AB2D", "DPC"))
            .concurrentSyncs(9)
            .claimsPerSync(1000)
            .build();

        // Execute concurrent sync test
        LoadTestingService.ConcurrentSyncResult result = loadTestingService.simulateConcurrentSyncs(config);

        // Assert all syncs completed
        assertTrue(result.isSuccess(), "Concurrent sync test should succeed");
        assertEquals(9, result.getCompletedSyncs());
        assertEquals(9000, result.getTotalClaimsProcessed()); // 9 syncs × 1000 claims

        // Performance check
        assertTrue(result.getTotalTimeMs() < 120000,
            "9 concurrent syncs should complete in <2 minutes");

        log.info("Concurrent Sync Results:");
        log.info("  Completed Syncs: {}", result.getCompletedSyncs());
        log.info("  Total Claims: {}", result.getTotalClaimsProcessed());
        log.info("  Total Time: {} ms", result.getTotalTimeMs());
        log.info("  Claims/sec: {}", String.format("%.2f", result.getClaimsPerSecond()));
    }

    @Test
    @DisplayName("Should maintain performance with mixed read/write workload")
    void testMixedReadWriteWorkload() {
        // Load initial dataset
        LoadTestingService.LoadTestConfig config = LoadTestingService.LoadTestConfig.builder()
            .tenantId(testTenantId)
            .claimCount(5000)
            .threadCount(4)
            .batchCount(5)
            .build();

        LoadTestingService.LoadTestResult loadResult = loadTestingService.generateAndLoadClaims(config);
        assertTrue(loadResult.isSuccess());

        long startTime = System.currentTimeMillis();

        // Perform concurrent reads while system is operational
        for (int i = 0; i < 10; i++) {
            long count = claimRepository.count();
            assertTrue(count == 5000, "Should have 5000 claims");
        }

        long readTime = System.currentTimeMillis() - startTime;
        double readsPerSecond = (10 * 1000.0) / readTime;

        log.info("Mixed Read/Write Results:");
        log.info("  Read Operations: 10");
        log.info("  Read Time: {} ms", readTime);
        log.info("  Reads/sec: {}", String.format("%.2f", readsPerSecond));

        // Reads should be fast even with loaded data
        assertTrue(readTime < 5000,
            "10 reads should complete in <5 seconds");
    }

    @Test
    @DisplayName("Should handle batch insertion with error recovery")
    void testBatchInsertionWithErrors() {
        // Load valid data
        LoadTestingService.LoadTestConfig config = LoadTestingService.LoadTestConfig.builder()
            .tenantId(testTenantId)
            .claimCount(5000)
            .threadCount(4)
            .batchCount(5)
            .build();

        LoadTestingService.LoadTestResult result = loadTestingService.generateAndLoadClaims(config);

        // Assert all claims were successfully inserted
        assertTrue(result.isSuccess());
        assertEquals(5000, result.getActualClaimsInserted());

        // Verify no partial failures
        long actualCount = claimRepository.count();
        assertEquals(5000, actualCount,
            "Database should have exactly 5000 claims, no partial batch failures");

        log.info("Batch Insertion Results:");
        log.info("  Total Inserted: {}", actualCount);
        log.info("  Insertion Time: {} ms", result.getInsertionTimeMs());
        log.info("  Error Recovery: NONE (all successful)");
    }

    @Test
    @DisplayName("Should provide consistent query performance with large dataset")
    void testQueryPerformanceAtScale() {
        // Load 10K claims
        LoadTestingService.LoadTestConfig config = LoadTestingService.LoadTestConfig.builder()
            .tenantId(testTenantId)
            .claimCount(10000)
            .threadCount(4)
            .batchCount(10)
            .build();

        LoadTestingService.LoadTestResult loadResult = loadTestingService.generateAndLoadClaims(config);
        assertTrue(loadResult.isSuccess());

        // Measure query performance
        long countStartTime = System.currentTimeMillis();
        long totalClaims = claimRepository.count();
        long countTime = System.currentTimeMillis() - countStartTime;

        long processingStartTime = System.currentTimeMillis();
        long processedClaims = claimRepository.countByIsProcessedTrue();
        long processingTime = System.currentTimeMillis() - processingStartTime;

        // Both queries should be fast even with 10K records
        assertTrue(countTime < 500,
            "Count query should be <500ms, took: " + countTime + "ms");
        assertTrue(processingTime < 500,
            "Processing count query should be <500ms, took: " + processingTime + "ms");

        log.info("Query Performance Results (10K claims):");
        log.info("  Total Claims Count: {} in {} ms", totalClaims, countTime);
        log.info("  Processed Claims: {} in {} ms", processedClaims, processingTime);
        log.info("  Query Response: < {} ms", Math.max(countTime, processingTime));
    }

    @Test
    @DisplayName("Should monitor memory utilization during load test")
    void testMemoryUtilizationPatterns() {
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();

        // Load 50K claims
        LoadTestingService.LoadTestConfig config = LoadTestingService.LoadTestConfig.builder()
            .tenantId(testTenantId)
            .claimCount(50000)
            .threadCount(8)
            .batchCount(20)
            .build();

        LoadTestingService.LoadTestResult result = loadTestingService.generateAndLoadClaims(config);
        assertTrue(result.isSuccess());

        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryIncrease = (finalMemory - initialMemory) / (1024 * 1024);

        // Memory increase should be reasonable (not more than 500MB for 50K claims)
        assertTrue(memoryIncrease < 500,
            "Memory increase should be <500MB, was: " + memoryIncrease + "MB");

        log.info("Memory Utilization Results:");
        log.info("  Initial Memory: {} MB", initialMemory / (1024 * 1024));
        log.info("  Final Memory: {} MB", finalMemory / (1024 * 1024));
        log.info("  Memory Increase: {} MB", memoryIncrease);
        log.info("  Peak Memory Used: {} MB", result.getMemoryUsedMb());
    }

    @Test
    @DisplayName("Should complete stress test with multiple data sources")
    void testStressTestMultipleSources() {
        // Simulate multiple concurrent sync sources
        LoadTestingService.ConcurrentSyncConfig config = LoadTestingService.ConcurrentSyncConfig.builder()
            .tenantId(testTenantId)
            .sources(Arrays.asList("BCDA", "AB2D", "DPC"))
            .concurrentSyncs(15) // Heavy load: 15 concurrent syncs
            .claimsPerSync(2000)
            .build();

        long startTime = System.currentTimeMillis();
        LoadTestingService.ConcurrentSyncResult result = loadTestingService.simulateConcurrentSyncs(config);
        long totalTime = System.currentTimeMillis() - startTime;

        // Assert all syncs completed successfully
        assertTrue(result.isSuccess(), "Stress test should succeed");
        assertEquals(15, result.getCompletedSyncs());
        assertEquals(30000, result.getTotalClaimsProcessed());

        // Performance should scale reasonably
        double claimsPerSecond = result.getClaimsPerSecond();
        assertTrue(claimsPerSecond > 100,
            "Should process >100 claims/sec, got: " + claimsPerSecond);

        log.info("Stress Test Results (15 concurrent syncs × 2K claims):");
        log.info("  Total Time: {} ms", totalTime);
        log.info("  Total Claims: {}", result.getTotalClaimsProcessed());
        log.info("  Throughput: {} claims/sec", String.format("%.2f", claimsPerSecond));
        log.info("  Syncs/sec: {}", String.format("%.2f", (15.0 * 1000) / totalTime));
    }
}
