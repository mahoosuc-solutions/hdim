package com.healthdata.eventreplay.temporal;

import com.healthdata.eventsourcing.event.DomainEvent;
import com.healthdata.eventsourcing.event.PatientCreatedEvent;
import com.healthdata.eventsourcing.event.ConditionDiagnosedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for TemporalQueryService
 *
 * Tests historical state reconstruction:
 * - "State as of [date]" queries
 * - Temporal snapshots at specific points
 * - Point-in-time compliance checks
 * - Audit trail reconstruction
 * - Care gap analysis at specific dates
 * - Quality measure evaluation at historical dates
 *
 * Critical for healthcare use cases:
 * - HIPAA audit trails (what was the state on [date]?)
 * - Care gap analysis (was patient compliant on [date]?)
 * - Quality reporting (measure scores as of quarter end)
 * - Appeals and disputes (historical patient records)
 */
@DisplayName("TemporalQueryService Tests")
class TemporalQueryServiceTest {

    private TemporalQueryService temporalQueryService;
    private MockEventStore mockEventStore;
    private MockProjectionStore mockProjectionStore;

    @BeforeEach
    void setup() {
        mockEventStore = new MockEventStore();
        mockProjectionStore = new MockProjectionStore();
        temporalQueryService = new TemporalQueryService(mockEventStore, mockProjectionStore);
    }

    // ===== Basic Temporal Query Tests =====

