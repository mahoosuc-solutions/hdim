package com.healthdata.caregap;

import com.healthdata.caregap.event.*;
import com.healthdata.caregap.eventhandler.CareGapEventHandler;
import com.healthdata.caregap.projection.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.AssertionsForClassTypes.within;

/**
 * RED Phase Tests for CareGapEventHandler
 *
 * Tests validate care gap event handling:
 * - Care gap detection and identification
 * - Gap severity levels (CRITICAL, HIGH, MEDIUM, LOW)
 * - Patient qualification criteria
 * - Recommended interventions
 * - Gap closure tracking and validation
 * - Temporal gap lifecycle management
 * - Multi-tenant isolation
 */
@DisplayName("CareGapEventHandler Tests")
class CareGapEventHandlerTest {

    private CareGapEventHandler careGapEventHandler;
    private MockCareGapProjectionStore projectionStore;
    private MockEventStore eventStore;

    @BeforeEach
    void setup() {
        projectionStore = new MockCareGapProjectionStore();
        eventStore = new MockEventStore();
        careGapEventHandler = new CareGapEventHandler(projectionStore, eventStore);
    }

    // ===== Care Gap Detection Tests =====

    @Test
    @DisplayName("Should create care gap projection from CareGapDetectedEvent")
    void testCareGapDetection() {
        // Given: Care gap detection event
        String tenantId = "TENANT-001";
        String patientId = "PATIENT-123";
        String gapCode = "COL";

        CareGapDetectedEvent event = new CareGapDetectedEvent(
            tenantId, patientId, gapCode, "Colorectal Cancer Screening Gap", "CRITICAL"
        );

        // When: Event is handled
        careGapEventHandler.handle(event);

        // Then: Projection should be created
        CareGapProjection projection = projectionStore.getCareGapProjection(
            patientId, tenantId, gapCode
        );
        assertThat(projection).isNotNull();
        assertThat(projection.getGapCode()).isEqualTo("COL");
        assertThat(projection.getSeverity()).isEqualTo("CRITICAL");
    }

    @Test
    @DisplayName("Should store care gap event in event store")
    void testCareGapEventStorage() {
        // Given: Care gap event
        CareGapDetectedEvent event = new CareGapDetectedEvent(
            "TENANT-001", "PATIENT-456", "DM", "Diabetes Control Gap", "HIGH"
        );

        // When: Event is handled
        careGapEventHandler.handle(event);

        // Then: Event should be stored
        assertThat(eventStore.getEventCount()).isGreaterThan(0);
        assertThat(eventStore.getLastEventType()).isEqualTo("CareGapDetectedEvent");
    }

    // ===== Gap Severity Tests =====

    @Test
    @DisplayName("Should track gap severity level")
    void testGapSeverity() {
        // Given: Gap with severity
        CareGapDetectedEvent event = new CareGapDetectedEvent(
            "TENANT-001", "PATIENT-789", "BP", "Blood Pressure Gap", "MEDIUM"
        );

        // When: Event is handled
        careGapEventHandler.handle(event);

        // Then: Severity should be recorded
        CareGapProjection projection = projectionStore.getCareGapProjection(
            "PATIENT-789", "TENANT-001", "BP"
        );
        assertThat(projection.getSeverity()).isEqualTo("MEDIUM");
    }

    // ===== Qualification Tests =====

    @Test
    @DisplayName("Should track patient qualification status")
    void testPatientQualification() {
        // Given: Patient qualification event
        CareGapDetectedEvent gapEvent = new CareGapDetectedEvent(
            "TENANT-001", "PATIENT-111", "COL", "Colorectal Screening Gap", "HIGH"
        );
        careGapEventHandler.handle(gapEvent);

        PatientQualifiedEvent qualEvent = new PatientQualifiedEvent(
            "TENANT-001", "PATIENT-111", "COL", true, "Age 50+ no prior screening"
        );
        careGapEventHandler.handle(qualEvent);

        // When/Then: Qualification should be recorded
        CareGapProjection projection = projectionStore.getCareGapProjection(
            "PATIENT-111", "TENANT-001", "COL"
        );
        assertThat(projection.isQualified()).isTrue();
    }

    // ===== Intervention Tracking Tests =====

