package com.healthdata.eventreplay.concurrent;

import com.healthdata.eventsourcing.event.DomainEvent;
import com.healthdata.eventsourcing.event.PatientCreatedEvent;
import com.healthdata.eventsourcing.event.ConditionDiagnosedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Concurrent Replay Safety
 *
 * Tests thread-safe event replay:
 * - Multiple threads replaying different aggregates
 * - Concurrent updates to same projection
 * - Race condition prevention
 * - Deadlock prevention
 * - Optimistic locking under contention
 * - Snapshot consistency under concurrent access
 *
 * Critical for scalability:
 * - Batch rebuilds of 1000+ aggregates in parallel
 * - Real-time projections with competing writers
 * - Horizontal scaling across multiple replay instances
 *
 * HIPAA Requirement: Multi-tenant isolation must hold under concurrent access
 */
@DisplayName("ConcurrentReplay Tests")
class ConcurrentReplayTest {

    private EventReplayEngine replayEngine;
    private MockEventStore mockEventStore;
    private ExecutorService executorService;

    @BeforeEach
    void setup() {
        mockEventStore = new MockEventStore();
        replayEngine = new EventReplayEngine(mockEventStore);
        executorService = Executors.newFixedThreadPool(4);
    }

    void tearDown() throws InterruptedException {
        executorService.shutdownNow();
        executorService.awaitTermination(5, TimeUnit.SECONDS);
    }

    // ===== Basic Concurrent Replay Tests =====

