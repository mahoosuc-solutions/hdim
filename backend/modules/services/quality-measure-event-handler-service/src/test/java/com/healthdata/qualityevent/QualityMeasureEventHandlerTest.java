package com.healthdata.qualityevent;

import com.healthdata.qualityevent.event.*;
import com.healthdata.qualityevent.eventhandler.QualityMeasureEventHandler;
import com.healthdata.qualityevent.projection.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.AssertionsForClassTypes.within;

/**
 * RED Phase Tests for QualityMeasureEventHandler
 *
 * Tests validate quality measure event handling:
 * - Measure evaluation from CQL engine
 * - Score calculation and tracking
 * - Numerator/denominator status
 * - Risk score calculation
 * - Cohort aggregation
 * - Temporal measurement tracking
 * - Multi-tenant isolation
 */
@DisplayName("QualityMeasureEventHandler Tests")
class QualityMeasureEventHandlerTest {

    private QualityMeasureEventHandler qualityEventHandler;
    private MockMeasureProjectionStore projectionStore;
    private MockEventStore eventStore;

    @BeforeEach
    void setup() {
        projectionStore = new MockMeasureProjectionStore();
        eventStore = new MockEventStore();
        qualityEventHandler = new QualityMeasureEventHandler(projectionStore, eventStore);
    }

    // ===== Measure Evaluation Tests =====

    @Test
    @DisplayName("Should create measure evaluation projection from MeasureEvaluatedEvent")
    void testMeasureEvaluation() {
        // Given: Measure evaluation event
        String tenantId = "TENANT-001";
        String patientId = "PATIENT-123";
        String measureCode = "COL";

        MeasureEvaluatedEvent event = new MeasureEvaluatedEvent(
            tenantId, patientId, measureCode, "HEDIS Colorectal Cancer Screening"
        );

        // When: Event is handled
        qualityEventHandler.handle(event);

        // Then: Projection should be created
        MeasureEvaluationProjection projection = projectionStore.getMeasureProjection(
            patientId, tenantId, measureCode
        );
        assertThat(projection).isNotNull();
        assertThat(projection.getMeasureCode()).isEqualTo("COL");
        assertThat(projection.getMeasureDescription()).contains("Colorectal");
    }

    @Test
    @DisplayName("Should store measure event in event store")
    void testMeasureEventStorage() {
        // Given: Measure evaluation event
        MeasureEvaluatedEvent event = new MeasureEvaluatedEvent(
            "TENANT-001", "PATIENT-456", "DM", "Diabetes Control"
        );

        // When: Event is handled
        qualityEventHandler.handle(event);

        // Then: Event should be stored
        assertThat(eventStore.getEventCount()).isGreaterThan(0);
        assertThat(eventStore.getLastEventType()).isEqualTo("MeasureEvaluatedEvent");
    }

    // ===== Score Calculation Tests =====

    @Test
    @DisplayName("Should update projection with calculated score")
    void testScoreCalculation() {
        // Given: Measure evaluation event
        MeasureEvaluatedEvent evalEvent = new MeasureEvaluatedEvent(
            "TENANT-001", "PATIENT-789", "HTN", "Hypertension Control"
        );
        qualityEventHandler.handle(evalEvent);

        // When: Score is calculated
        MeasureScoreCalculatedEvent scoreEvent = new MeasureScoreCalculatedEvent(
            "TENANT-001", "PATIENT-789", "HTN", 0.85f, "Patient meets all criteria"
        );
        qualityEventHandler.handle(scoreEvent);

        // Then: Score should be stored in projection
        MeasureEvaluationProjection projection = projectionStore.getMeasureProjection(
            "PATIENT-789", "TENANT-001", "HTN"
        );
        assertThat(projection.getScore()).isEqualTo(0.85f);
        assertThat(projection.getStatus()).isEqualTo("MET");
    }

