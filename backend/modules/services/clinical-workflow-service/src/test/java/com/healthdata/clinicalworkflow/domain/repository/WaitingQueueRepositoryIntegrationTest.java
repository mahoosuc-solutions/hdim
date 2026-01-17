package com.healthdata.clinicalworkflow.domain.repository;

import com.healthdata.clinicalworkflow.config.BaseIntegrationTest;
import com.healthdata.clinicalworkflow.domain.model.WaitingQueueEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@BaseIntegrationTest
@DisplayName("WaitingQueueRepository Integration Tests")
class WaitingQueueRepositoryIntegrationTest {

    @Autowired
    private WaitingQueueRepository waitingQueueRepository;

    private static final String TENANT_ID = "test-tenant";
    private static final String OTHER_TENANT = "other-tenant";
    private static final UUID PATIENT_ID_1 = UUID.randomUUID();
    private static final UUID PATIENT_ID_2 = UUID.randomUUID();
    private static final UUID PATIENT_ID_3 = UUID.randomUUID();

    private WaitingQueueEntity normalPriority;
    private WaitingQueueEntity highPriority;
    private WaitingQueueEntity urgentPriority;

    @BeforeEach
    void setUp() {
        waitingQueueRepository.deleteAll();
        
        normalPriority = createQueueEntry(TENANT_ID, PATIENT_ID_1, "normal", 3, "waiting");
        highPriority = createQueueEntry(TENANT_ID, PATIENT_ID_2, "high", 2, "waiting");
        urgentPriority = createQueueEntry(TENANT_ID, PATIENT_ID_3, "urgent", 1, "waiting");

        normalPriority = waitingQueueRepository.save(normalPriority);
        highPriority = waitingQueueRepository.save(highPriority);
        urgentPriority = waitingQueueRepository.save(urgentPriority);
    }

    @Nested
    @DisplayName("Queue Management Queries")
    class QueueManagementTests {

        @Test
        @DisplayName("Should find waiting patients by tenant")
        void shouldFindWaitingPatientsByTenant() {
            List<WaitingQueueEntity> waiting = waitingQueueRepository.findWaitingPatientsByTenant(TENANT_ID);

            assertThat(waiting).hasSize(3);
            assertThat(waiting).allMatch(w -> w.getStatus().equals("waiting"));
        }

        @Test
        @DisplayName("Should order by priority")
        void shouldOrderByPriority() {
            List<WaitingQueueEntity> waiting = waitingQueueRepository.findWaitingPatientsByTenant(TENANT_ID);

            assertThat(waiting.get(0).getPriority()).isEqualTo("urgent");
            assertThat(waiting.get(1).getPriority()).isEqualTo("high");
            assertThat(waiting.get(2).getPriority()).isEqualTo("normal");
        }

        @Test
        @DisplayName("Should find next patient in queue")
        void shouldFindNextPatientInQueue() {
            Optional<WaitingQueueEntity> next = waitingQueueRepository.findNextPatientInQueue(TENANT_ID);

            assertThat(next).isPresent();
            assertThat(next.get().getPriority()).isEqualTo("urgent");
            assertThat(next.get().getPatientId()).isEqualTo(PATIENT_ID_3);
        }