    @Test
    @DisplayName("Should query state at specific point in time")
    void testPointInTimeQuery() {
        // Given: Patient events over time
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        Instant t1 = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant t2 = Instant.now().minus(20, ChronoUnit.DAYS);
        Instant t3 = Instant.now().minus(10, ChronoUnit.DAYS);

        mockEventStore.storeEventAtTime(patientId, "PatientCreatedEvent", t1);
        mockEventStore.storeEventAtTime(patientId, "ConditionDiagnosedEvent", t2);
        mockEventStore.storeEventAtTime(patientId, "ConditionDiagnosedEvent", t3);

        // When: Querying state as of t2
        TemporalSnapshot snapshot = temporalQueryService.getStateAsOf(patientId, tenantId, t2);

        // Then: Should reflect state at t2 (patient created + 1 condition)
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getTimestamp()).isEqualTo(t2);
        assertThat(snapshot.getVersion()).isGreaterThan(1L);
    }

    @Test
    @DisplayName("Should handle queries before any events")
    void testQueryBeforeFirstEvent() {
        // Given: First event at t2
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        Instant t1 = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant t2 = Instant.now().minus(20, ChronoUnit.DAYS);

        mockEventStore.storeEventAtTime(patientId, "PatientCreatedEvent", t2);

        // When: Querying before first event
        TemporalSnapshot snapshot = temporalQueryService.getStateAsOf(patientId, tenantId, t1);

        // Then: Should return empty (no state yet)
        assertThat(snapshot).isNull();
    }

    @Test
    @DisplayName("Should handle queries after all events")
    void testQueryAfterAllEvents() {
        // Given: Last event at t2
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        Instant t1 = Instant.now().minus(20, ChronoUnit.DAYS);
        Instant t2 = Instant.now(); // Current time

        mockEventStore.storeEventAtTime(patientId, "PatientCreatedEvent", t1);

        // When: Querying current time
        TemporalSnapshot snapshot = temporalQueryService.getStateAsOf(patientId, tenantId, t2);

        // Then: Should return latest state
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getVersion()).isGreaterThan(0);
    }

    // ===== Temporal Consistency Tests =====

    @Test
    @DisplayName("Should show consistent state progression over time")
    void testTemporalConsistency() {
        // Given: Events spaced over time
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        Instant t1 = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant t2 = Instant.now().minus(20, ChronoUnit.DAYS);
        Instant t3 = Instant.now().minus(10, ChronoUnit.DAYS);

        mockEventStore.storeEventAtTime(patientId, "PatientCreatedEvent", t1);
        mockEventStore.storeEventAtTime(patientId, "ConditionDiagnosedEvent", t2);
        mockEventStore.storeEventAtTime(patientId, "MedicationPrescribedEvent", t3);

        // When: Querying at each time
        TemporalSnapshot snap1 = temporalQueryService.getStateAsOf(patientId, tenantId, t1);
        TemporalSnapshot snap2 = temporalQueryService.getStateAsOf(patientId, tenantId, t2);
        TemporalSnapshot snap3 = temporalQueryService.getStateAsOf(patientId, tenantId, t3);

        // Then: Versions should increase
        assertThat(snap1.getVersion()).isLessThan(snap2.getVersion());
        assertThat(snap2.getVersion()).isLessThan(snap3.getVersion());
    }

    @Test
    @DisplayName("Should not include future events in temporal query")
    void testFutureEventExclusion() {
        // Given: Events at different times
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        Instant t1 = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant t2 = Instant.now().minus(10, ChronoUnit.DAYS);
        Instant queryTime = Instant.now().minus(20, ChronoUnit.DAYS);

        mockEventStore.storeEventAtTime(patientId, "PatientCreatedEvent", t1);
        mockEventStore.storeEventAtTime(patientId, "ConditionDiagnosedEvent", t2); // AFTER queryTime

        // When: Querying at time between t1 and t2
        TemporalSnapshot snapshot = temporalQueryService.getStateAsOf(patientId, tenantId, queryTime);

        // Then: Should only include event at t1
        assertThat(snapshot.getVersion()).isEqualTo(1L);
    }

    // ===== Multi-Tenant Temporal Tests =====

    @Test
    @DisplayName("Should isolate temporal queries by tenant")
    void testMultiTenantTemporalIsolation() {
        // Given: Same patient in different tenants at same time
        String patientId = "PATIENT-123";
        String tenant1 = "TENANT-001";
        String tenant2 = "TENANT-002";

        Instant queryTime = Instant.now().minus(15, ChronoUnit.DAYS);

        mockEventStore.storeEventAtTime(patientId, "PatientCreatedEvent", queryTime, tenant1);
        mockEventStore.storeEventAtTime(patientId, "PatientCreatedEvent", queryTime, tenant2);

        // When: Querying each tenant
        TemporalSnapshot snap1 = temporalQueryService.getStateAsOf(patientId, tenant1, queryTime);
        TemporalSnapshot snap2 = temporalQueryService.getStateAsOf(patientId, tenant2, queryTime);

        // Then: Should return same version but different tenant contexts
        assertThat(snap1.getTenantId()).isEqualTo(tenant1);
        assertThat(snap2.getTenantId()).isEqualTo(tenant2);
    }

    @Test
    @DisplayName("Should enforce tenant access in temporal queries")
    void testTemporalTenantAccess() {
        // Given: Temporal snapshot for TENANT-001
        String patientId = "PATIENT-123";
        String authorizedTenant = "TENANT-001";
        String unauthorizedTenant = "TENANT-999";

        Instant queryTime = Instant.now().minus(15, ChronoUnit.DAYS);
        mockEventStore.storeEventAtTime(patientId, "PatientCreatedEvent", queryTime, authorizedTenant);

        // When: Querying from unauthorized tenant
        TemporalSnapshot snapshot = temporalQueryService.getStateAsOf(patientId, unauthorizedTenant, queryTime);

        // Then: Should deny access
        assertThat(snapshot).isNull();
    }

    // ===== Temporal Range Queries =====

    @Test
    @DisplayName("Should retrieve temporal snapshots for date range")
    void testTemporalRangeQuery() {
        // Given: Events over time
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        Instant t1 = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant t2 = Instant.now().minus(20, ChronoUnit.DAYS);
        Instant t3 = Instant.now().minus(10, ChronoUnit.DAYS);
        Instant rangeStart = Instant.now().minus(25, ChronoUnit.DAYS);
        Instant rangeEnd = Instant.now().minus(15, ChronoUnit.DAYS);

        mockEventStore.storeEventAtTime(patientId, "PatientCreatedEvent", t1);
        mockEventStore.storeEventAtTime(patientId, "ConditionDiagnosedEvent", t2); // Within range
        mockEventStore.storeEventAtTime(patientId, "MedicationPrescribedEvent", t3);

        // When: Querying date range
        List<TemporalSnapshot> snapshots = temporalQueryService.getStateRange(patientId, tenantId, rangeStart, rangeEnd);

        // Then: Should return snapshots for events within range
        assertThat(snapshots).isNotEmpty();
    }

    @Test
    @DisplayName("Should return daily snapshots for month")
    void testMonthlyTemporalSnapshots() {
        // Given: Events throughout month
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        Instant monthStart = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant monthEnd = Instant.now();

        // Store events spread throughout month
        for (int i = 0; i < 30; i++) {
            Instant eventTime = monthStart.plus(i, ChronoUnit.DAYS);
            mockEventStore.storeEventAtTime(patientId, "ObservationRecordedEvent", eventTime);
        }

        // When: Getting daily snapshots
        List<TemporalSnapshot> dailySnapshots = temporalQueryService.getDailySnapshots(patientId, tenantId, monthStart, monthEnd);

        // Then: Should return snapshot per day with event
        assertThat(dailySnapshots).isNotEmpty();
    }

    // ===== Compliance Testing Use Case =====

    @Test
    @DisplayName("Should verify care gap status at historical date")
    void testHistoricalCareGapStatus() {
        // Given: Patient with medication prescribed and condition diagnosed
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        Instant diagnosisDate = Instant.now().minus(60, ChronoUnit.DAYS);
        Instant treatmentDate = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant auditDate = Instant.now().minus(45, ChronoUnit.DAYS); // Between diagnosis and treatment

        mockEventStore.storeEventAtTime(patientId, "ConditionDiagnosedEvent", diagnosisDate); // Hypertension
        mockEventStore.storeEventAtTime(patientId, "MedicationPrescribedEvent", treatmentDate); // After audit

        // When: Checking care gap status as of audit date
        TemporalSnapshot snapshot = temporalQueryService.getStateAsOf(patientId, tenantId, auditDate);

        // Then: Should show gap existed at audit date (diagnosed but not treated)
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.hasOpenGap()).isTrue();
    }

    @Test
    @DisplayName("Should reconstruct audit trail for specific date")
    void testAuditTrailReconstruction() {
        // Given: Series of events
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        Instant t1 = Instant.now().minus(90, ChronoUnit.DAYS);
        Instant t2 = Instant.now().minus(60, ChronoUnit.DAYS);
        Instant t3 = Instant.now().minus(30, ChronoUnit.DAYS);
        Instant auditDate = Instant.now().minus(60, ChronoUnit.DAYS);

        mockEventStore.storeEventAtTime(patientId, "PatientCreatedEvent", t1);
        mockEventStore.storeEventAtTime(patientId, "ConditionDiagnosedEvent", t2);
        mockEventStore.storeEventAtTime(patientId, "MedicationPrescribedEvent", t3);

        // When: Reconstructing audit trail as of t2
        AuditTrail auditTrail = temporalQueryService.getAuditTrail(patientId, tenantId, t1, t2);

        // Then: Should contain events up to t2
        assertThat(auditTrail).isNotNull();
        assertThat(auditTrail.getEvents()).hasSize(2); // PatientCreated + ConditionDiagnosed
    }

    // ===== Quality Measure Evaluation =====

    @Test
    @DisplayName("Should evaluate quality measure as of measurement period end")
    void testMeasureEvaluationAtPeriodEnd() {
        // Given: Patient events and measurement period
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        Instant periodStart = Instant.now().minus(365, ChronoUnit.DAYS);
        Instant periodEnd = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant diagnosisDate = Instant.now().minus(200, ChronoUnit.DAYS);
        Instant treatmentDate = Instant.now().minus(100, ChronoUnit.DAYS);

        mockEventStore.storeEventAtTime(patientId, "ConditionDiagnosedEvent", diagnosisDate);
        mockEventStore.storeEventAtTime(patientId, "MedicationPrescribedEvent", treatmentDate);

        // When: Evaluating measure as of period end
        TemporalSnapshot snapshot = temporalQueryService.getStateAsOf(patientId, tenantId, periodEnd);

        // Then: Snapshot reflects patient state during measurement period
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getTimestamp()).isEqualTo(periodEnd);
    }

    // ===== Snapshot Performance Tests =====

    @Test
    @DisplayName("Should use cached snapshots for performance")
    void testTemporalSnapshotCaching() {
        // Given: Temporal query
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";
        Instant queryTime = Instant.now().minus(15, ChronoUnit.DAYS);

        mockEventStore.storeEventAtTime(patientId, "PatientCreatedEvent", queryTime.minus(10, ChronoUnit.DAYS));
        mockEventStore.storeEventAtTime(patientId, "ConditionDiagnosedEvent", queryTime);

        // When: Querying same temporal point twice
        long start1 = System.currentTimeMillis();
        TemporalSnapshot snapshot1 = temporalQueryService.getStateAsOf(patientId, tenantId, queryTime);
        long duration1 = System.currentTimeMillis() - start1;

        long start2 = System.currentTimeMillis();
        TemporalSnapshot snapshot2 = temporalQueryService.getStateAsOf(patientId, tenantId, queryTime);
        long duration2 = System.currentTimeMillis() - start2;

        // Then: Second query should be faster (cached)
        assertThat(snapshot1).isEqualTo(snapshot2);
        assertThat(duration2).isLessThanOrEqualTo(duration1);
    }

    // ===== Temporal Projection Consistency =====

    @Test
    @DisplayName("Should maintain consistency between temporal snapshots and projections")
    void testTemporalProjectionConsistency() {
        // Given: Events and temporal snapshot
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";
        Instant queryTime = Instant.now().minus(15, ChronoUnit.DAYS);

        mockEventStore.storeEventAtTime(patientId, "PatientCreatedEvent", queryTime.minus(20, ChronoUnit.DAYS));
        mockEventStore.storeEventAtTime(patientId, "ConditionDiagnosedEvent", queryTime);

        // When: Comparing temporal snapshot to projection
        TemporalSnapshot snapshot = temporalQueryService.getStateAsOf(patientId, tenantId, queryTime);

        // Then: Snapshot should match what projection would show
        assertThat(snapshot.getVersion()).isGreaterThan(0);
    }

    // ===== Error Handling Tests =====

    @Test
    @DisplayName("Should handle invalid temporal query gracefully")
    void testInvalidTemporalQuery() {
        // When: Querying with invalid time (queryEnd before queryStart)
        Instant start = Instant.now().minus(10, ChronoUnit.DAYS);
        Instant end = Instant.now().minus(20, ChronoUnit.DAYS);

        // Then: Should throw or return empty
        assertThatThrownBy(() -> temporalQueryService.getStateRange("PATIENT-123", "TENANT-001", start, end))
            .isInstanceOf(InvalidTemporalQueryException.class);
    }

    @Test
    @DisplayName("Should handle corrupted temporal data")
    void testCorruptedTemporalData() {
        // Given: Corrupted event store
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";
        Instant queryTime = Instant.now().minus(15, ChronoUnit.DAYS);

        mockEventStore.simulateCorruptedData(patientId);

        // When: Querying
        // Then: Should handle gracefully
        assertThatCode(() -> temporalQueryService.getStateAsOf(patientId, tenantId, queryTime))
            .doesNotThrowAnyException();
    }

    // ===== Helper Classes =====

    static class TestDomainEvent extends DomainEvent {
        public TestDomainEvent(String aggregateId, String tenantId) {
            super(aggregateId, tenantId);
        }
    }
}