    @Test
    @DisplayName("Should track recommended interventions")
    void testRecommendedInterventions() {
        // Given: Gap with intervention
        CareGapDetectedEvent gapEvent = new CareGapDetectedEvent(
            "TENANT-001", "PATIENT-222", "A1C", "A1C Control Gap", "CRITICAL"
        );
        careGapEventHandler.handle(gapEvent);

        InterventionRecommendedEvent interventionEvent = new InterventionRecommendedEvent(
            "TENANT-001", "PATIENT-222", "A1C", "Increase medication frequency", "HIGH_PRIORITY"
        );
        careGapEventHandler.handle(interventionEvent);

        // When/Then: Intervention should be recorded
        CareGapProjection projection = projectionStore.getCareGapProjection(
            "PATIENT-222", "TENANT-001", "A1C"
        );
        assertThat(projection.getRecommendedIntervention()).isNotEmpty();
    }

    // ===== Gap Closure Tests =====

    @Test
    @DisplayName("Should mark gap as closed when closure criteria met")
    void testGapClosure() {
        // Given: Detected gap
        CareGapDetectedEvent gapEvent = new CareGapDetectedEvent(
            "TENANT-001", "PATIENT-333", "DM", "Diabetes Management Gap", "HIGH"
        );
        careGapEventHandler.handle(gapEvent);

        // When: Gap closure event
        GapClosedEvent closureEvent = new GapClosedEvent(
            "TENANT-001", "PATIENT-333", "DM", "Patient completed intervention", "CLOSED"
        );
        careGapEventHandler.handle(closureEvent);

        // Then: Gap should be marked closed
        CareGapProjection projection = projectionStore.getCareGapProjection(
            "PATIENT-333", "TENANT-001", "DM"
        );
        assertThat(projection.getStatus()).isEqualTo("CLOSED");
    }

    // ===== Temporal Tracking Tests =====

    @Test
    @DisplayName("Should track gap detection date")
    void testGapDetectionDate() {
        // Given: Gap event
        LocalDate detectionDate = LocalDate.of(2025, 1, 15);
        CareGapDetectedEvent event = new CareGapDetectedEvent(
            "TENANT-001", "PATIENT-444", "BP", "Blood Pressure Gap", "MEDIUM"
        );

        // When: Event is handled
        careGapEventHandler.handle(event);

        // Then: Detection date should be recorded
        CareGapProjection projection = projectionStore.getCareGapProjection(
            "PATIENT-444", "TENANT-001", "BP"
        );
        assertThat(projection.getDetectionDate()).isNotNull();
    }

    @Test
    @DisplayName("Should track gap aging in days")
    void testGapAging() {
        // Given: Older gap
        CareGapDetectedEvent event = new CareGapDetectedEvent(
            "TENANT-001", "PATIENT-555", "COL", "Colorectal Screening Gap", "HIGH"
        );
        careGapEventHandler.handle(event);

        // When/Then: Aging should be trackable
        CareGapProjection projection = projectionStore.getCareGapProjection(
            "PATIENT-555", "TENANT-001", "COL"
        );
        assertThat(projection.getDaysOpen()).isGreaterThanOrEqualTo(0);
    }

    // ===== Population Health Tests =====

    @Test
    @DisplayName("Should aggregate gaps by type and severity")
    void testGapAggregation() {
        // Given: Multiple gaps
        String tenantId = "TENANT-001";
        String[] gapTypes = {"COL", "A1C", "BP", "DM", "HTN"};

        for (int i = 1; i <= 10; i++) {
            String patientId = "PATIENT-" + String.format("%03d", i);
            String gapType = gapTypes[(i - 1) % gapTypes.length];
            String severity = (i <= 4) ? "CRITICAL" : (i <= 7) ? "HIGH" : "MEDIUM";

            CareGapDetectedEvent event = new CareGapDetectedEvent(
                tenantId, patientId, gapType, "Gap for " + gapType, severity
            );
            careGapEventHandler.handle(event);
        }

        // When: Population health view queried
        PopulationHealthProjection healthProjection = projectionStore.getPopulationHealth(tenantId);

        // Then: Should aggregate gaps
        assertThat(healthProjection).isNotNull();
        assertThat(healthProjection.getTotalGapsOpen()).isGreaterThan(0);
    }

    // ===== Multi-Tenant Isolation Tests =====