        @Test
        @DisplayName("Should count waiting patients")
        void shouldCountWaitingPatients() {
            long count = waitingQueueRepository.countWaitingPatients(TENANT_ID);

            assertThat(count).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Priority-Based Queries")
    class PriorityBasedQueryTests {

        @Test
        @DisplayName("Should find queue by priority")
        void shouldFindQueueByPriority() {
            List<WaitingQueueEntity> urgent = waitingQueueRepository.findQueueByPriority(TENANT_ID, "urgent");

            assertThat(urgent).hasSize(1);
            assertThat(urgent.get(0).getPriority()).isEqualTo("urgent");
        }

        @Test
        @DisplayName("Should find urgent patients")
        void shouldFindUrgentPatients() {
            List<WaitingQueueEntity> urgent = waitingQueueRepository.findUrgentPatients(TENANT_ID);

            assertThat(urgent).hasSize(1);
            assertThat(urgent.get(0).getPriority()).isEqualTo("urgent");
        }

        @Test
        @DisplayName("Should calculate estimated wait time")
        void shouldCalculateEstimatedWaitTime() {
            // Complete some queue entries to get historical data
            normalPriority.setWaitTimeMinutes(15);
            normalPriority.setExitedQueueAt(Instant.now());
            waitingQueueRepository.save(normalPriority);

            Integer avgWait = waitingQueueRepository.getEstimatedWaitTime(TENANT_ID, "normal");

            assertThat(avgWait).isNotNull();
            assertThat(avgWait).isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("Patient Queue Position Queries")
    class PatientQueuePositionTests {

        @Test
        @DisplayName("Should find patient queue position")
        void shouldFindPatientQueuePosition() {
            Optional<WaitingQueueEntity> found = waitingQueueRepository.findPatientQueuePosition(PATIENT_ID_1, TENANT_ID);

            assertThat(found).isPresent();
            assertThat(found.get().getPatientId()).isEqualTo(PATIENT_ID_1);
            assertThat(found.get().getQueuePosition()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should handle patient not in queue")
        void shouldHandlePatientNotInQueue() {
            UUID nonExistentPatient = UUID.randomUUID();
            Optional<WaitingQueueEntity> found = waitingQueueRepository.findPatientQueuePosition(nonExistentPatient, TENANT_ID);

            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("Should find all queue entries for patient")
        void shouldFindAllQueueEntriesForPatient() {
            // Create additional entry for same patient
            WaitingQueueEntity oldEntry = createQueueEntry(TENANT_ID, PATIENT_ID_1, "normal", 5, "completed");
            oldEntry.setEnteredQueueAt(Instant.now().minusSeconds(7200));
            oldEntry.setExitedQueueAt(Instant.now().minusSeconds(3600));
            waitingQueueRepository.save(oldEntry);

            List<WaitingQueueEntity> entries = waitingQueueRepository.findByTenantIdAndPatientIdOrderByEnteredQueueAtDesc(TENANT_ID, PATIENT_ID_1);

            assertThat(entries).hasSize(2);
            assertThat(entries).allMatch(e -> e.getPatientId().equals(PATIENT_ID_1));
        }
    }

    @Nested
    @DisplayName("Status-Based Queries")
    class StatusBasedQueryTests {

        @Test
        @DisplayName("Should find entries by status")
        void shouldFindEntriesByStatus() {
            List<WaitingQueueEntity> waiting = waitingQueueRepository.findByTenantIdAndStatusOrderByEnteredQueueAtAsc(TENANT_ID, "waiting");

            assertThat(waiting).hasSize(3);
            assertThat(waiting).allMatch(w -> w.getStatus().equals("waiting"));
        }

        @Test
        @DisplayName("Should order by entered time ascending")
        void shouldOrderByEnteredTimeAscending() {
            // Set different entry times
            urgentPriority.setEnteredQueueAt(Instant.now().minusSeconds(300));
            highPriority.setEnteredQueueAt(Instant.now().minusSeconds(200));
            normalPriority.setEnteredQueueAt(Instant.now().minusSeconds(100));
            waitingQueueRepository.save(urgentPriority);
            waitingQueueRepository.save(highPriority);
            waitingQueueRepository.save(normalPriority);

            List<WaitingQueueEntity> waiting = waitingQueueRepository.findByTenantIdAndStatusOrderByEnteredQueueAtAsc(TENANT_ID, "waiting");

            for (int i = 0; i < waiting.size() - 1; i++) {
                assertThat(waiting.get(i).getEnteredQueueAt())
                    .isBeforeOrEqualTo(waiting.get(i + 1).getEnteredQueueAt());
            }
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation")
    class MultiTenantIsolationTests {

        @Test
        @DisplayName("Should isolate queue data between tenants")
        void shouldIsolateQueueDataBetweenTenants() {
            WaitingQueueEntity otherTenantEntry = createQueueEntry(OTHER_TENANT, UUID.randomUUID(), "urgent", 1, "waiting");
            waitingQueueRepository.save(otherTenantEntry);

            List<WaitingQueueEntity> tenant1Queue = waitingQueueRepository.findWaitingPatientsByTenant(TENANT_ID);
            List<WaitingQueueEntity> tenant2Queue = waitingQueueRepository.findWaitingPatientsByTenant(OTHER_TENANT);

            assertThat(tenant1Queue).noneMatch(q -> q.getTenantId().equals(OTHER_TENANT));
            assertThat(tenant2Queue).noneMatch(q -> q.getTenantId().equals(TENANT_ID));
            assertThat(tenant1Queue).hasSize(3);
            assertThat(tenant2Queue).hasSize(1);
        }

        @Test
        @DisplayName("Should count only tenant's own waiting patients")
        void shouldCountOnlyTenantOwnWaitingPatients() {
            WaitingQueueEntity otherTenantEntry = createQueueEntry(OTHER_TENANT, UUID.randomUUID(), "urgent", 1, "waiting");
            waitingQueueRepository.save(otherTenantEntry);

            long tenant1Count = waitingQueueRepository.countWaitingPatients(TENANT_ID);
            long tenant2Count = waitingQueueRepository.countWaitingPatients(OTHER_TENANT);

            assertThat(tenant1Count).isEqualTo(3);
            assertThat(tenant2Count).isEqualTo(1);
        }
    }

    private WaitingQueueEntity createQueueEntry(String tenantId, UUID patientId, String priority, int position, String status) {
        return WaitingQueueEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .priority(priority)
                .queuePosition(position)
                .status(status)
                .enteredQueueAt(Instant.now())
                .build();
    }
}
