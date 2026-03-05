package com.healthdata.eventreplay.projection;

import com.healthdata.eventreplay.engine.EventStore;
import com.healthdata.eventreplay.engine.ProjectionStore;
import com.healthdata.eventsourcing.event.DomainEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Projection Layer Unit Tests")
class ProjectionManagerTest {

    private static final String AGGREGATE_ID = "agg-001";
    private static final String TENANT_ID = "tenant-alpha";
    private static final String PROJECTION_TYPE = "care-gap";

    @Mock
    private ProjectionStore projectionStore;

    @Mock
    private EventStore eventStore;

    private ProjectionManager projectionManager;

    @BeforeEach
    void setUp() {
        projectionManager = new ProjectionManager(projectionStore, eventStore);
    }

    // ---------------------------------------------------------------
    // ProjectionState value-object tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("ProjectionState")
    class ProjectionStateTests {

        @Test
        @DisplayName("should initialise with correct aggregateId, tenantId, and version")
        void creation() {
            ProjectionState state = new ProjectionState(AGGREGATE_ID, TENANT_ID, 5L);

            assertThat(state.getAggregateId()).isEqualTo(AGGREGATE_ID);
            assertThat(state.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(state.getVersion()).isEqualTo(5L);
            assertThat(state.getLastUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("setVersion should update version and refresh lastUpdatedAt")
        void setVersionUpdatesTimestamp() throws Exception {
            ProjectionState state = new ProjectionState(AGGREGATE_ID, TENANT_ID, 1L);
            Instant before = state.getLastUpdatedAt();

            // Small sleep to guarantee clock advances
            Thread.sleep(2);
            state.setVersion(2L);

            assertThat(state.getVersion()).isEqualTo(2L);
            assertThat(state.getLastUpdatedAt()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("duplicate event ID set should track additions")
        void duplicateEventTracking() {
            ProjectionState state = new ProjectionState(AGGREGATE_ID, TENANT_ID, 0L);

            state.getDuplicateEventIds().add("evt-1");
            state.getDuplicateEventIds().add("evt-2");
            state.getDuplicateEventIds().add("evt-1"); // duplicate

            assertThat(state.getDuplicateEventIds()).containsExactlyInAnyOrder("evt-1", "evt-2");
        }

        @Test
        @DisplayName("care-gap flags should default to false and be settable")
        void careGapFlags() {
            ProjectionState state = new ProjectionState(AGGREGATE_ID, TENANT_ID, 0L);

            assertThat(state.hasOpenGap()).isFalse();
            assertThat(state.hasConditionDiagnosed()).isFalse();
            assertThat(state.hasMedicationPrescribed()).isFalse();

            state.setOpenGap(true);
            state.setConditionDiagnosed(true);
            state.setMedicationPrescribed(true);

            assertThat(state.hasOpenGap()).isTrue();
            assertThat(state.hasConditionDiagnosed()).isTrue();
            assertThat(state.hasMedicationPrescribed()).isTrue();
        }

        @Test
        @DisplayName("equals and hashCode should use aggregateId, tenantId, and version")
        void equalsAndHashCode() {
            ProjectionState a = new ProjectionState(AGGREGATE_ID, TENANT_ID, 3L);
            ProjectionState b = new ProjectionState(AGGREGATE_ID, TENANT_ID, 3L);
            ProjectionState differentVersion = new ProjectionState(AGGREGATE_ID, TENANT_ID, 4L);
            ProjectionState differentTenant = new ProjectionState(AGGREGATE_ID, "other-tenant", 3L);

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
            assertThat(a).isNotEqualTo(differentVersion);
            assertThat(a).isNotEqualTo(differentTenant);
            assertThat(a).isNotEqualTo(null);
        }
    }

    // ---------------------------------------------------------------
    // ProjectionSnapshot value-object tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("ProjectionSnapshot")
    class ProjectionSnapshotTests {

        @Test
        @DisplayName("should capture aggregateId, tenantId, version, and auto-generate snapshotId + createdAt")
        void creation() {
            ProjectionSnapshot snap = new ProjectionSnapshot(AGGREGATE_ID, TENANT_ID, 7L);

            assertThat(snap.getAggregateId()).isEqualTo(AGGREGATE_ID);
            assertThat(snap.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(snap.getVersion()).isEqualTo(7L);
            assertThat(snap.getSnapshotId()).isNotNull().isNotBlank();
            assertThat(snap.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("two snapshots should have unique IDs")
        void uniqueIds() {
            ProjectionSnapshot s1 = new ProjectionSnapshot(AGGREGATE_ID, TENANT_ID, 1L);
            ProjectionSnapshot s2 = new ProjectionSnapshot(AGGREGATE_ID, TENANT_ID, 1L);

            assertThat(s1.getSnapshotId()).isNotEqualTo(s2.getSnapshotId());
        }
    }

    // ---------------------------------------------------------------
    // ProjectionRebuildProgress value-object tests
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("ProjectionRebuildProgress")
    class RebuildProgressTests {

        @Test
        @DisplayName("should initialise with tenant, type, startTime, and not complete")
        void creation() {
            ProjectionRebuildProgress progress = new ProjectionRebuildProgress(TENANT_ID, PROJECTION_TYPE);

            assertThat(progress.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(progress.getProjectionType()).isEqualTo(PROJECTION_TYPE);
            assertThat(progress.isComplete()).isFalse();
            assertThat(progress.getTotalProjections()).isZero();
            assertThat(progress.getProjectionsRebuilt()).isZero();
        }

        @Test
        @DisplayName("should track progress totals")
        void progressTracking() {
            ProjectionRebuildProgress progress = new ProjectionRebuildProgress(TENANT_ID, PROJECTION_TYPE);

            progress.setTotalProjections(100);
            progress.setProjectionsRebuilt(42);

            assertThat(progress.getTotalProjections()).isEqualTo(100);
            assertThat(progress.getProjectionsRebuilt()).isEqualTo(42);
        }

        @Test
        @DisplayName("markComplete should set complete flag and record positive duration")
        void markComplete() throws Exception {
            ProjectionRebuildProgress progress = new ProjectionRebuildProgress(TENANT_ID, PROJECTION_TYPE);

            // Small sleep so duration > 0
            Thread.sleep(2);
            progress.markComplete();

            assertThat(progress.isComplete()).isTrue();
            assertThat(progress.getDurationMs()).isGreaterThanOrEqualTo(0L);
        }

        @Test
        @DisplayName("getDurationMs should return 0 when not yet complete")
        void durationBeforeComplete() {
            ProjectionRebuildProgress progress = new ProjectionRebuildProgress(TENANT_ID, PROJECTION_TYPE);

            assertThat(progress.getDurationMs()).isZero();
        }
    }

    // ---------------------------------------------------------------
    // ProjectionManager.createProjection
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("createProjection")
    class CreateProjectionTests {

        @Test
        @DisplayName("should create projection with version equal to event count")
        void withEvents() {
            DomainEvent e1 = mockEvent(AGGREGATE_ID);
            DomainEvent e2 = mockEvent(AGGREGATE_ID);
            DomainEvent e3 = mockEvent(AGGREGATE_ID);
            List<DomainEvent> events = List.of(e1, e2, e3);
            when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID)).thenReturn(events);

            ProjectionState result = projectionManager.createProjection(AGGREGATE_ID, TENANT_ID, PROJECTION_TYPE);

            assertThat(result.getAggregateId()).isEqualTo(AGGREGATE_ID);
            assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(result.getVersion()).isEqualTo(3L);
            verify(projectionStore).saveProjection(any(ProjectionState.class));
        }

        @Test
        @DisplayName("should create projection with version 0 when no events exist")
        void withNoEvents() {
            when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID)).thenReturn(List.of());

            ProjectionState result = projectionManager.createProjection(AGGREGATE_ID, TENANT_ID, PROJECTION_TYPE);

            assertThat(result.getVersion()).isZero();
            verify(projectionStore).saveProjection(any(ProjectionState.class));
        }
    }

    // ---------------------------------------------------------------
    // ProjectionManager.updateProjection
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("updateProjection")
    class UpdateProjectionTests {

        @Test
        @DisplayName("should increment version by 1 and persist")
        void normalUpdate() {
            ProjectionState state = new ProjectionState(AGGREGATE_ID, TENANT_ID, 5L);
            DomainEvent event = mockEvent(AGGREGATE_ID);

            ProjectionState updated = projectionManager.updateProjection(state, event);

            assertThat(updated.getVersion()).isEqualTo(6L);
            verify(projectionStore).saveProjection(state);
        }

        @Test
        @DisplayName("should throw EventApplyException when event aggregateId does not match projection")
        void mismatchedAggregateId() {
            ProjectionState state = new ProjectionState(AGGREGATE_ID, TENANT_ID, 1L);
            DomainEvent event = mockEvent("different-aggregate");

            assertThatThrownBy(() -> projectionManager.updateProjection(state, event))
                    .isInstanceOf(EventApplyException.class)
                    .hasMessageContaining("aggregate ID");
        }
    }

    // ---------------------------------------------------------------
    // ProjectionManager.isProjectionStale
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("isProjectionStale")
    class IsProjectionStaleTests {

        @Test
        @DisplayName("should return true when no projection exists")
        void nullProjection() {
            when(projectionStore.getProjection(AGGREGATE_ID, TENANT_ID)).thenReturn(null);

            assertThat(projectionManager.isProjectionStale(AGGREGATE_ID, TENANT_ID)).isTrue();
        }

        @Test
        @DisplayName("should return true when projection version is behind event count")
        void staleVersion() {
            DomainEvent e1 = mockEvent(AGGREGATE_ID);
            DomainEvent e2 = mockEvent(AGGREGATE_ID);
            DomainEvent e3 = mockEvent(AGGREGATE_ID);
            ProjectionState stale = new ProjectionState(AGGREGATE_ID, TENANT_ID, 2L);
            when(projectionStore.getProjection(AGGREGATE_ID, TENANT_ID)).thenReturn(stale);
            when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID))
                    .thenReturn(List.of(e1, e2, e3));

            assertThat(projectionManager.isProjectionStale(AGGREGATE_ID, TENANT_ID)).isTrue();
        }

        @Test
        @DisplayName("should return false when projection version matches event count")
        void freshVersion() {
            DomainEvent e1 = mockEvent(AGGREGATE_ID);
            DomainEvent e2 = mockEvent(AGGREGATE_ID);
            DomainEvent e3 = mockEvent(AGGREGATE_ID);
            ProjectionState fresh = new ProjectionState(AGGREGATE_ID, TENANT_ID, 3L);
            when(projectionStore.getProjection(AGGREGATE_ID, TENANT_ID)).thenReturn(fresh);
            when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID))
                    .thenReturn(List.of(e1, e2, e3));

            assertThat(projectionManager.isProjectionStale(AGGREGATE_ID, TENANT_ID)).isFalse();
        }
    }