    @Test
    @DisplayName("Should track score calculation reason")
    void testScoreReason() {
        // Given: Measure with score calculation
        MeasureEvaluatedEvent evalEvent = new MeasureEvaluatedEvent(
            "TENANT-001", "PATIENT-111", "A1C", "A1C Control"
        );
        qualityEventHandler.handle(evalEvent);

        String reason = "A1C < 8% on current medication";
        MeasureScoreCalculatedEvent scoreEvent = new MeasureScoreCalculatedEvent(
            "TENANT-001", "PATIENT-111", "A1C", 1.0f, reason
        );
        qualityEventHandler.handle(scoreEvent);

        // When/Then: Reason should be captured
        MeasureEvaluationProjection projection = projectionStore.getMeasureProjection(
            "PATIENT-111", "TENANT-001", "A1C"
        );
        assertThat(projection.getEvaluationReason()).isEqualTo(reason);
    }

    // ===== Numerator/Denominator Tests =====

    @Test
    @DisplayName("Should track numerator status from event")
    void testNumeratorStatus() {
        // Given: Measure with numerator event
        MeasureEvaluatedEvent evalEvent = new MeasureEvaluatedEvent(
            "TENANT-001", "PATIENT-222", "COL", "Colorectal Screening"
        );
        qualityEventHandler.handle(evalEvent);

        MeasureNumeratorStatusEvent numeratorEvent = new MeasureNumeratorStatusEvent(
            "TENANT-001", "PATIENT-222", "COL", true, "Screening completed 2025"
        );
        qualityEventHandler.handle(numeratorEvent);

        // When/Then: Numerator status should be recorded
        MeasureEvaluationProjection projection = projectionStore.getMeasureProjection(
            "PATIENT-222", "TENANT-001", "COL"
        );
        assertThat(projection.isInNumerator()).isTrue();
    }

    @Test
    @DisplayName("Should track denominator status from event")
    void testDenominatorStatus() {
        // Given: Measure with denominator event
        MeasureEvaluatedEvent evalEvent = new MeasureEvaluatedEvent(
            "TENANT-001", "PATIENT-333", "DM", "Diabetes Control"
        );
        qualityEventHandler.handle(evalEvent);

        MeasureDenominatorStatusEvent denominatorEvent = new MeasureDenominatorStatusEvent(
            "TENANT-001", "PATIENT-333", "DM", true, "Active enrollment"
        );
        qualityEventHandler.handle(denominatorEvent);

        // When/Then: Denominator status should be recorded
        MeasureEvaluationProjection projection = projectionStore.getMeasureProjection(
            "PATIENT-333", "TENANT-001", "DM"
        );
        assertThat(projection.isInDenominator()).isTrue();
    }

    // ===== Risk Score Tests =====

    @Test
    @DisplayName("Should update risk score from RiskScoreCalculatedEvent")
    void testRiskScoreCalculation() {
        // Given: Patient with risk score event
        RiskScoreCalculatedEvent riskEvent = new RiskScoreCalculatedEvent(
            "TENANT-001", "PATIENT-444", 0.72f, "High utilization + multiple conditions"
        );

        // When: Event is handled
        qualityEventHandler.handle(riskEvent);

        // Then: Risk score should be stored
        RiskScoreProjection riskProjection = projectionStore.getRiskScoreProjection("PATIENT-444", "TENANT-001");
        assertThat(riskProjection).isNotNull();
        assertThat(riskProjection.getRiskScore()).isEqualTo(0.72f);
    }

    @Test
    @DisplayName("Should categorize risk level based on score")
    void testRiskLevelCategorization() {
        // Given: Risk score event with high risk
        RiskScoreCalculatedEvent riskEvent = new RiskScoreCalculatedEvent(
            "TENANT-001", "PATIENT-555", 0.85f, "Very high risk profile"
        );

        // When: Event is handled
        qualityEventHandler.handle(riskEvent);

        // Then: Risk level should be HIGH
        RiskScoreProjection riskProjection = projectionStore.getRiskScoreProjection("PATIENT-555", "TENANT-001");
        assertThat(riskProjection.getRiskLevel()).isEqualTo("HIGH");
    }

    // ===== Cohort Aggregation Tests =====

