package com.healthdata.eventreplay;

import com.healthdata.eventreplay.projection.ProjectionState;
import java.util.*;

/**
 * MockProjectionStore - Test double for projection store
 */
public class MockProjectionStore {
    private final Map<String, ProjectionState> projections = new HashMap<>();

    public void saveProjection(ProjectionState projection) {
        String key = projection.getAggregateId() + ":" + projection.getTenantId();
        projections.put(key, projection);
    }

    public ProjectionState getProjection(String aggregateId, String tenantId) {
        String key = aggregateId + ":" + tenantId;
        return projections.get(key);
    }

    public void clear() {
        projections.clear();
    }
}
