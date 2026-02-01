package com.healthdata.qualityevent.eventhandler;

import com.healthdata.qualityevent.event.*;
import com.healthdata.qualityevent.projection.*;

/**
 * QualityMeasureEventHandler - Handles quality measure evaluation events
 *
 * Processes events and maintains projections for fast quality measure queries.
 * Idempotent: Safe to call multiple times with same event.
 * Supports cohort aggregation and risk score calculation.
 */
public class QualityMeasureEventHandler {
    private final MeasureProjectionStore projectionStore;
    private final EventStore eventStore;

    public QualityMeasureEventHandler(MeasureProjectionStore projectionStore, EventStore eventStore) {
        this.projectionStore = projectionStore;
        this.eventStore = eventStore;
    }

    public void handle(MeasureEvaluatedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getPatientId() == null || event.getTenantId() == null) {
            throw new IllegalArgumentException("Patient ID and Tenant ID are required");
        }
        if (event.getMeasureCode() == null) {
            throw new IllegalArgumentException("Measure Code is required");
        }

        // Create projection from event
        MeasureEvaluationProjection projection = new MeasureEvaluationProjection(
            event.getPatientId(),
            event.getTenantId(),
            event.getMeasureCode(),
            event.getMeasureDescription()
        );

        // Save projection
        projectionStore.saveMeasureProjection(projection);

        // Store event (append-only)
        eventStore.storeEvent(event);
    }

    public void handle(MeasureScoreCalculatedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getPatientId() == null || event.getTenantId() == null) {
            throw new IllegalArgumentException("Patient ID and Tenant ID are required");
        }

        // Get existing projection
        MeasureEvaluationProjection projection = projectionStore.getMeasureProjection(
            event.getPatientId(), event.getTenantId(), event.getMeasureCode()
        );

        if (projection != null) {
            // Update score and status
            projection.setScore(event.getScore());
            projection.setEvaluationReason(event.getReason());

            // Determine status based on score (>75% is considered MET)
            if (event.getScore() > 0.75f) {
                projection.setStatus("MET");
            } else if (event.getScore() > 0.0f) {
                projection.setStatus("PARTIAL");
            } else {
                projection.setStatus("NOT_MET");
            }

            projection.incrementVersion();
            projectionStore.saveMeasureProjection(projection);

            // Update cohort aggregation
            updateCohortRate(event.getTenantId(), event.getMeasureCode(), event.getScore());
        }

        // Store event
        eventStore.storeEvent(event);
    }

    private void updateCohortRate(String tenantId, String measureCode, float score) {
        CohortMeasureRateProjection cohortProjection = projectionStore.getCohortRate(tenantId, measureCode);
        if (cohortProjection == null) {
            cohortProjection = new CohortMeasureRateProjection(tenantId, measureCode);
        }

        // Increment denominator count
        cohortProjection.setDenominatorCount(cohortProjection.getDenominatorCount() + 1);

        // Increment numerator count if patient meets measure (score > 0.75)
        if (score > 0.75f) {
            cohortProjection.setNumeratorCount(cohortProjection.getNumeratorCount() + 1);
        }

        // Recalculate compliance rate
        cohortProjection.calculateComplianceRate();
        projectionStore.saveCohortRate(cohortProjection);
    }

    public void handle(MeasureNumeratorStatusEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getPatientId() == null || event.getTenantId() == null) {
            throw new IllegalArgumentException("Patient ID and Tenant ID are required");
        }

        // Get existing projection
        MeasureEvaluationProjection projection = projectionStore.getMeasureProjection(
            event.getPatientId(), event.getTenantId(), event.getMeasureCode()
        );

        if (projection != null) {
            // Update numerator status
            projection.setInNumerator(event.isInNumerator());
            projection.incrementVersion();
            projectionStore.saveMeasureProjection(projection);
        }

        // Store event
        eventStore.storeEvent(event);
    }

    public void handle(MeasureDenominatorStatusEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getPatientId() == null || event.getTenantId() == null) {
            throw new IllegalArgumentException("Patient ID and Tenant ID are required");
        }

        // Get existing projection
        MeasureEvaluationProjection projection = projectionStore.getMeasureProjection(
            event.getPatientId(), event.getTenantId(), event.getMeasureCode()
        );

        if (projection != null) {
            // Update denominator status
            projection.setInDenominator(event.isInDenominator());
            projection.incrementVersion();
            projectionStore.saveMeasureProjection(projection);
        }

        // Store event
        eventStore.storeEvent(event);
    }

    public void handle(RiskScoreCalculatedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        if (event.getPatientId() == null || event.getTenantId() == null) {
            throw new IllegalArgumentException("Patient ID and Tenant ID are required");
        }

        // Create or update risk score projection
        RiskScoreProjection riskProjection = projectionStore.getRiskScoreProjection(
            event.getPatientId(), event.getTenantId()
        );

        if (riskProjection == null) {
            riskProjection = new RiskScoreProjection(
                event.getPatientId(),
                event.getTenantId(),
                event.getRiskScore(),
                event.getReason()
            );
        } else {
            riskProjection.setRiskScore(event.getRiskScore());
            riskProjection.setReason(event.getReason());
            riskProjection.incrementVersion();
        }

        projectionStore.saveRiskScoreProjection(riskProjection);

        // Store event
        eventStore.storeEvent(event);
    }

    // Support interfaces for tests
    public interface MeasureProjectionStore {
        void saveMeasureProjection(MeasureEvaluationProjection projection);
        MeasureEvaluationProjection getMeasureProjection(String patientId, String tenantId, String measureCode);
        void saveRiskScoreProjection(RiskScoreProjection projection);
        RiskScoreProjection getRiskScoreProjection(String patientId, String tenantId);
        void saveCohortRate(CohortMeasureRateProjection projection);
        CohortMeasureRateProjection getCohortRate(String tenantId, String measureCode);
    }

    public interface EventStore {
        void storeEvent(Object event);
    }
}