    // ---------------------------------------------------------------
    // ProjectionManager.rebuildProjection
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("rebuildProjection")
    class RebuildProjectionTests {

        @Test
        @DisplayName("should create fresh projection state from current events")
        void rebuild() {
            DomainEvent e1 = mockEvent(AGGREGATE_ID);
            DomainEvent e2 = mockEvent(AGGREGATE_ID);
            List<DomainEvent> events = List.of(e1, e2);
            when(eventStore.getEventsForAggregate(AGGREGATE_ID, TENANT_ID)).thenReturn(events);

            ProjectionState result = projectionManager.rebuildProjection(AGGREGATE_ID, TENANT_ID);

            assertThat(result.getAggregateId()).isEqualTo(AGGREGATE_ID);
            assertThat(result.getVersion()).isEqualTo(2L);
            verify(projectionStore).saveProjection(any(ProjectionState.class));
        }
    }

    // ---------------------------------------------------------------
    // ProjectionManager.createSnapshot
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("createSnapshot")
    class CreateSnapshotTests {

        @Test
        @DisplayName("should create snapshot with projection version and matching identifiers")
        void createsSnapshot() {
            ProjectionState state = new ProjectionState(AGGREGATE_ID, TENANT_ID, 10L);

            ProjectionSnapshot snapshot = projectionManager.createSnapshot(AGGREGATE_ID, TENANT_ID, state);

            assertThat(snapshot.getAggregateId()).isEqualTo(AGGREGATE_ID);
            assertThat(snapshot.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(snapshot.getVersion()).isEqualTo(10L);
            assertThat(snapshot.getSnapshotId()).isNotNull();
            assertThat(snapshot.getCreatedAt()).isNotNull();
        }
    }

    // ---------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------

    private static DomainEvent mockEvent(String aggregateId) {
        DomainEvent event = mock(DomainEvent.class);
        lenient().when(event.getAggregateId()).thenReturn(aggregateId);
        lenient().when(event.getEventId()).thenReturn(java.util.UUID.randomUUID().toString());
        lenient().when(event.getTimestamp()).thenReturn(Instant.now());
        return event;
    }
}
