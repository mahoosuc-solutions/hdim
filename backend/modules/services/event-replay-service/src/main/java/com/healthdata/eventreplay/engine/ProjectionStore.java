package com.healthdata.eventreplay.engine;

import com.healthdata.eventreplay.projection.ProjectionState;

/**
 * ProjectionStore - Repository interface for denormalized read models
 *
 * Abstraction for accessing read-optimized projections built from events.
 * Implementations may use cache, database, or other storage.
 */
public interface ProjectionStore {

    /**
     * Save or update a projection
     */
    void saveProjection(ProjectionState projection);

    /**
     * Get a projection for an aggregate
     */
    ProjectionState getProjection(String aggregateId, String tenantId);

    /**
     * Check if projection is stale
     */
    boolean isProjectionStale(String aggregateId, String tenantId);

    /**
     * Delete a projection
     */
    void deleteProjection(String aggregateId, String tenantId);
}
