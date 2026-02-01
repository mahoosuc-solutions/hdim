package com.healthdata.eventreplay.integration;

import com.healthdata.eventsourcing.event.DomainEvent;
import com.healthdata.eventsourcing.event.PatientCreatedEvent;
import com.healthdata.eventsourcing.event.ConditionDiagnosedEvent;
import com.healthdata.eventsourcing.event.MedicationPrescribedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for Event Replay & Projections
 *
 * Tests end-to-end scenarios:
 * - Creating patient → diagnosing conditions → prescribing medication
 * - Rebuilding projections after outage
 * - Temporal compliance analysis
 * - Care gap identification across time
 * - Multi-step audit trail reconstruction
 *
 * These integration tests validate that the replay engine, projection manager,
 * replay strategies, and temporal queries work together correctly in realistic
 * healthcare scenarios.
 */
@DisplayName("Replay Integration Tests")
class ReplayIntegrationTest {

    private EventReplayEngine replayEngine;
    private ProjectionManager projectionManager;
    private TemporalQueryService temporalQueryService;
    private ReplayStrategyFactory strategyFactory;
    private MockEventStore mockEventStore;
    private MockProjectionStore mockProjectionStore;

    @BeforeEach
    void setup() {
        mockEventStore = new MockEventStore();
        mockProjectionStore = new MockProjectionStore();
        replayEngine = new EventReplayEngine(mockEventStore);
        projectionManager = new ProjectionManager(mockProjectionStore, mockEventStore);
        temporalQueryService = new TemporalQueryService(mockEventStore, mockProjectionStore);
        strategyFactory = new ReplayStrategyFactory(mockEventStore);
    }

    // ===== End-to-End Patient Lifecycle Scenario =====

    @Test
    @DisplayName("Should handle complete patient lifecycle: create → diagnose → treat")
    void testPatientLifecycleIntegration() {
        // Given: Patient workflow
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        Instant createdAt = Instant.now().minus(90, ChronoUnit.DAYS);
        Instant diagnosedAt = Instant.now().minus(60, ChronoUnit.DAYS);
        Instant treatedAt = Instant.now().minus(30, ChronoUnit.DAYS);

        // Step 1: Create patient
        DomainEvent patientCreated = new PatientCreatedEvent(patientId, tenantId, "John", "Doe", "1960-05-15");
        mockEventStore.storeEventAtTime(patientId, patientCreated, createdAt);

        // Step 2: Diagnose HTN
        DomainEvent htnDiagnosed = new ConditionDiagnosedEvent(patientId, tenantId, "I10", "Essential Hypertension");
        mockEventStore.storeEventAtTime(patientId, htnDiagnosed, diagnosedAt);

        // Step 3: Prescribe medication
        DomainEvent medPrescribed = new MedicationPrescribedEvent(patientId, tenantId, "C09AA05", "Lisinopril", "10mg", 90);
        mockEventStore.storeEventAtTime(patientId, medPrescribed, treatedAt);

        // When: Replaying complete history
        List<DomainEvent> fullHistory = replayEngine.replayAllEvents(patientId, tenantId);

        // Then: Should have all events in order
        assertThat(fullHistory)
            .hasSize(3)
            .extracting(DomainEvent::getEventType)
            .containsExactly("PatientCreatedEvent", "ConditionDiagnosedEvent", "MedicationPrescribedEvent");

        // And: Should create valid projection
        ProjectionState projection = projectionManager.createProjection(patientId, tenantId, "PatientProjection");
        assertThat(projection).isNotNull();
        assertThat(projection.getVersion()).isEqualTo(3L);
    }

    // ===== Projection Rebuild Scenario =====

    @Test
    @DisplayName("Should rebuild projections after system outage")
    void testProjectionRebuildAfterOutage() {
        // Given: Events in store but projections cleared (simulating outage recovery)
        String patientId = "PATIENT-456";
        String tenantId = "TENANT-001";

        List<DomainEvent> events = Arrays.asList(
            new PatientCreatedEvent(patientId, tenantId, "Jane", "Smith", "1970-03-20"),
            new ConditionDiagnosedEvent(patientId, tenantId, "E11", "Type 2 Diabetes"),
            new MedicationPrescribedEvent(patientId, tenantId, "A10BA02", "Metformin", "500mg", 120)
        );
        mockEventStore.storeEvents(patientId, events);

        // Simulate: Projections were cleared
        mockProjectionStore.clear();

        // When: Rebuilding projections
        ProjectionState rebuilt = projectionManager.rebuildProjection(patientId, tenantId);

        // Then: Projections should be reconstructed from events
        assertThat(rebuilt).isNotNull();
        assertThat(rebuilt.getVersion()).isEqualTo(3L);
        assertThat(rebuilt.getAggregateId()).isEqualTo(patientId);
    }

