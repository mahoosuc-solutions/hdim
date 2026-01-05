package com.healthdata.cms.performance;

import com.healthdata.cms.model.CmsClaim;
import com.healthdata.cms.model.SyncAuditLog;
import com.healthdata.cms.repository.CmsClaimRepository;
import com.healthdata.cms.repository.SyncAuditLogRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Load Testing Service
 * Generates synthetic CMS claims data for load testing:
 * - Configurable claim generation (10K, 50K, 100K+)
 * - Multi-threaded batch insertion
 * - Performance metrics collection
 * - Memory and GC profiling
 *
 * Used to validate:
 * - Database performance under high volume
 * - Connection pool saturation handling
 * - Query performance with large datasets
 * - Memory usage patterns
 */
@Slf4j
@Service
public class LoadTestingService {

    @Autowired
    private CmsClaimRepository claimRepository;

    @Autowired
    private SyncAuditLogRepository auditLogRepository;

    private static final String[] BENEFICIARY_IDS = {
        "BEN-001", "BEN-002", "BEN-003", "BEN-004", "BEN-005"
    };

    private static final String[] CLAIM_TYPES = {
        "Part A", "Part B", "Part D"
    };

    private static final String[] PROVIDERS = {
        "PROVIDER-001", "PROVIDER-002", "PROVIDER-003"
    };