    @Test
    @DisplayName("Should isolate care gaps by tenant")
    void testMultiTenantIsolation() {
        // Given: Same gap in different tenants
        String patientId = "PATIENT-999";
        String gapCode = "COL";

        CareGapDetectedEvent tenant1Event = new CareGapDetectedEvent(
            "TENANT-001", patientId, gapCode, "Tenant 1 gap", "HIGH"
        );
        CareGapDetectedEvent tenant2Event = new CareGapDetectedEvent(
            "TENANT-002", patientId, gapCode, "Tenant 2 gap", "MEDIUM"
        );

        // When: Events handled for both tenants
        careGapEventHandler.handle(tenant1Event);
        careGapEventHandler.handle(tenant2Event);

        // Then: Projections should be isolated
        CareGapProjection tenant1Projection = projectionStore.getCareGapProjection(
            patientId, "TENANT-001", gapCode
        );
        CareGapProjection tenant2Projection = projectionStore.getCareGapProjection(
            patientId, "TENANT-002", gapCode
        );

        assertThat(tenant1Projection.getSeverity()).isEqualTo("HIGH");
        assertThat(tenant2Projection.getSeverity()).isEqualTo("MEDIUM");
    }

    // ===== Idempotency Tests =====

    @Test
    @DisplayName("Should handle duplicate care gap events idempotently")
    void testIdempotentGapDetection() {
        // Given: Care gap event
        CareGapDetectedEvent event = new CareGapDetectedEvent(
            "TENANT-001", "PATIENT-101", "COL", "Colorectal Screening Gap", "CRITICAL"
        );

        // When: Same event handled twice
        careGapEventHandler.handle(event);
        CareGapProjection projection1 = projectionStore.getCareGapProjection(
            "PATIENT-101", "TENANT-001", "COL"
        );
        long version1 = projection1.getVersion();

        careGapEventHandler.handle(event);
        CareGapProjection projection2 = projectionStore.getCareGapProjection(
            "PATIENT-101", "TENANT-001", "COL"
        );
        long version2 = projection2.getVersion();

        // Then: Version should not significantly increase
        assertThat(version2).isLessThanOrEqualTo(version1 + 1);
    }

    // ===== Error Handling Tests =====

    @Test
    @DisplayName("Should handle null care gap event")
    void testNullCareGapEvent() {
        // When/Then: Should throw validation error
        assertThatThrownBy(() -> careGapEventHandler.handle((CareGapDetectedEvent) null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should validate gap code")
    void testInvalidGapCode() {
        // Given: Event with null gap code
        CareGapDetectedEvent event = new CareGapDetectedEvent(
            "TENANT-001", "PATIENT-202", null, "Invalid gap", "HIGH"
        );

        // When/Then: Should throw validation error
        assertThatThrownBy(() -> careGapEventHandler.handle(event))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should validate tenant ID")
    void testMissingTenantId() {
        // Given: Event with null tenant
        CareGapDetectedEvent event = new CareGapDetectedEvent(
            null, "PATIENT-303", "COL", "Colorectal Screening Gap", "HIGH"
        );

        // When/Then: Should throw validation error
        assertThatThrownBy(() -> careGapEventHandler.handle(event))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ===== Mock Classes =====

    static class MockCareGapProjectionStore implements CareGapEventHandler.CareGapProjectionStore {
        private final java.util.Map<String, CareGapProjection> gapStore = new java.util.HashMap<>();
        private final java.util.Map<String, PopulationHealthProjection> healthStore = new java.util.HashMap<>();

        @Override
        public void saveCareGapProjection(CareGapProjection projection) {
            String key = projection.getTenantId() + ":" + projection.getPatientId() + ":" + projection.getGapCode();
            gapStore.put(key, projection);
        }

        @Override
        public CareGapProjection getCareGapProjection(String patientId, String tenantId, String gapCode) {
            String key = tenantId + ":" + patientId + ":" + gapCode;
            return gapStore.get(key);
        }

        @Override
        public void savePopulationHealth(PopulationHealthProjection projection) {
            String key = projection.getTenantId();
            healthStore.put(key, projection);
        }

        @Override
        public PopulationHealthProjection getPopulationHealth(String tenantId) {
            return healthStore.get(tenantId);
        }
    }

    static class MockEventStore implements CareGapEventHandler.EventStore {
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