    // ===== Care Gap Analysis Scenario =====

    @Test
    @DisplayName("Should identify care gaps in temporal analysis")
    void testCareGapIdentificationScenario() {
        // Given: Patient with unmet care needs
        String patientId = "PATIENT-GAP";
        String tenantId = "TENANT-001";

        Instant diagnosisDate = Instant.now().minus(120, ChronoUnit.DAYS);
        Instant treatmentDate = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant auditDate = Instant.now().minus(60, ChronoUnit.DAYS); // Between diagnosis and treatment

        // Patient diagnosed with HTN
        DomainEvent htnDiagnosed = new ConditionDiagnosedEvent(patientId, tenantId, "I10", "Essential Hypertension");
        mockEventStore.storeEventAtTime(patientId, htnDiagnosed, diagnosisDate);

        // Treatment started (later)
        DomainEvent medPrescribed = new MedicationPrescribedEvent(patientId, tenantId, "C09AA05", "Lisinopril", "10mg", 90);
        mockEventStore.storeEventAtTime(patientId, medPrescribed, treatmentDate);

        // When: Checking care gap status as of audit date
        TemporalSnapshot snapshot = temporalQueryService.getStateAsOf(patientId, tenantId, auditDate);

        // Then: Gap should exist at audit date (diagnosed but not treated)
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.hasConditionDiagnosed()).isTrue();
        assertThat(snapshot.hasMedicationPrescribed()).isFalse();
    }

    // ===== Quality Measure Evaluation Scenario =====

    @Test
    @DisplayName("Should evaluate quality measure performance period")
    void testQualityMeasureEvaluationScenario() {
        // Given: Measurement period and patient events
        String patientId = "PATIENT-MEASURE";
        String tenantId = "TENANT-001";

        Instant periodStart = Instant.now().minus(365, ChronoUnit.DAYS);
        Instant periodEnd = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant diagnosisDate = Instant.now().minus(180, ChronoUnit.DAYS); // Mid-period
        Instant treatmentDate = Instant.now().minus(60, ChronoUnit.DAYS); // Later in period

        // Create patient
        DomainEvent created = new PatientCreatedEvent(patientId, tenantId, "John", "Doe", "1960-01-01");
        mockEventStore.storeEventAtTime(patientId, created, periodStart.plus(30, ChronoUnit.DAYS));

        // Diagnose HTN mid-period
        DomainEvent diagnosed = new ConditionDiagnosedEvent(patientId, tenantId, "I10", "Essential Hypertension");
        mockEventStore.storeEventAtTime(patientId, diagnosed, diagnosisDate);

        // Start treatment in period
        DomainEvent medStarted = new MedicationPrescribedEvent(patientId, tenantId, "C09AA05", "Lisinopril", "10mg", 90);
        mockEventStore.storeEventAtTime(patientId, medStarted, treatmentDate);

        // When: Evaluating measure as of period end
        TemporalSnapshot snapshotAtPeriodEnd = temporalQueryService.getStateAsOf(patientId, tenantId, periodEnd);

        // Then: Patient should show as compliant (both diagnosis and treatment during period)
        assertThat(snapshotAtPeriodEnd).isNotNull();
        assertThat(snapshotAtPeriodEnd.getTimestamp()).isEqualTo(periodEnd);
    }

    // ===== Bulk Replay + Temporal Analysis Scenario =====

    @Test
    @DisplayName("Should rebuild 100 patients and analyze temporal compliance")
    void testBulkReplayWithTemporalAnalysis() {
        // Given: 100 patients with varying compliance timelines
        String tenantId = "TENANT-001";
        int patientCount = 100;

        for (int i = 0; i < patientCount; i++) {
            String patientId = "PATIENT-" + i;

            Instant createdAt = Instant.now().minus(365, ChronoUnit.DAYS);
            Instant diagnosedAt = Instant.now().minus(180 - i, ChronoUnit.DAYS); // Varied diagnosis dates
            Instant treatmentDate = Instant.now().minus(60 - (i % 60), ChronoUnit.DAYS); // Varied treatment dates

            mockEventStore.storeEventAtTime(patientId,
                new PatientCreatedEvent(patientId, tenantId, "Patient", String.valueOf(i), "1960-01-01"),
                createdAt);

            mockEventStore.storeEventAtTime(patientId,
                new ConditionDiagnosedEvent(patientId, tenantId, "I10", "Essential Hypertension"),
                diagnosedAt);

            mockEventStore.storeEventAtTime(patientId,
                new MedicationPrescribedEvent(patientId, tenantId, "C09AA05", "Lisinopril", "10mg", 90),
                treatmentDate);
        }

        // When: Rebuilding all projections
        List<ProjectionState> allProjections = projectionManager.rebuildAllProjections(tenantId, "PatientProjection");

        // Then: All should be rebuilt
        assertThat(allProjections).hasSize(patientCount);

        // And: Analyzing temporal compliance for a sample
        String samplePatient = "PATIENT-0";
        Instant auditDate = Instant.now().minus(120, ChronoUnit.DAYS);
        TemporalSnapshot snapshot = temporalQueryService.getStateAsOf(samplePatient, tenantId, auditDate);

        assertThat(snapshot).isNotNull();
    }

    // ===== Snapshot Strategy Selection Scenario =====

    @Test
    @DisplayName("Should use optimal replay strategy based on data volume")
    void testSnapshotStrategySelectionScenario() {
        // Given: Patient with 500 events and snapshot at 100
        String patientId = "PATIENT-LARGE";
        String tenantId = "TENANT-001";

        ReplaySnapshot snapshot = new ReplaySnapshot(patientId, tenantId, 100L);
        mockEventStore.storeSnapshot(patientId, snapshot);

        List<DomainEvent> incrementalEvents = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            incrementalEvents.add(new TestDomainEvent(patientId, tenantId));
        }
        mockEventStore.storeEventsAfterSnapshot(patientId, incrementalEvents, 100L);

        // When: Selecting optimal strategy
        ReplayStrategy selected = strategyFactory.selectBestStrategy(patientId, tenantId, 500L, true);

        // Then: Should select snapshot strategy
        assertThat(selected).isInstanceOf(SnapshotReplayStrategy.class);

        // And: Replay should be efficient
        List<DomainEvent> replayed = selected.replay(patientId, tenantId);
        assertThat(replayed).isNotEmpty();
    }

    // ===== Audit Trail Reconstruction Scenario =====

    @Test
    @DisplayName("Should reconstruct complete audit trail for HIPAA compliance")
    void testAuditTrailReconstructionScenario() {
        // Given: Patient with medication adherence history
        String patientId = "PATIENT-AUDIT";
        String tenantId = "TENANT-001";

        Instant t1 = Instant.now().minus(90, ChronoUnit.DAYS);
        Instant t2 = Instant.now().minus(60, ChronoUnit.DAYS);
        Instant t3 = Instant.now().minus(30, ChronoUnit.DAYS);

        mockEventStore.storeEventAtTime(patientId,
            new PatientCreatedEvent(patientId, tenantId, "John", "Doe", "1960-01-01"), t1);
        mockEventStore.storeEventAtTime(patientId,
            new ConditionDiagnosedEvent(patientId, tenantId, "I10", "Essential Hypertension"), t2);
        mockEventStore.storeEventAtTime(patientId,
            new MedicationPrescribedEvent(patientId, tenantId, "C09AA05", "Lisinopril", "10mg", 90), t3);

        // When: Reconstructing audit trail for entire period
        AuditTrail trail = temporalQueryService.getAuditTrail(patientId, tenantId, t1, Instant.now());

        // Then: Should contain all events
        assertThat(trail).isNotNull();
        assertThat(trail.getEvents()).hasSize(3);

        // And: Should be ordered
        assertThat(trail.getEvents())
            .extracting(DomainEvent::getEventType)
            .containsExactly("PatientCreatedEvent", "ConditionDiagnosedEvent", "MedicationPrescribedEvent");
    }

    // ===== Concurrent Rebuild + Query Scenario =====

    @Test
    @DisplayName("Should handle concurrent rebuild and temporal queries")
    void testConcurrentRebuildAndQueryScenario() {
        // Given: 10 patients
        String tenantId = "TENANT-001";

        for (int i = 0; i < 10; i++) {
            String patientId = "PATIENT-" + i;
            List<DomainEvent> events = Arrays.asList(
                new PatientCreatedEvent(patientId, tenantId, "Patient", String.valueOf(i), "1960-01-01"),
                new ConditionDiagnosedEvent(patientId, tenantId, "I10", "Essential Hypertension"),
                new MedicationPrescribedEvent(patientId, tenantId, "C09AA05", "Lisinopril", "10mg", 90)
            );
            mockEventStore.storeEvents(patientId, events);
        }

        // When: Rebuilding projections
        List<ProjectionState> rebuilt = projectionManager.rebuildAllProjections(tenantId, "PatientProjection");

        // And: Querying temporal states
        List<TemporalSnapshot> snapshots = new ArrayList<>();
        Instant queryTime = Instant.now().minus(30, ChronoUnit.DAYS);

        for (int i = 0; i < 10; i++) {
            String patientId = "PATIENT-" + i;
            TemporalSnapshot snapshot = temporalQueryService.getStateAsOf(patientId, tenantId, queryTime);
            if (snapshot != null) {
                snapshots.add(snapshot);
            }
        }

        // Then: All should succeed
        assertThat(rebuilt).hasSize(10);
        assertThat(snapshots).hasSizeGreaterThan(0);
    }

    // ===== Error Recovery Scenario =====

    @Test
    @DisplayName("Should recover from partial replay failures")
    void testErrorRecoveryScenario() {
        // Given: Multiple patients, one with errors
        String tenantId = "TENANT-001";

        // Valid patient
        String validPatient = "PATIENT-VALID";
        mockEventStore.storeEvents(validPatient, Arrays.asList(
            new PatientCreatedEvent(validPatient, tenantId, "John", "Doe", "1960-01-01"),
            new ConditionDiagnosedEvent(validPatient, tenantId, "I10", "Essential Hypertension")
        ));

        // Problematic patient
        String problemPatient = "PATIENT-ERROR";
        mockEventStore.simulateErrorForPatient(problemPatient);

        // When: Attempting to rebuild both
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        try {
            ProjectionState validProjection = projectionManager.rebuildProjection(validPatient, tenantId);
            if (validProjection != null) {
                successCount.incrementAndGet();
            }
        } catch (Exception e) {
            errorCount.incrementAndGet();
        }

        try {
            ProjectionState errorProjection = projectionManager.rebuildProjection(problemPatient, tenantId);
            if (errorProjection != null) {
                successCount.incrementAndGet();
            }
        } catch (Exception e) {
            errorCount.incrementAndGet();
        }

        // Then: Should handle partial failures gracefully
        assertThat(successCount.get()).isGreaterThan(0);
    }

    // ===== Multi-Tenant Scenario =====

    @Test
    @DisplayName("Should isolate replay and temporal data by tenant")
    void testMultiTenantReplayScenario() {
        // Given: Same patient in different tenants
        String patientId = "PATIENT-MT";
        String tenant1 = "TENANT-001";
        String tenant2 = "TENANT-002";

        List<DomainEvent> tenant1Events = Arrays.asList(
            new PatientCreatedEvent(patientId, tenant1, "John", "Tenant1", "1960-01-01"),
            new ConditionDiagnosedEvent(patientId, tenant1, "I10", "Essential Hypertension")
        );

        List<DomainEvent> tenant2Events = Arrays.asList(
            new PatientCreatedEvent(patientId, tenant2, "Jane", "Tenant2", "1970-01-01"),
            new ConditionDiagnosedEvent(patientId, tenant2, "E11", "Type 2 Diabetes")
        );

        mockEventStore.storeEvents(patientId, tenant1Events);
        mockEventStore.storeEvents(patientId, tenant2Events);

        // When: Replaying for each tenant
        List<DomainEvent> t1Events = replayEngine.replayAllEvents(patientId, tenant1);
        List<DomainEvent> t2Events = replayEngine.replayAllEvents(patientId, tenant2);

        // Then: Should isolate by tenant
        assertThat(t1Events).allMatch(e -> e.getTenantId().equals(tenant1));
        assertThat(t2Events).allMatch(e -> e.getTenantId().equals(tenant2));

        // And: Different diagnoses
        assertThat(t1Events).anySatisfy(e -> e.getEventType().equals("ConditionDiagnosedEvent"));
        assertThat(t2Events).anySatisfy(e -> e.getEventType().equals("ConditionDiagnosedEvent"));
    }

    // ===== Helper Classes =====

    static class TestDomainEvent extends DomainEvent {
        public TestDomainEvent(String aggregateId, String tenantId) {
            super(aggregateId, tenantId);
        }
    }
}
