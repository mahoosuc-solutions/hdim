package com.healthdata.caregap.eventhandler;

import com.healthdata.caregap.event.*;
import com.healthdata.caregap.projection.*;

/**
 * CareGapEventHandler - Handles care gap lifecycle events
 *
 * Processes events and maintains projections for care gap queries.
 * Idempotent: Safe to call multiple times with same event.
 */
public class CareGapEventHandler {
    private final CareGapProjectionStore projectionStore;
    private final EventStore eventStore;

    public CareGapEventHandler(CareGapProjectionStore projectionStore, EventStore eventStore) {
        this.projectionStore = projectionStore;
        this.eventStore = eventStore;
    }

    public void handle(CareGapDetectedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getPatientId() == null || event.getTenantId() == null) {
            throw new IllegalArgumentException("Patient ID and Tenant ID are required");
        }
        if (event.getGapCode() == null) {
            throw new IllegalArgumentException("Gap Code is required");
        }

        // Create projection from event
        CareGapProjection projection = new CareGapProjection(
            event.getPatientId(),
            event.getTenantId(),
            event.getGapCode(),
            event.getGapDescription(),
            event.getSeverity()
        );

        // Save projection
        projectionStore.saveCareGapProjection(projection);

        // Update population health
        updatePopulationHealth(event.getTenantId(), event.getSeverity(), 1, 0);

        // Store event (append-only)
        eventStore.storeEvent(event);
    }

    public void handle(PatientQualifiedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getPatientId() == null || event.getTenantId() == null) {
            throw new IllegalArgumentException("Patient ID and Tenant ID are required");
        }

        // Get existing projection
        CareGapProjection projection = projectionStore.getCareGapProjection(
            event.getPatientId(), event.getTenantId(), event.getGapCode()
        );

        if (projection != null) {
            // Update qualification status
            projection.setQualified(event.isQualified());
            projection.incrementVersion();
            projectionStore.saveCareGapProjection(projection);
        }

        // Store event
        eventStore.storeEvent(event);
    }

    public void handle(InterventionRecommendedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getPatientId() == null || event.getTenantId() == null) {
            throw new IllegalArgumentException("Patient ID and Tenant ID are required");
        }

        // Get existing projection
        CareGapProjection projection = projectionStore.getCareGapProjection(
            event.getPatientId(), event.getTenantId(), event.getGapCode()
        );

        if (projection != null) {
            // Update recommended intervention
            projection.setRecommendedIntervention(event.getIntervention());
            projection.incrementVersion();
            projectionStore.saveCareGapProjection(projection);
        }

        // Store event
        eventStore.storeEvent(event);
    }

    public void handle(GapClosedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getPatientId() == null || event.getTenantId() == null) {
            throw new IllegalArgumentException("Patient ID and Tenant ID are required");
        }

        // Get existing projection
        CareGapProjection projection = projectionStore.getCareGapProjection(
            event.getPatientId(), event.getTenantId(), event.getGapCode()
        );

        if (projection != null) {
            // Mark gap as closed
            projection.setStatus(event.getClosureStatus());
            projection.setClosureDate(java.time.LocalDate.now());
            projection.incrementVersion();
            projectionStore.saveCareGapProjection(projection);

            // Update population health
            updatePopulationHealth(event.getTenantId(), projection.getSeverity(), -1, 1);
        }

        // Store event
        eventStore.storeEvent(event);
    }

    private void updatePopulationHealth(String tenantId, String severity, int openDelta, int closedDelta) {
        PopulationHealthProjection healthProjection = projectionStore.getPopulationHealth(tenantId);
        if (healthProjection == null) {
            healthProjection = new PopulationHealthProjection(tenantId);
        }

        // Update gap counts
        healthProjection.setTotalGapsOpen(healthProjection.getTotalGapsOpen() + openDelta);
        healthProjection.setGapsClosed(healthProjection.getGapsClosed() + closedDelta);

        // Update severity-specific counts
        if (openDelta > 0) {
            switch (severity) {
                case "CRITICAL" -> healthProjection.setCriticalGaps(healthProjection.getCriticalGaps() + 1);
                case "HIGH" -> healthProjection.setHighGaps(healthProjection.getHighGaps() + 1);
                case "MEDIUM" -> healthProjection.setMediumGaps(healthProjection.getMediumGaps() + 1);
                case "LOW" -> healthProjection.setLowGaps(healthProjection.getLowGaps() + 1);
            }
        }

        // Recalculate closure rate
        healthProjection.calculateClosureRate();
        projectionStore.savePopulationHealth(healthProjection);
    }

    // Support interfaces for tests
    public interface CareGapProjectionStore {
        void saveCareGapProjection(CareGapProjection projection);
        CareGapProjection getCareGapProjection(String patientId, String tenantId, String gapCode);
        void savePopulationHealth(PopulationHealthProjection projection);
        PopulationHealthProjection getPopulationHealth(String tenantId);
    }

    public interface EventStore {
        void storeEvent(Object event);
    }
}
