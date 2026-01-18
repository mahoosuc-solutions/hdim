package com.healthdata.patientevent;

import com.healthdata.patientevent.event.*;
import com.healthdata.patientevent.eventhandler.PatientEventHandler;
import com.healthdata.patientevent.projection.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * RED Phase Tests for PatientEventHandler
 *
 * Test-driven development: Tests written FIRST, implementation follows.
 * These tests validate patient lifecycle event handling:
 * - Patient creation with demographics
 * - Enrollment status management
 * - Demographic updates
 * - Patient activation/deactivation
 * - Multi-tenant isolation
 * - Temporal consistency
 */
@DisplayName("PatientEventHandler Tests")
class PatientEventHandlerTest {

    private PatientEventHandler patientEventHandler;
    private MockPatientProjectionStore projectionStore;
    private MockEventStore eventStore;

    @BeforeEach
    void setup() {
        projectionStore = new MockPatientProjectionStore();
        eventStore = new MockEventStore();
        patientEventHandler = new PatientEventHandler(projectionStore, eventStore);
    }

    // ===== Patient Creation Tests =====

    @Test
    @DisplayName("Should create patient projection from PatientCreatedEvent")
    void testPatientCreation() {
        // Given: Patient creation event
        String patientId = "PATIENT-001";
        String tenantId = "TENANT-001";
        PatientCreatedEvent event = new PatientCreatedEvent(
            patientId, tenantId, "John", "Doe", "1965-01-15"
        );

        // When: Event is handled
        patientEventHandler.handle(event);

        // Then: Projection should be created
        PatientActiveProjection projection = projectionStore.getPatientProjection(patientId, tenantId);
        assertThat(projection).isNotNull();
        assertThat(projection.getPatientId()).isEqualTo(patientId);
        assertThat(projection.getFirstName()).isEqualTo("John");
        assertThat(projection.getLastName()).isEqualTo("Doe");
        assertThat(projection.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Should store event in event store after handling")
    void testEventStorageOnCreation() {
        // Given: Patient creation event
        PatientCreatedEvent event = new PatientCreatedEvent(
            "PATIENT-002", "TENANT-001", "Jane", "Smith", "1970-03-20"
        );

        // When: Event is handled
        patientEventHandler.handle(event);

        // Then: Event should be stored
        assertThat(eventStore.getEventCount()).isGreaterThan(0);
        assertThat(eventStore.getLastEventType()).isEqualTo("PatientCreatedEvent");
    }

    @Test
    @DisplayName("Should set initial enrollment status as ACTIVE")
    void testInitialEnrollmentStatus() {
        // Given: Patient creation event
        PatientCreatedEvent event = new PatientCreatedEvent(
            "PATIENT-003", "TENANT-001", "Bob", "Johnson", "1975-06-10"
        );

        // When: Event is handled
        patientEventHandler.handle(event);

        // Then: Enrollment status should be ACTIVE
        PatientActiveProjection projection = projectionStore.getPatientProjection("PATIENT-003", "TENANT-001");
        assertThat(projection.getEnrollmentStatus()).isEqualTo("ACTIVE");
    }

    // ===== Enrollment Status Changes =====

    @Test
    @DisplayName("Should update enrollment status when PatientEnrollmentChangedEvent received")
    void testEnrollmentStatusChange() {
        // Given: Existing patient
        String patientId = "PATIENT-004";
        String tenantId = "TENANT-001";
        PatientCreatedEvent createEvent = new PatientCreatedEvent(
            patientId, tenantId, "Alice", "Brown", "1968-09-25"
        );
        patientEventHandler.handle(createEvent);

        // When: Enrollment changes to INACTIVE
        PatientEnrollmentChangedEvent enrollmentEvent = new PatientEnrollmentChangedEvent(
            patientId, tenantId, "INACTIVE", "Policy terminated"
        );
        patientEventHandler.handle(enrollmentEvent);

        // Then: Enrollment status should be updated
        PatientActiveProjection projection = projectionStore.getPatientProjection(patientId, tenantId);
        assertThat(projection.getEnrollmentStatus()).isEqualTo("INACTIVE");
    }

    @Test
    @DisplayName("Should track enrollment reason in projection")
    void testEnrollmentReason() {
        // Given: Patient with enrollment change
        String patientId = "PATIENT-005";
        String tenantId = "TENANT-001";
        PatientCreatedEvent createEvent = new PatientCreatedEvent(
            patientId, tenantId, "Charlie", "Davis", "1980-12-01"
        );
        patientEventHandler.handle(createEvent);

        PatientEnrollmentChangedEvent enrollmentEvent = new PatientEnrollmentChangedEvent(
            patientId, tenantId, "SUSPENDED", "Medical leave"
        );
        patientEventHandler.handle(enrollmentEvent);

        // When/Then: Reason should be captured
        PatientActiveProjection projection = projectionStore.getPatientProjection(patientId, tenantId);
        assertThat(projection.getEnrollmentReason()).isEqualTo("Medical leave");
    }

    // ===== Demographics Updates =====

    @Test
    @DisplayName("Should update demographics on PatientDemographicsUpdatedEvent")
    void testDemographicsUpdate() {
        // Given: Existing patient
        String patientId = "PATIENT-006";
        String tenantId = "TENANT-001";
        PatientCreatedEvent createEvent = new PatientCreatedEvent(
            patientId, tenantId, "Diana", "Evans", "1972-04-15"
        );
        patientEventHandler.handle(createEvent);

        // When: Demographics updated (name change after marriage)
        PatientDemographicsUpdatedEvent demographicsEvent = new PatientDemographicsUpdatedEvent(
            patientId, tenantId, "Diana", "Wilson", "1972-04-15"
        );
        patientEventHandler.handle(demographicsEvent);

        // Then: Last name should be updated
        PatientActiveProjection projection = projectionStore.getPatientProjection(patientId, tenantId);
        assertThat(projection.getFirstName()).isEqualTo("Diana");
        assertThat(projection.getLastName()).isEqualTo("Wilson");
    }

    @Test
    @DisplayName("Should increment projection version on update")
    void testProjectionVersionIncrement() {
        // Given: Patient and demographics update
        String patientId = "PATIENT-007";
        String tenantId = "TENANT-001";
        PatientCreatedEvent createEvent = new PatientCreatedEvent(
            patientId, tenantId, "Edward", "Foster", "1965-11-20"
        );
        patientEventHandler.handle(createEvent);

        long versionAfterCreate = projectionStore.getPatientProjection(patientId, tenantId).getVersion();

        PatientDemographicsUpdatedEvent demographicsEvent = new PatientDemographicsUpdatedEvent(
            patientId, tenantId, "Edward", "Grant", "1965-11-20"
        );
        patientEventHandler.handle(demographicsEvent);

        // When/Then: Version should increase
        long versionAfterUpdate = projectionStore.getPatientProjection(patientId, tenantId).getVersion();
        assertThat(versionAfterUpdate).isGreaterThan(versionAfterCreate);
    }

    // ===== Patient Activation/Deactivation =====

    @Test
    @DisplayName("Should deactivate patient when PatientDeactivatedEvent received")
    void testPatientDeactivation() {
        // Given: Active patient
        String patientId = "PATIENT-008";
        String tenantId = "TENANT-001";
        PatientCreatedEvent createEvent = new PatientCreatedEvent(
            patientId, tenantId, "Fiona", "Graham", "1978-07-30"
        );
        patientEventHandler.handle(createEvent);

        // When: Patient deactivated
        PatientDeactivatedEvent deactivateEvent = new PatientDeactivatedEvent(
            patientId, tenantId, "Patient deceased"
        );
        patientEventHandler.handle(deactivateEvent);

        // Then: Status should be INACTIVE
        PatientActiveProjection projection = projectionStore.getPatientProjection(patientId, tenantId);
        assertThat(projection.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    @DisplayName("Should reactivate patient when PatientActivatedEvent received")
    void testPatientReactivation() {
        // Given: Deactivated patient
        String patientId = "PATIENT-009";
        String tenantId = "TENANT-001";
        PatientCreatedEvent createEvent = new PatientCreatedEvent(
            patientId, tenantId, "George", "Harper", "1962-02-14"
        );
        patientEventHandler.handle(createEvent);

        PatientDeactivatedEvent deactivateEvent = new PatientDeactivatedEvent(
            patientId, tenantId, "Temporarily inactive"
        );
        patientEventHandler.handle(deactivateEvent);

        // When: Patient reactivated
        PatientActivatedEvent activateEvent = new PatientActivatedEvent(
            patientId, tenantId, "Reactivated by admin"
        );
        patientEventHandler.handle(activateEvent);

        // Then: Status should be ACTIVE again
        PatientActiveProjection projection = projectionStore.getPatientProjection(patientId, tenantId);
        assertThat(projection.getStatus()).isEqualTo("ACTIVE");
    }

    // ===== Multi-Tenant Isolation Tests =====

    @Test
    @DisplayName("Should isolate patients by tenant")
    void testMultiTenantIsolation() {
        // Given: Same patient ID in different tenants
        String patientId = "PATIENT-010";
        PatientCreatedEvent tenant1Event = new PatientCreatedEvent(
            patientId, "TENANT-001", "Helen", "Ingram", "1985-05-08"
        );
        PatientCreatedEvent tenant2Event = new PatientCreatedEvent(
            patientId, "TENANT-002", "Henry", "Jones", "1990-08-12"
        );

        // When: Events handled for both tenants
        patientEventHandler.handle(tenant1Event);
        patientEventHandler.handle(tenant2Event);

        // Then: Projections should be separate
        PatientActiveProjection tenant1Projection = projectionStore.getPatientProjection(patientId, "TENANT-001");
        PatientActiveProjection tenant2Projection = projectionStore.getPatientProjection(patientId, "TENANT-002");

        assertThat(tenant1Projection.getFirstName()).isEqualTo("Helen");
        assertThat(tenant2Projection.getFirstName()).isEqualTo("Henry");
    }

    @Test
    @DisplayName("Should prevent cross-tenant access")
    void testCrossTenantAccessPrevention() {
        // Given: Patient in TENANT-001
        String patientId = "PATIENT-011";
        PatientCreatedEvent event = new PatientCreatedEvent(
            patientId, "TENANT-001", "Iris", "Kennedy", "1975-10-22"
        );
        patientEventHandler.handle(event);

        // When/Then: Should not find patient in different tenant
        PatientActiveProjection projection = projectionStore.getPatientProjection(patientId, "TENANT-002");
        assertThat(projection).isNull();
    }

    // ===== Idempotency Tests =====

    @Test
    @DisplayName("Should handle duplicate PatientCreatedEvent idempotently")
    void testIdempotentCreation() {
        // Given: Patient creation event
        String patientId = "PATIENT-012";
        String tenantId = "TENANT-001";
        PatientCreatedEvent event = new PatientCreatedEvent(
            patientId, tenantId, "Jack", "Lewis", "1968-03-18"
        );

        // When: Same event handled twice
        patientEventHandler.handle(event);
        PatientActiveProjection projection1 = projectionStore.getPatientProjection(patientId, tenantId);
        long version1 = projection1.getVersion();

        patientEventHandler.handle(event);
        PatientActiveProjection projection2 = projectionStore.getPatientProjection(patientId, tenantId);
        long version2 = projection2.getVersion();

        // Then: Projection should not change
        assertThat(projection1.getFirstName()).isEqualTo(projection2.getFirstName());
        assertThat(version2).isLessThanOrEqualTo(version1 + 1);
    }

    // ===== Temporal Consistency Tests =====

    @Test
    @DisplayName("Should maintain temporal ordering of events")
    void testTemporalOrdering() {
        // Given: Series of events
        String patientId = "PATIENT-013";
        String tenantId = "TENANT-001";

        PatientCreatedEvent createEvent = new PatientCreatedEvent(
            patientId, tenantId, "Karen", "Miller", "1980-01-05"
        );
        PatientEnrollmentChangedEvent enrollmentEvent = new PatientEnrollmentChangedEvent(
            patientId, tenantId, "ACTIVE", "Enrolled in plan"
        );
        PatientDemographicsUpdatedEvent demographicsEvent = new PatientDemographicsUpdatedEvent(
            patientId, tenantId, "Karen", "Martinez", "1980-01-05"
        );

        // When: Events handled in order
        patientEventHandler.handle(createEvent);
        patientEventHandler.handle(enrollmentEvent);
        patientEventHandler.handle(demographicsEvent);

        // Then: Final projection should reflect all changes
        PatientActiveProjection projection = projectionStore.getPatientProjection(patientId, tenantId);
        assertThat(projection.getFirstName()).isEqualTo("Karen");
        assertThat(projection.getLastName()).isEqualTo("Martinez");
        assertThat(projection.getEnrollmentStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Should handle out-of-order event processing")
    void testOutOfOrderEventHandling() {
        // Given: Events that might be out of order
        String patientId = "PATIENT-014";
        String tenantId = "TENANT-001";

        PatientCreatedEvent createEvent = new PatientCreatedEvent(
            patientId, tenantId, "Leo", "Nelson", "1955-07-14"
        );
        PatientDemographicsUpdatedEvent demographicsEvent = new PatientDemographicsUpdatedEvent(
            patientId, tenantId, "Leo", "Owen", "1955-07-14"
        );

        // When: Demographics update handled before creation (edge case)
        patientEventHandler.handle(demographicsEvent);
        patientEventHandler.handle(createEvent);

        // Then: Creation should establish the patient, update should apply
        PatientActiveProjection projection = projectionStore.getPatientProjection(patientId, tenantId);
        assertThat(projection).isNotNull();
        assertThat(projection.getPatientId()).isEqualTo(patientId);
    }

    // ===== Error Handling Tests =====

    @Test
    @DisplayName("Should handle null event gracefully")
    void testNullEventHandling() {
        // When/Then: Should not throw NPE
        assertThatThrownBy(() -> patientEventHandler.handle((PatientCreatedEvent) null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should handle missing patient ID")
    void testMissingPatientId() {
        // Given: Event with null patient ID
        PatientCreatedEvent event = new PatientCreatedEvent(
            null, "TENANT-001", "Mike", "Phillips", "1960-12-25"
        );

        // When/Then: Should throw validation exception
        assertThatThrownBy(() -> patientEventHandler.handle(event))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should handle missing tenant ID")
    void testMissingTenantId() {
        // Given: Event with null tenant ID
        PatientCreatedEvent event = new PatientCreatedEvent(
            "PATIENT-015", null, "Nancy", "Quinn", "1970-05-30"
        );

        // When/Then: Should throw validation exception
        assertThatThrownBy(() -> patientEventHandler.handle(event))
            .isInstanceOf(IllegalArgumentException.class);
    }

    // ===== Mock Classes =====

    static class MockPatientProjectionStore implements PatientEventHandler.PatientProjectionStore {
        private final java.util.Map<String, PatientActiveProjection> store = new java.util.HashMap<>();

        @Override
        public void saveProjection(PatientActiveProjection projection) {
            String key = projection.getTenantId() + ":" + projection.getPatientId();
            store.put(key, projection);
        }

        @Override
        public PatientActiveProjection getPatientProjection(String patientId, String tenantId) {
            String key = tenantId + ":" + patientId;
            return store.get(key);
        }
    }

    static class MockEventStore implements PatientEventHandler.EventStore {
        private int eventCount = 0;
        private String lastEventType = "";

        @Override
        public void storeEvent(Object event) {
            eventCount++;
            lastEventType = event.getClass().getSimpleName();
        }

        int getEventCount() {
            return eventCount;
        }

        String getLastEventType() {
            return lastEventType;
        }
    }
}