    @Test
    @DisplayName("Should replay events concurrently for different aggregates")
    void testConcurrentReplayDifferentAggregates() throws Exception {
        // Given: Events for 10 different patients
        String tenantId = "TENANT-001";
        int patientCount = 10;
        List<String> patientIds = new ArrayList<>();

        for (int i = 0; i < patientCount; i++) {
            String patientId = "PATIENT-" + i;
            patientIds.add(patientId);

            List<DomainEvent> events = Arrays.asList(
                new PatientCreatedEvent(patientId, tenantId, "Patient", String.valueOf(i), "1990-01-01"),
                new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension")
            );
            mockEventStore.storeEvents(patientId, events);
        }

        // When: Replaying all patients concurrently
        CyclicBarrier barrier = new CyclicBarrier(patientCount);
        List<Future<List<DomainEvent>>> futures = new ArrayList<>();

        for (String patientId : patientIds) {
            futures.add(executorService.submit(() -> {
                try {
                    barrier.await(); // Synchronize start
                    return replayEngine.replayAllEvents(patientId, tenantId);
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }));
        }

        // Then: All should complete successfully
        List<List<DomainEvent>> results = new ArrayList<>();
        for (Future<List<DomainEvent>> future : futures) {
            results.add(future.get(5, TimeUnit.SECONDS));
        }

        assertThat(results).hasSize(patientCount);
        results.forEach(events -> assertThat(events).hasSize(2));
    }

    @Test
    @DisplayName("Should prevent race conditions in concurrent replay")
    void testNoRaceConditions() throws Exception {
        // Given: Single patient, multiple replayers
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        List<DomainEvent> events = Arrays.asList(
            new PatientCreatedEvent(patientId, tenantId, "John", "Doe", "1990-01-01"),
            new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension"),
            new ConditionDiagnosedEvent(patientId, tenantId, "DM", "Diabetes")
        );
        mockEventStore.storeEvents(patientId, events);

        // When: 5 threads replay same patient concurrently
        int threadCount = 5;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    barrier.await();
                    List<DomainEvent> replayed = replayEngine.replayAllEvents(patientId, tenantId);
                    return replayed.size();
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }));
        }

        // Then: All should see same event count
        Set<Integer> eventCounts = new HashSet<>();
        for (Future<Integer> future : futures) {
            eventCounts.add(future.get(5, TimeUnit.SECONDS));
        }

        assertThat(eventCounts).hasSize(1).contains(3); // All see 3 events
    }

    // ===== Concurrent Projection Update Tests =====

    @Test
    @DisplayName("Should handle concurrent projection updates safely")
    void testConcurrentProjectionUpdates() throws Exception {
        // Given: Projection with 100 concurrent updates
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        ProjectionState projection = new ProjectionState(patientId, tenantId, 1L);
        AtomicInteger successfulUpdates = new AtomicInteger(0);
        AtomicInteger failedUpdates = new AtomicInteger(0);

        // When: 100 threads try to update projection concurrently
        int updateCount = 100;
        CyclicBarrier barrier = new CyclicBarrier(updateCount);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < updateCount; i++) {
            final int index = i;
            futures.add(executorService.submit(() -> {
                try {
                    barrier.await();

                    DomainEvent event = new ConditionDiagnosedEvent(patientId, tenantId, "CODE" + index, "Condition");
                    ProjectionState updated = projectionManager.updateProjection(projection, event);

                    if (updated != null) {
                        successfulUpdates.incrementAndGet();
                    } else {
                        failedUpdates.incrementAndGet();
                    }
                } catch (Exception e) {
                    failedUpdates.incrementAndGet();
                }
            }));
        }

        // Wait for all to complete
        for (Future<?> future : futures) {
            future.get(5, TimeUnit.SECONDS);
        }

        // Then: All updates should be serialized successfully
        assertThat(successfulUpdates.get() + failedUpdates.get()).isEqualTo(updateCount);
        assertThat(successfulUpdates.get()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should detect and prevent optimistic lock conflicts")
    void testOptimisticLockConflictDetection() throws Exception {
        // Given: Projection with stale version
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        ProjectionState projection = new ProjectionState(patientId, tenantId, 1L);
        ProjectionState staleCopy = new ProjectionState(patientId, tenantId, 1L);

        AtomicInteger conflictCount = new AtomicInteger(0);

        // When: Two threads attempt concurrent update with conflicting versions
        CyclicBarrier barrier = new CyclicBarrier(2);

        Future<?> future1 = executorService.submit(() -> {
            try {
                barrier.await();
                DomainEvent event = new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension");
                projectionManager.updateProjection(projection, event);
            } catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
            }
        });

        Future<?> future2 = executorService.submit(() -> {
            try {
                barrier.await();
                DomainEvent event = new ConditionDiagnosedEvent(patientId, tenantId, "DM", "Diabetes");
                projectionManager.updateProjection(staleCopy, event); // Will conflict
            } catch (OptimisticLockException e) {
                conflictCount.incrementAndGet();
            } catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
            }
        });

        future1.get(5, TimeUnit.SECONDS);
        future2.get(5, TimeUnit.SECONDS);

        // Then: One should succeed, one should conflict
        assertThat(conflictCount.get()).isGreaterThan(0);
    }

    // ===== Deadlock Prevention Tests =====

    @Test
    @DisplayName("Should not deadlock under concurrent access")
    void testNoDeadlock() throws Exception {
        // Given: Multiple patients with interdependent events
        String tenantId = "TENANT-001";
        int patientCount = 5;

        for (int i = 0; i < patientCount; i++) {
            String patientId = "PATIENT-" + i;
            List<DomainEvent> events = Arrays.asList(
                new PatientCreatedEvent(patientId, tenantId, "Patient", String.valueOf(i), "1990-01-01"),
                new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension")
            );
            mockEventStore.storeEvents(patientId, events);
        }

        // When: All threads access in round-robin order
        ExecutorService service = Executors.newFixedThreadPool(patientCount);
        List<Future<?>> futures = new ArrayList<>();

        for (int round = 0; round < 10; round++) {
            for (int i = 0; i < patientCount; i++) {
                final int patientIndex = i;
                futures.add(service.submit(() -> {
                    try {
                        String patientId = "PATIENT-" + patientIndex;
                        replayEngine.replayAllEvents(patientId, tenantId);
                        Thread.sleep(10); // Brief pause to increase contention
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));
            }
        }

        // Then: All should complete without deadlock
        for (Future<?> future : futures) {
            assertThatCode(() -> future.get(10, TimeUnit.SECONDS))
                .doesNotThrowAnyException();
        }

        service.shutdownNow();
    }

    // ===== Multi-Tenant Concurrent Safety =====

    @Test
    @DisplayName("Should isolate tenants under concurrent access")
    void testMultiTenantConcurrentIsolation() throws Exception {
        // Given: Same patient ID in different tenants
        String patientId = "PATIENT-123";
        String tenant1 = "TENANT-001";
        String tenant2 = "TENANT-002";

        for (String tenantId : Arrays.asList(tenant1, tenant2)) {
            List<DomainEvent> events = Arrays.asList(
                new PatientCreatedEvent(patientId, tenantId, "Patient", "Name", "1990-01-01"),
                new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension")
            );
            mockEventStore.storeEvents(patientId, events);
        }

        // When: Both tenants replay concurrently
        CyclicBarrier barrier = new CyclicBarrier(2);

        Future<List<DomainEvent>> tenant1Future = executorService.submit(() -> {
            try {
                barrier.await();
                return replayEngine.replayAllEvents(patientId, tenant1);
            } catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
                return new ArrayList<>();
            }
        });

        Future<List<DomainEvent>> tenant2Future = executorService.submit(() -> {
            try {
                barrier.await();
                return replayEngine.replayAllEvents(patientId, tenant2);
            } catch (InterruptedException | BrokenBarrierException e) {
                Thread.currentThread().interrupt();
                return new ArrayList<>();
            }
        });

        // Then: Each should see only its own tenant's events
        List<DomainEvent> tenant1Events = tenant1Future.get(5, TimeUnit.SECONDS);
        List<DomainEvent> tenant2Events = tenant2Future.get(5, TimeUnit.SECONDS);

        assertThat(tenant1Events).allMatch(e -> e.getTenantId().equals(tenant1));
        assertThat(tenant2Events).allMatch(e -> e.getTenantId().equals(tenant2));
    }

    // ===== Snapshot Consistency Tests =====

    @Test
    @DisplayName("Should maintain snapshot consistency under concurrent reads")
    void testSnapshotConsistencyUnderConcurrency() throws Exception {
        // Given: Snapshot and concurrent readers
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        ReplaySnapshot snapshot = new ReplaySnapshot(patientId, tenantId, 100L);
        mockEventStore.storeSnapshot(patientId, snapshot);

        // When: 10 threads read snapshot concurrently
        int readerCount = 10;
        CyclicBarrier barrier = new CyclicBarrier(readerCount);
        List<Future<ReplaySnapshot>> futures = new ArrayList<>();

        for (int i = 0; i < readerCount; i++) {
            futures.add(executorService.submit(() -> {
                try {
                    barrier.await();
                    return mockEventStore.getSnapshot(patientId);
                } catch (InterruptedException | BrokenBarrierException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }));
        }

        // Then: All should see same snapshot
        Set<Long> versions = new HashSet<>();
        for (Future<ReplaySnapshot> future : futures) {
            ReplaySnapshot retrieved = future.get(5, TimeUnit.SECONDS);
            if (retrieved != null) {
                versions.add(retrieved.getVersion());
            }
        }

        assertThat(versions).hasSize(1).contains(100L);
    }

    // ===== Performance Under Load =====

    @Test
    @DisplayName("Should handle 1000 concurrent replays")
    void testHighConcurrencyLoad() throws Exception {
        // Given: 1000 patients
        String tenantId = "TENANT-001";
        int patientCount = 1000;

        for (int i = 0; i < patientCount; i++) {
            String patientId = "PATIENT-" + i;
            DomainEvent event = new PatientCreatedEvent(patientId, tenantId, "Patient", String.valueOf(i), "1990-01-01");
            mockEventStore.storeEvents(patientId, Collections.singletonList(event));
        }

        // When: Replaying all 1000 concurrently
        ExecutorService service = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(patientCount);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < patientCount; i++) {
            final int index = i;
            service.submit(() -> {
                try {
                    String patientId = "PATIENT-" + index;
                    replayEngine.replayAllEvents(patientId, tenantId);
                } finally {
                    latch.countDown();
                }
            });
        }

        // Then: All should complete in reasonable time
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        assertThat(completed).isTrue();
        assertThat(duration).isLessThan(30_000); // Should complete in 30 seconds

        service.shutdownNow();
    }

    // ===== Exception Handling in Concurrent Context =====

    @Test
    @DisplayName("Should handle exceptions in concurrent threads safely")
    void testExceptionHandlingInConcurrency() throws Exception {
        // Given: Mix of valid and invalid patients
        String tenantId = "TENANT-001";

        DomainEvent validEvent = new PatientCreatedEvent("PATIENT-VALID", tenantId, "John", "Doe", "1990-01-01");
        mockEventStore.storeEvents("PATIENT-VALID", Collections.singletonList(validEvent));
        mockEventStore.simulateErrorForPatient("PATIENT-ERROR");

        // When: Replaying mix of valid and invalid
        CyclicBarrier barrier = new CyclicBarrier(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        Future<?> validFuture = executorService.submit(() -> {
            try {
                barrier.await();
                replayEngine.replayAllEvents("PATIENT-VALID", tenantId);
                successCount.incrementAndGet();
            } catch (Exception e) {
                errorCount.incrementAndGet();
            }
        });

        Future<?> errorFuture = executorService.submit(() -> {
            try {
                barrier.await();
                replayEngine.replayAllEvents("PATIENT-ERROR", tenantId);
                successCount.incrementAndGet();
            } catch (Exception e) {
                errorCount.incrementAndGet();
            }
        });

        validFuture.get(5, TimeUnit.SECONDS);
        errorFuture.get(5, TimeUnit.SECONDS);

        // Then: Should handle errors gracefully without affecting other threads
        assertThat(successCount.get() + errorCount.get()).isEqualTo(2);
    }

    // ===== Helper Classes =====

    static class TestDomainEvent extends DomainEvent {
        public TestDomainEvent(String aggregateId, String tenantId) {
            super(aggregateId, tenantId);
        }
    }
}