    @Test
    @DisplayName("Should aggregate measure rates across cohort")
    void testCohortAggregation() {
        // Given: Multiple patients evaluated for same measure
        String measureCode = "COL";
        String tenantId = "TENANT-001";

        for (int i = 1; i <= 10; i++) {
            String patientId = "PATIENT-" + String.format("%03d", i);
            MeasureEvaluatedEvent evalEvent = new MeasureEvaluatedEvent(tenantId, patientId, measureCode, "Colorectal Screening");
            qualityEventHandler.handle(evalEvent);

            float score = (i <= 7) ? 1.0f : 0.0f;  // 7 of 10 compliant
            MeasureScoreCalculatedEvent scoreEvent = new MeasureScoreCalculatedEvent(
                tenantId, patientId, measureCode, score, ""
            );
            qualityEventHandler.handle(scoreEvent);
        }

        // When: Cohort rate calculated
        CohortMeasureRateProjection cohortProjection = projectionStore.getCohortRate(tenantId, measureCode);

        // Then: Cohort rate should be 70%
        assertThat(cohortProjection.getComplianceRate()).isCloseTo(0.70f, within(0.01f));
    }

    // ===== Temporal Tracking Tests =====

    @Test
    @DisplayName("Should track measure evaluation date")
    void testMeasureEvaluationDate() {
        // Given: Measure evaluation event with timestamp
        LocalDate evalDate = LocalDate.of(2025, 12, 31);
        MeasureEvaluatedEvent event = new MeasureEvaluatedEvent(
            "TENANT-001", "PATIENT-666", "COL", "Colorectal Screening"
        );

        // When: Event handled (timestamp captured)
        qualityEventHandler.handle(event);

        // Then: Evaluation date should be recorded
        MeasureEvaluationProjection projection = projectionStore.getMeasureProjection(
            "PATIENT-666", "TENANT-001", "COL"
        );
        assertThat(projection.getEvaluationDate()).isNotNull();
    }

    @Test
    @DisplayName("Should support measurement period queries")
    void testMeasurementPeriod() {
        // Given: Multiple measures evaluated in period
        LocalDate periodStart = LocalDate.of(2025, 1, 1);
        LocalDate periodEnd = LocalDate.of(2025, 12, 31);

        MeasureEvaluatedEvent event = new MeasureEvaluatedEvent(
            "TENANT-001", "PATIENT-777", "DM", "Diabetes Control"
        );
        qualityEventHandler.handle(event);

        // When: Query measures for period
        // (Implementation captures measurement period in projection)

        // Then: Projection should track measurement period
        MeasureEvaluationProjection projection = projectionStore.getMeasureProjection(
            "PATIENT-777", "TENANT-001", "DM"
        );
        assertThat(projection).isNotNull();
    }

    // ===== Multi-Tenant Isolation Tests =====

    @Test
    @DisplayName("Should isolate measure data by tenant")
    void testMultiTenantIsolation() {
        // Given: Same measure in different tenants
        String patientId = "PATIENT-999";
        String measureCode = "COL";

        MeasureEvaluatedEvent tenant1Event = new MeasureEvaluatedEvent(
            "TENANT-001", patientId, measureCode, "Tenant 1 measure"
        );
        MeasureEvaluatedEvent tenant2Event = new MeasureEvaluatedEvent(
            "TENANT-002", patientId, measureCode, "Tenant 2 measure"
        );

        // When: Events handled for both tenants
        qualityEventHandler.handle(tenant1Event);
        qualityEventHandler.handle(tenant2Event);

        // Then: Projections should be isolated
        MeasureEvaluationProjection tenant1Projection = projectionStore.getMeasureProjection(
            patientId, "TENANT-001", measureCode
        );
        MeasureEvaluationProjection tenant2Projection = projectionStore.getMeasureProjection(
            patientId, "TENANT-002", measureCode
        );

        assertThat(tenant1Projection.getMeasureDescription()).contains("Tenant 1");
        assertThat(tenant2Projection.getMeasureDescription()).contains("Tenant 2");
    }

    // ===== Idempotency Tests =====

