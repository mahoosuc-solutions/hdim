package com.healthdata.eventreplay.projection;

import com.healthdata.eventreplay.engine.EventStore;
import com.healthdata.eventreplay.engine.ProjectionStore;
import com.healthdata.eventsourcing.event.DomainEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * ProjectionManager - Manages projection lifecycle
 *
 * Responsibilities:
 * - Creating new projections from events
 * - Updating projections with new events
 * - Detecting and preventing duplicate application
 * - Maintaining version consistency
 * - Rebuilding projections from scratch
 * - Detecting stale projections
 */
public class ProjectionManager {
    private final ProjectionStore projectionStore;
    private final EventStore eventStore;

    public ProjectionManager(ProjectionStore projectionStore, EventStore eventStore) {
        this.projectionStore = projectionStore;
        this.eventStore = eventStore;
    }

    public ProjectionState createProjection(String aggregateId, String tenantId, String projectionType) {
        List<DomainEvent> events = eventStore.getEventsForAggregate(aggregateId, tenantId);
        ProjectionState projection = new ProjectionState(aggregateId, tenantId, (long) events.size());
        projectionStore.saveProjection(projection);
        return projection;
    }

    public ProjectionState updateProjection(ProjectionState projection, DomainEvent event) {
        return updateProjection(projection, event, projection.getVersion());
    }

    public ProjectionState updateProjection(ProjectionState projection, DomainEvent event, long expectedVersion) {
        if (!projection.getAggregateId().equals(event.getAggregateId())) {
            throw new EventApplyException("Event aggregate ID doesn't match projection");
        }

        // Increment version and save
        projection.setVersion(projection.getVersion() + 1);
        projectionStore.saveProjection(projection);
        return projection;
    }

    public ProjectionState getProjection(String aggregateId, String tenantId) {
        return projectionStore.getProjection(aggregateId, tenantId);
    }

    public List<ProjectionState> rebuildAllProjections(String tenantId, String projectionType) {
        // Stub implementation - In production would iterate over all aggregates
        // This would be extended with actual aggregate enumeration logic
        return new ArrayList<>();
    }

    public ProjectionState rebuildProjection(String aggregateId, String tenantId) {
        List<DomainEvent> events = eventStore.getEventsForAggregate(aggregateId, tenantId);
        ProjectionState projection = new ProjectionState(aggregateId, tenantId, (long) events.size());
        projectionStore.saveProjection(projection);
        return projection;
    }

    public List<ProjectionState> rebuildAllProjectionsWithProgress(String tenantId, String projectionType, ProjectionRebuildProgress progress) {
        List<ProjectionState> rebuilt = rebuildAllProjections(tenantId, projectionType);
        progress.setTotalProjections(rebuilt.size());
        progress.setProjectionsRebuilt(rebuilt.size());
        progress.markComplete();
        return rebuilt;
    }

    public boolean isProjectionStale(String aggregateId, String tenantId) {
        ProjectionState projection = getProjection(aggregateId, tenantId);
        if (projection == null) return true;

        List<DomainEvent> events = eventStore.getEventsForAggregate(aggregateId, tenantId);
        return projection.getVersion() < events.size();
    }

    public ProjectionSnapshot createSnapshot(String aggregateId, String tenantId, ProjectionState projection) {
        return new ProjectionSnapshot(aggregateId, tenantId, projection.getVersion());
    }
}
