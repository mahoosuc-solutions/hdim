package com.healthdata.patientevent.eventhandler;

import com.healthdata.patientevent.event.*;
import com.healthdata.patientevent.projection.PatientActiveProjection;

/**
 * PatientEventHandler - Handles patient lifecycle events
 *
 * Processes events and maintains projections for fast queries.
 * Idempotent: Safe to call multiple times with same event.
 */
public class PatientEventHandler {
    private final PatientProjectionStore projectionStore;
    private final EventStore eventStore;

    public PatientEventHandler(PatientProjectionStore projectionStore, EventStore eventStore) {
        this.projectionStore = projectionStore;
        this.eventStore = eventStore;
    }

    public void handle(PatientCreatedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getPatientId() == null || event.getTenantId() == null) {
            throw new IllegalArgumentException("Patient ID and Tenant ID are required");
        }

        // Create projection from event
        PatientActiveProjection projection = new PatientActiveProjection(
            event.getPatientId(),
            event.getTenantId(),
            event.getFirstName(),
            event.getLastName(),
            "ACTIVE"
        );
        projection.setDateOfBirth(event.getDateOfBirth());

        // Save projection
        projectionStore.saveProjection(projection);

        // Store event (append-only)
        eventStore.storeEvent(event);
    }

    public void handle(PatientEnrollmentChangedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getPatientId() == null || event.getTenantId() == null) {
            throw new IllegalArgumentException("Patient ID and Tenant ID are required");
        }

        // Get existing projection
        PatientActiveProjection projection = projectionStore.getPatientProjection(event.getPatientId(), event.getTenantId());
        if (projection != null) {
            // Update enrollment status
            projection.setEnrollmentStatus(event.getNewStatus());
            projection.setEnrollmentReason(event.getReason());
            projection.incrementVersion();
            projectionStore.saveProjection(projection);
        }

        // Store event
        eventStore.storeEvent(event);
    }

    public void handle(PatientDemographicsUpdatedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getPatientId() == null || event.getTenantId() == null) {
            throw new IllegalArgumentException("Patient ID and Tenant ID are required");
        }

        // Get existing projection
        PatientActiveProjection projection = projectionStore.getPatientProjection(event.getPatientId(), event.getTenantId());
        if (projection != null) {
            // Update demographics
            projection.setFirstName(event.getFirstName());
            projection.setLastName(event.getLastName());
            projection.setDateOfBirth(event.getDateOfBirth());
            projection.incrementVersion();
            projectionStore.saveProjection(projection);
        }

        // Store event
        eventStore.storeEvent(event);
    }

    public void handle(PatientDeactivatedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getPatientId() == null || event.getTenantId() == null) {
            throw new IllegalArgumentException("Patient ID and Tenant ID are required");
        }

        // Get existing projection
        PatientActiveProjection projection = projectionStore.getPatientProjection(event.getPatientId(), event.getTenantId());
        if (projection != null) {
            // Deactivate patient
            projection.setStatus("INACTIVE");
            projection.incrementVersion();
            projectionStore.saveProjection(projection);
        }

        // Store event
        eventStore.storeEvent(event);
    }

    public void handle(PatientActivatedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getPatientId() == null || event.getTenantId() == null) {
            throw new IllegalArgumentException("Patient ID and Tenant ID are required");
        }

        // Get existing projection
        PatientActiveProjection projection = projectionStore.getPatientProjection(event.getPatientId(), event.getTenantId());
        if (projection != null) {
            // Activate patient
            projection.setStatus("ACTIVE");
            projection.incrementVersion();
            projectionStore.saveProjection(projection);
        }

        // Store event
        eventStore.storeEvent(event);
    }

    // Support interface for tests
    public interface PatientProjectionStore {
        void saveProjection(PatientActiveProjection projection);
        PatientActiveProjection getPatientProjection(String patientId, String tenantId);
    }

    public interface EventStore {
        void storeEvent(Object event);
    }
}