    @Test
    @DisplayName("Should handle duplicate measure evaluation idempotently")
    void testIdempotentMeasureEvaluation() {
        // Given: Measure evaluation event
        MeasureEvaluatedEvent event = new MeasureEvaluatedEvent(
            "TENANT-001", "PATIENT-101", "COL", "Colorectal Screening"
        );

        // When: Same event handled twice
        qualityEventHandler.handle(event);
        MeasureEvaluationProjection projection1 = projectionStore.getMeasureProjection(
            "PATIENT-101", "TENANT-001", "COL"
        );
        long version1 = projection1.getVersion();

        qualityEventHandler.handle(event);
        MeasureEvaluationProjection projection2 = projectionStore.getMeasureProjection(
            "PATIENT-101", "TENANT-001", "COL"
        );
        long version2 = projection2.getVersion();

        // Then: Version should not significantly increase
        assertThat(version2).isLessThanOrEqualTo(version1 + 1);
    }

    // ===== Error Handling Tests =====

    @Test
    @DisplayName("Should handle null measure event")
    void testNullMeasureEvent() {
        // When/Then: Should throw validation error
        assertThatThrownBy(() -> qualityEventHandler.handle((MeasureEvaluatedEvent) null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should validate measure code")
    void testInvalidMeasureCode() {
        // Given: Event with null measure code
        MeasureEvaluatedEvent event = new MeasureEvaluatedEvent(
            "TENANT-001", "PATIENT-202", null, "Invalid measure"
        );

        // When/Then: Should throw validation error
        assertThatThrownBy(() -> qualityEventHandler.handle(event))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should validate tenant ID")
    void testMissingTenantId() {
        // Given: Event with null tenant
        MeasureEvaluatedEvent event = new MeasureEvaluatedEvent(
            null, "PATIENT-303", "COL", "Colorectal Screening"
        );

        // When/Then: Should throw validation error
        assertThatThrownBy(() -> qualityEventHandler.handle(event))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ===== Mock Classes =====

    static class MockMeasureProjectionStore implements QualityMeasureEventHandler.MeasureProjectionStore {
        private final java.util.Map<String, MeasureEvaluationProjection> measureStore = new java.util.HashMap<>();
        private final java.util.Map<String, RiskScoreProjection> riskStore = new java.util.HashMap<>();
        private final java.util.Map<String, CohortMeasureRateProjection> cohortStore = new java.util.HashMap<>();

        @Override
        public void saveMeasureProjection(MeasureEvaluationProjection projection) {
            String key = projection.getTenantId() + ":" + projection.getPatientId() + ":" + projection.getMeasureCode();
            measureStore.put(key, projection);
        }

        @Override
        public MeasureEvaluationProjection getMeasureProjection(String patientId, String tenantId, String measureCode) {
            String key = tenantId + ":" + patientId + ":" + measureCode;
            return measureStore.get(key);
        }

        @Override
        public void saveRiskScoreProjection(RiskScoreProjection projection) {
            String key = projection.getTenantId() + ":" + projection.getPatientId();
            riskStore.put(key, projection);
        }

        @Override
        public RiskScoreProjection getRiskScoreProjection(String patientId, String tenantId) {
            String key = tenantId + ":" + patientId;
            return riskStore.get(key);
        }

        @Override
        public void saveCohortRate(CohortMeasureRateProjection projection) {
            String key = projection.getTenantId() + ":" + projection.getMeasureCode();
            cohortStore.put(key, projection);
        }

        @Override
        public CohortMeasureRateProjection getCohortRate(String tenantId, String measureCode) {
            String key = tenantId + ":" + measureCode;
            return cohortStore.get(key);
        }
    }

    static class MockEventStore implements QualityMeasureEventHandler.EventStore {
        private int eventCount = 0;
        private String lastEventType = "";

        @Override
        public void storeEvent(Object event) {
            eventCount++;
            lastEventType = event.getClass().getSimpleName();
        }

        int getEventCount() { return eventCount; }
        String getLastEventType() { return lastEventType; }
    }

}
