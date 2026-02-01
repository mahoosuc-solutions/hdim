package com.healthdata.eventreplay.projection;

import com.healthdata.eventsourcing.event.DomainEvent;
import com.healthdata.eventsourcing.event.PatientCreatedEvent;
import com.healthdata.eventsourcing.event.ConditionDiagnosedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for ProjectionManager
 *
 * Tests the projection lifecycle:
 * - Creating new projections from events
 * - Updating existing projections
 * - Version tracking of projections
 * - Idempotent projection updates
 * - Multi-tenant projection isolation
 * - Rebuilding projections from scratch
 * - Consistency guarantees
 *
 * Projections are denormalized read models that aggregate data from events.
 * The ProjectionManager ensures they stay consistent with the event store
 * while being optimized for query performance.
 */
@DisplayName("ProjectionManager Tests")
class ProjectionManagerTest {

    private ProjectionManager projectionManager;
    private MockProjectionStore mockProjectionStore;
    private MockEventStore mockEventStore;

    @BeforeEach
    void setup() {
        mockProjectionStore = new MockProjectionStore();
        mockEventStore = new MockEventStore();
        projectionManager = new ProjectionManager(mockProjectionStore, mockEventStore);
    }

    // ===== Basic Projection Tests =====

    @Test
    @DisplayName("Should create new projection from events")
    void testCreateProjectionFromEvents() {
        // Given: Events for a patient
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        List<DomainEvent> events = Arrays.asList(
            new PatientCreatedEvent(patientId, tenantId, "John", "Doe", "1990-01-01"),
            new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension")
        );
        mockEventStore.storeEvents(patientId, events);

        // When: Creating projection
        ProjectionState projection = projectionManager.createProjection(patientId, tenantId, "PatientProjection");

        // Then: Projection should contain aggregated data
        assertThat(projection).isNotNull();
        assertThat(projection.getAggregateId()).isEqualTo(patientId);
        assertThat(projection.getTenantId()).isEqualTo(tenantId);
        assertThat(projection.getVersion()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should update projection with new events")
    void testUpdateProjection() {
        // Given: Existing projection
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        ProjectionState projection = new ProjectionState(patientId, tenantId, 1L);
        mockProjectionStore.saveProjection(projection);

        // When: Updating with new event
        DomainEvent newEvent = new ConditionDiagnosedEvent(patientId, tenantId, "DM", "Diabetes");
        ProjectionState updated = projectionManager.updateProjection(projection, newEvent);

        // Then: Version should increment
        assertThat(updated.getVersion()).isGreaterThan(projection.getVersion());
    }

    @Test
    @DisplayName("Should retrieve existing projection by ID")
    void testGetProjection() {
        // Given: Saved projection
        String patientId = "PATIENT-456";
        String tenantId = "TENANT-002";

        ProjectionState projection = new ProjectionState(patientId, tenantId, 1L);
        mockProjectionStore.saveProjection(projection);

        // When: Retrieving projection
        ProjectionState retrieved = projectionManager.getProjection(patientId, tenantId);

        // Then: Should retrieve saved projection
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getAggregateId()).isEqualTo(patientId);
    }

    @Test
    @DisplayName("Should handle non-existent projection")
    void testGetNonExistentProjection() {
        // When: Retrieving non-existent projection
        ProjectionState retrieved = projectionManager.getProjection("UNKNOWN-ID", "TENANT-001");

        // Then: Should return null or empty
        assertThat(retrieved).isNull();
    }

    // ===== Idempotency Tests =====

    @Test
    @DisplayName("Should be idempotent when applying same event twice")
    void testIdempotentEventApplication() {
        // Given: Event and projection
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        ProjectionState projection = new ProjectionState(patientId, tenantId, 1L);
        DomainEvent event = new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension");
        event.setCorrelationId("UNIQUE-ID-1");

        mockProjectionStore.saveProjection(projection);

        // When: Applying same event twice
        ProjectionState updated1 = projectionManager.updateProjection(projection, event);
        ProjectionState updated2 = projectionManager.updateProjection(updated1, event);

        // Then: Second update should not double-apply changes
        assertThat(updated1).isEqualTo(updated2);
    }

    @Test
    @DisplayName("Should detect duplicate events by correlation ID")
    void testDuplicateEventDetection() {
        // Given: Events with same correlation ID
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";
        String correlationId = "CORRELATION-001";

        ProjectionState projection = new ProjectionState(patientId, tenantId, 1L);
        DomainEvent event1 = new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension");
        event1.setCorrelationId(correlationId);

        DomainEvent event2 = new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension");
        event2.setCorrelationId(correlationId);

        mockProjectionStore.saveProjection(projection);

        // When: Applying both events
        ProjectionState updated1 = projectionManager.updateProjection(projection, event1);
        ProjectionState updated2 = projectionManager.updateProjection(updated1, event2);

        // Then: Should recognize as duplicate and not apply twice
        // This prevents double-charging, double-counting, etc.
        assertThat(updated2.getDuplicateEventIds()).contains(correlationId);
    }

    // ===== Version Tracking Tests =====

    @Test
    @DisplayName("Should track projection version")
    void testProjectionVersionTracking() {
        // Given: Projection
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        ProjectionState projection = new ProjectionState(patientId, tenantId, 1L);

        // When: Recording events
        DomainEvent event1 = new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension");
        DomainEvent event2 = new ConditionDiagnosedEvent(patientId, tenantId, "DM", "Diabetes");

        ProjectionState v1 = projectionManager.updateProjection(projection, event1);
        ProjectionState v2 = projectionManager.updateProjection(v1, event2);

        // Then: Version should increment with each event
        assertThat(v1.getVersion()).isEqualTo(2L);
        assertThat(v2.getVersion()).isEqualTo(3L);
    }

    @Test
    @DisplayName("Should detect version conflicts")
    void testVersionConflictDetection() {
        // Given: Two projections with conflicting versions
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        ProjectionState original = new ProjectionState(patientId, tenantId, 1L);
        ProjectionState modified = new ProjectionState(patientId, tenantId, 1L);

        DomainEvent event = new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension");

        mockProjectionStore.saveProjection(original);
        mockProjectionStore.saveProjection(modified);

        // When: Both attempt to update
        ProjectionState updated1 = projectionManager.updateProjection(original, event);

        // Then: Second update should detect conflict
        assertThatThrownBy(() -> projectionManager.updateProjection(modified, event, updated1.getVersion()))
            .isInstanceOf(OptimisticLockException.class);
    }

    // ===== Multi-Tenant Tests =====

    @Test
    @DisplayName("Should isolate projections by tenant")
    void testMultiTenantProjectionIsolation() {
        // Given: Same patient ID in different tenants
        String patientId = "PATIENT-123";
        String tenant1 = "TENANT-001";
        String tenant2 = "TENANT-002";

        ProjectionState tenant1Projection = new ProjectionState(patientId, tenant1, 1L);
        ProjectionState tenant2Projection = new ProjectionState(patientId, tenant2, 1L);

        mockProjectionStore.saveProjection(tenant1Projection);
        mockProjectionStore.saveProjection(tenant2Projection);

        // When: Retrieving projections
        ProjectionState retrieved1 = projectionManager.getProjection(patientId, tenant1);
        ProjectionState retrieved2 = projectionManager.getProjection(patientId, tenant2);

        // Then: Should return different projections
        assertThat(retrieved1.getTenantId()).isEqualTo(tenant1);
        assertThat(retrieved2.getTenantId()).isEqualTo(tenant2);
    }

    @Test
    @DisplayName("Should enforce tenant access control")
    void testTenantAccessControl() {
        // Given: Projection for TENANT-001
        String patientId = "PATIENT-123";
        String authorizedTenant = "TENANT-001";
        String unauthorizedTenant = "TENANT-999";

        ProjectionState projection = new ProjectionState(patientId, authorizedTenant, 1L);
        mockProjectionStore.saveProjection(projection);

        // When: Attempting to access from unauthorized tenant
        ProjectionState retrieved = projectionManager.getProjection(patientId, unauthorizedTenant);

        // Then: Should return null (access denied)
        assertThat(retrieved).isNull();
    }

    // ===== Bulk Operation Tests =====

    @Test
    @DisplayName("Should rebuild projections from events")
    void testRebuildProjectionsFromEvents() {
        // Given: Events for multiple patients
        String patient1 = "PATIENT-1";
        String patient2 = "PATIENT-2";
        String tenantId = "TENANT-001";

        List<DomainEvent> events = Arrays.asList(
            new PatientCreatedEvent(patient1, tenantId, "John", "Doe", "1990-01-01"),
            new PatientCreatedEvent(patient2, tenantId, "Jane", "Smith", "1992-05-15")
        );
        mockEventStore.storeEvents(patient1, Collections.singletonList(events.get(0)));
        mockEventStore.storeEvents(patient2, Collections.singletonList(events.get(1)));

        // When: Rebuilding all projections
        List<ProjectionState> rebuilt = projectionManager.rebuildAllProjections(tenantId, "PatientProjection");

        // Then: Should rebuild projections for all patients
        assertThat(rebuilt).hasSize(2);
    }

    @Test
    @DisplayName("Should rebuild single projection from scratch")
    void testRebuildSingleProjection() {
        // Given: Patient with multiple events
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        List<DomainEvent> events = Arrays.asList(
            new PatientCreatedEvent(patientId, tenantId, "John", "Doe", "1990-01-01"),
            new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension"),
            new ConditionDiagnosedEvent(patientId, tenantId, "DM", "Diabetes")
        );
        mockEventStore.storeEvents(patientId, events);

        // When: Rebuilding projection from scratch
        ProjectionState rebuilt = projectionManager.rebuildProjection(patientId, tenantId);

        // Then: Should contain all aggregated data
        assertThat(rebuilt).isNotNull();
        assertThat(rebuilt.getVersion()).isEqualTo(events.size());
    }

    @Test
    @DisplayName("Should track rebuild progress")
    void testRebuildProgressTracking() {
        // Given: Many patients (100)
        String tenantId = "TENANT-001";

        for (int i = 0; i < 100; i++) {
            String patientId = "PATIENT-" + i;
            DomainEvent event = new PatientCreatedEvent(patientId, tenantId, "Patient", String.valueOf(i), "1990-01-01");
            mockEventStore.storeEvents(patientId, Collections.singletonList(event));
        }

        // When: Rebuilding with progress tracking
        ProjectionRebuildProgress progress = new ProjectionRebuildProgress(tenantId, "PatientProjection");
        List<ProjectionState> rebuilt = projectionManager.rebuildAllProjectionsWithProgress(tenantId, "PatientProjection", progress);

        // Then: Progress should track completion
        assertThat(rebuilt).hasSize(100);
        assertThat(progress.getTotalProjections()).isEqualTo(100);
        assertThat(progress.isComplete()).isTrue();
    }

    // ===== Consistency Tests =====

    @Test
    @DisplayName("Should maintain consistency between event store and projections")
    void testEventProjectionConsistency() {
        // Given: Events and corresponding projection
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        DomainEvent event = new PatientCreatedEvent(patientId, tenantId, "John", "Doe", "1990-01-01");
        mockEventStore.storeEvents(patientId, Collections.singletonList(event));

        // When: Creating projection and comparing to events
        ProjectionState projection = projectionManager.createProjection(patientId, tenantId, "PatientProjection");

        // Then: Projection should match event state
        assertThat(projection.getVersion()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should detect stale projections")
    void testStaleProjectionDetection() {
        // Given: Projection at version 5
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        ProjectionState staleProjection = new ProjectionState(patientId, tenantId, 5L);
        mockProjectionStore.saveProjection(staleProjection);

        // When: Events added to event store (version now 10)
        for (int i = 0; i < 5; i++) {
            DomainEvent event = new ConditionDiagnosedEvent(patientId, tenantId, "CODE" + i, "Condition");
            mockEventStore.storeEvents(patientId, Collections.singletonList(event));
        }

        // Then: Should detect staleness
        boolean isStale = projectionManager.isProjectionStale(patientId, tenantId);
        assertThat(isStale).isTrue();
    }

    // ===== Snapshot Tests =====

    @Test
    @DisplayName("Should optimize projection queries using snapshots")
    void testProjectionSnapshot() {
        // Given: Projection at version 100
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        ProjectionState projection = new ProjectionState(patientId, tenantId, 100L);
        mockProjectionStore.saveProjection(projection);

        // When: Saving snapshot
        ProjectionSnapshot snapshot = projectionManager.createSnapshot(patientId, tenantId, projection);

        // Then: Snapshot should capture state
        assertThat(snapshot).isNotNull();
        assertThat(snapshot.getVersion()).isEqualTo(100L);
    }

    // ===== Error Handling Tests =====

    @Test
    @DisplayName("Should handle projection creation errors")
    void testProjectionCreationError() {
        // When: Creating projection with invalid type
        // Then: Should throw appropriate error
        assertThatThrownBy(() -> projectionManager.createProjection("PATIENT-123", "TENANT-001", "InvalidProjection"))
            .isInstanceOf(ProjectionException.class);
    }

    @Test
    @DisplayName("Should handle event application errors")
    void testEventApplicationError() {
        // Given: Projection and incompatible event
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        ProjectionState projection = new ProjectionState(patientId, tenantId, 1L);
        DomainEvent incompatibleEvent = new TestDomainEvent("DIFFERENT-PATIENT", tenantId);

        // When: Attempting to apply incompatible event
        // Then: Should throw error
        assertThatThrownBy(() -> projectionManager.updateProjection(projection, incompatibleEvent))
            .isInstanceOf(EventApplyException.class);
    }

    // ===== Timestamp Tests =====

    @Test
    @DisplayName("Should track projection update time")
    void testProjectionUpdateTimestamp() {
        // Given: Projection
        String patientId = "PATIENT-123";
        String tenantId = "TENANT-001";

        ProjectionState projection = new ProjectionState(patientId, tenantId, 1L);
        Instant beforeUpdate = Instant.now();

        // When: Updating projection
        DomainEvent event = new ConditionDiagnosedEvent(patientId, tenantId, "HTN", "Hypertension");
        ProjectionState updated = projectionManager.updateProjection(projection, event);
        Instant afterUpdate = Instant.now();

        // Then: Update time should be recorded
        assertThat(updated.getLastUpdatedAt())
            .isAfterOrEqualTo(beforeUpdate)
            .isBeforeOrEqualTo(afterUpdate);
    }

    // ===== Helper Methods =====

    static class TestDomainEvent extends DomainEvent {
        public TestDomainEvent(String aggregateId, String tenantId) {
            super(aggregateId, tenantId);
        }
    }
}