    /**
     * Generate and load synthetic claims
     */
    @Transactional
    public LoadTestResult generateAndLoadClaims(LoadTestConfig config) {
        long startTime = System.currentTimeMillis();
        LoadTestResult result = LoadTestResult.builder()
            .config(config)
            .startTime(LocalDateTime.now(ZoneId.of("UTC")))
            .build();

        try {
            log.info("Starting load test: {} claims across {} threads",
                config.getClaimCount(), config.getThreadCount());

            // Generate claims in batches
            List<List<CmsClaim>> batches = generateClaimBatches(config);
            result.setGenerationTimeMs(System.currentTimeMillis() - startTime);

            // Insert using thread pool
            long insertStartTime = System.currentTimeMillis();
            insertClaimsMultiThreaded(batches, config);
            result.setInsertionTimeMs(System.currentTimeMillis() - insertStartTime);

            // Verify insertion
            long verifyStartTime = System.currentTimeMillis();
            long insertedCount = claimRepository.count();
            result.setVerificationTimeMs(System.currentTimeMillis() - verifyStartTime);

            // Capture memory metrics
            Runtime runtime = Runtime.getRuntime();
            result.setMemoryUsedMb((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
            result.setMemoryMaxMb(runtime.maxMemory() / (1024 * 1024));

            result.setSuccess(true);
            result.setActualClaimsInserted(insertedCount);
            result.setTotalTimeMs(System.currentTimeMillis() - startTime);

            log.info("Load test completed: {} claims in {} ms (gen:{}, insert:{}, verify:{})",
                insertedCount, result.getTotalTimeMs(),
                result.getGenerationTimeMs(), result.getInsertionTimeMs(), result.getVerificationTimeMs());

            return result;

        } catch (Exception e) {
            log.error("Load test failed", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setTotalTimeMs(System.currentTimeMillis() - startTime);
            return result;
        }
    }

    /**
     * Generate claims in batches
     */
    private List<List<CmsClaim>> generateClaimBatches(LoadTestConfig config) {
        List<List<CmsClaim>> batches = new ArrayList<>();
        int batchSize = config.getClaimCount() / config.getBatchCount();

        for (int i = 0; i < config.getBatchCount(); i++) {
            List<CmsClaim> batch = new ArrayList<>();
            int startClaim = i * batchSize;
            int endClaim = (i == config.getBatchCount() - 1) ?
                config.getClaimCount() : (i + 1) * batchSize;

            for (int j = startClaim; j < endClaim; j++) {
                batch.add(generateClaim(config.getTenantId(), j));
            }
            batches.add(batch);
        }

        log.info("Generated {} batches with {} claims each",
            batches.size(), batchSize);
        return batches;
    }

    /**
     * Generate single claim with realistic data
     */
    private CmsClaim generateClaim(UUID tenantId, int index) {
        CmsClaim claim = new CmsClaim();
        claim.setTenantId(tenantId);
        claim.setClaimId("CLAIM-LOAD-" + index);
        claim.setBeneficiaryId(BENEFICIARY_IDS[index % BENEFICIARY_IDS.length]);
        claim.setDataSource(CmsClaim.ClaimSource.values()[index % 3]);
        claim.setContentHash("hash-" + UUID.randomUUID());
        claim.setFhirResource(generateFhirJson(index));
        claim.setImportedAt(LocalDateTime.now(ZoneId.of("UTC")));
        claim.setProcessed(index % 10 == 0); // 10% pre-processed
        return claim;
    }

    /**
     * Generate FHIR JSON for claim
     */
    private String generateFhirJson(int index) {
        return """
            {
              "resourceType": "ExplanationOfBenefit",
              "id": "eob-%d",
              "status": "active",
              "type": {
                "coding": [
                  {
                    "system": "http://terminology.hl7.org/CodeSystem/claim-type",
                    "code": "institutional"
                  }
                ]
              },
              "use": "claim",
              "patient": {
                "reference": "Patient/ben-%d"
              },
              "billablePeriod": {
                "start": "2024-01-01",
                "end": "2024-01-31"
              },
              "created": "2024-02-01T00:00:00Z",
              "provider": {
                "reference": "Organization/prov-%d"
              },
              "insurer": {
                "reference": "Organization/cms"
              },
              "claim": {
                "reference": "Claim/claim-%d"
              },
              "claimResponse": {
                "reference": "ClaimResponse/resp-%d"
              }
            }
            """.formatted(index, index, index, index, index);
    }

    /**
     * Insert claims using multiple threads
     */
    private void insertClaimsMultiThreaded(List<List<CmsClaim>> batches, LoadTestConfig config)
            throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(config.getThreadCount());
        List<Future<?>> futures = new ArrayList<>();

        for (List<CmsClaim> batch : batches) {
            futures.add(executor.submit(() -> {
                long batchStart = System.currentTimeMillis();
                claimRepository.saveAll(batch);
                log.debug("Batch of {} claims inserted in {} ms",
                    batch.size(), System.currentTimeMillis() - batchStart);
            }));
        }

        // Wait for all to complete
        for (Future<?> future : futures) {
            future.get(5, TimeUnit.MINUTES);
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }

    /**
     * Simulate concurrent sync operations
     */
    @Transactional
    public ConcurrentSyncResult simulateConcurrentSyncs(ConcurrentSyncConfig config) {
        long startTime = System.currentTimeMillis();
        ConcurrentSyncResult result = ConcurrentSyncResult.builder()
            .config(config)
            .startTime(LocalDateTime.now(ZoneId.of("UTC")))
            .build();

        try {
            ExecutorService executor = Executors.newFixedThreadPool(config.getConcurrentSyncs());
            List<Future<?>> futures = new ArrayList<>();
            List<SyncAuditLog> auditLogs = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < config.getConcurrentSyncs(); i++) {
                final int syncIndex = i;
                futures.add(executor.submit(() -> {
                    long syncStart = System.currentTimeMillis();
                    String source = config.getSources().get(syncIndex % config.getSources().size());

                    SyncAuditLog log = new SyncAuditLog();
                    log.setSource(source);
                    log.setStatus(SyncAuditLog.SyncStatus.COMPLETED);
                    log.setType("BULK_EXPORT");
                    log.setTenantId(config.getTenantId());
                    log.setTotalClaims(config.getClaimsPerSync());
                    log.setSuccessfulClaims(config.getClaimsPerSync() - (config.getClaimsPerSync() / 100));
                    log.setFailedClaims(config.getClaimsPerSync() / 100);
                    log.setStartedAt(LocalDateTime.now(ZoneId.of("UTC")));
                    log.setCompletedAt(LocalDateTime.now(ZoneId.of("UTC")));
                    log.setDurationSeconds(System.currentTimeMillis() - syncStart);

                    auditLogRepository.save(log);
                    auditLogs.add(log);
                }));
            }

            // Wait for all syncs to complete
            for (Future<?> future : futures) {
                future.get(5, TimeUnit.MINUTES);
            }

            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.MINUTES);

            result.setSuccess(true);
            result.setCompletedSyncs(auditLogs.size());
            result.setTotalClaimsProcessed(
                auditLogs.stream().mapToLong(SyncAuditLog::getTotalClaims).sum()
            );
            result.setTotalTimeMs(System.currentTimeMillis() - startTime);

            log.info("Concurrent sync test completed: {} syncs in {} ms",
                auditLogs.size(), result.getTotalTimeMs());

            return result;

        } catch (Exception e) {
            log.error("Concurrent sync test failed", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
            result.setTotalTimeMs(System.currentTimeMillis() - startTime);
            return result;
        }
    }

    /**
     * Test connection pool saturation
     */
    public ConnectionPoolTest testConnectionPoolSaturation(int maxConnections, int testDurationSeconds)
            throws InterruptedException {
        long startTime = System.currentTimeMillis();
        ConnectionPoolTest result = ConnectionPoolTest.builder()
            .maxConnections(maxConnections)
            .testDurationSeconds(testDurationSeconds)
            .startTime(LocalDateTime.now(ZoneId.of("UTC")))
            .build();

        ExecutorService executor = Executors.newFixedThreadPool(maxConnections * 2);
        AtomicInteger peakConnections = new AtomicInteger(0);
        AtomicInteger successfulQueries = new AtomicInteger(0);
        AtomicInteger failedQueries = new AtomicInteger(0);
        ConcurrentHashMap<Long, Integer> connectionTimeline = new ConcurrentHashMap<>();

        long endTime = System.currentTimeMillis() + (testDurationSeconds * 1000L);

        for (int i = 0; i < maxConnections * 2; i++) {
            executor.submit(() -> {
                while (System.currentTimeMillis() < endTime) {
                    try {
                        long claimCount = claimRepository.count();
                        successfulQueries.incrementAndGet();
                    } catch (Exception e) {
                        failedQueries.incrementAndGet();
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(testDurationSeconds + 10, TimeUnit.SECONDS);

        result.setSuccessfulQueries(successfulQueries.get());
        result.setFailedQueries(failedQueries.get());
        result.setTotalQueries(successfulQueries.get() + failedQueries.get());
        result.setTotalTimeMs(System.currentTimeMillis() - startTime);

        log.info("Connection pool saturation test: {} successful, {} failed queries",
            result.getSuccessfulQueries(), result.getFailedQueries());

        return result;
    }

    // ========== DTOs ==========

    @Data
    @Builder
    public static class LoadTestConfig {
        private UUID tenantId;
        private int claimCount;
        private int threadCount;
        private int batchCount;

        public int getThreadCount() {
            return threadCount > 0 ? threadCount : 4;
        }

        public int getBatchCount() {
            return batchCount > 0 ? batchCount : 10;
        }
    }

    @Data
    @Builder
    public static class LoadTestResult {
        private LoadTestConfig config;
        private LocalDateTime startTime;
        private boolean success;
        private long generationTimeMs;
        private long insertionTimeMs;
        private long verificationTimeMs;
        private long totalTimeMs;
        private long actualClaimsInserted;
        private long memoryUsedMb;
        private long memoryMaxMb;
        private String errorMessage;

        public double getClaimsPerSecond() {
            return insertionTimeMs > 0 ?
                (actualClaimsInserted * 1000.0) / insertionTimeMs : 0;
        }

        public double getAverageClaimInsertionMs() {
            return actualClaimsInserted > 0 ?
                (double) insertionTimeMs / actualClaimsInserted : 0;
        }
    }

    @Data
    @Builder
    public static class ConcurrentSyncConfig {
        private UUID tenantId;
        private List<String> sources;
        private int concurrentSyncs;
        private int claimsPerSync;
    }

    @Data
    @Builder
    public static class ConcurrentSyncResult {
        private ConcurrentSyncConfig config;
        private LocalDateTime startTime;
        private boolean success;
        private int completedSyncs;
        private long totalClaimsProcessed;
        private long totalTimeMs;
        private String errorMessage;

        public double getClaimsPerSecond() {
            return totalTimeMs > 0 ?
                (totalClaimsProcessed * 1000.0) / totalTimeMs : 0;
        }
    }

    @Data
    @Builder
    public static class ConnectionPoolTest {
        private int maxConnections;
        private int testDurationSeconds;
        private LocalDateTime startTime;
        private long successfulQueries;
        private long failedQueries;
        private long totalQueries;
        private long totalTimeMs;

        public double getSuccessRate() {
            return totalQueries > 0 ?
                (successfulQueries * 100.0) / totalQueries : 0;
        }

        public double getQueriesPerSecond() {
            return totalTimeMs > 0 ?
                (totalQueries * 1000.0) / totalTimeMs : 0;
        }
    }
}
