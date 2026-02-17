package com.healthdata.fhireventbridge.listener;

import com.healthdata.fhireventbridge.event.FhirPatientEvent;
import com.healthdata.fhireventbridge.service.PatientEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FhirPatientEventListener.
 *
 * Covers the three @KafkaListener methods:
 * - consumeFhirPatientCreated
 * - consumeFhirPatientUpdated
 * - consumeFhirPatientLinked
 *
 * Verifies: happy-path delegation to PatientEventPublisher,
 * manual ACK on success, and re-throw (for DLT delivery) on failure.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
class FhirPatientEventListenerTest {

    @Mock
    private PatientEventPublisher patientEventPublisher;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private FhirPatientEventListener listener;

    private FhirPatientEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = FhirPatientEvent.builder()
                .fhirResourceId("Patient/test-fhir-id-123")
                .tenantId("test-tenant")
                .linkedFhirResourceId(null)
                .linkType(null)
                .build();
    }

    // ── consumeFhirPatientCreated ──────────────────────────────────────────────

    @Test
    void consumeFhirPatientCreated_publishesEventAndAcknowledges() {
        listener.consumeFhirPatientCreated(testEvent, 0, 0L, acknowledgment);

        verify(patientEventPublisher).publishPatientCreated(testEvent);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeFhirPatientCreated_whenPublisherThrows_rethrowsAndDoesNotAcknowledge() {
        doThrow(new RuntimeException("Publisher unavailable"))
                .when(patientEventPublisher).publishPatientCreated(testEvent);

        assertThatThrownBy(() ->
                listener.consumeFhirPatientCreated(testEvent, 0, 0L, acknowledgment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process FHIR patient.created event");

        verify(acknowledgment, never()).acknowledge();
    }

    // ── consumeFhirPatientUpdated ─────────────────────────────────────────────

    @Test
    void consumeFhirPatientUpdated_publishesEventAndAcknowledges() {
        listener.consumeFhirPatientUpdated(testEvent, acknowledgment);

        verify(patientEventPublisher).publishPatientUpdated(testEvent);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeFhirPatientUpdated_whenPublisherThrows_rethrowsAndDoesNotAcknowledge() {
        doThrow(new RuntimeException("Publisher unavailable"))
                .when(patientEventPublisher).publishPatientUpdated(testEvent);

        assertThatThrownBy(() ->
                listener.consumeFhirPatientUpdated(testEvent, acknowledgment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process FHIR patient.updated event");

        verify(acknowledgment, never()).acknowledge();
    }

    // ── consumeFhirPatientLinked ──────────────────────────────────────────────

    @Test
    void consumeFhirPatientLinked_publishesEventAndAcknowledges() {
        FhirPatientEvent linkedEvent = FhirPatientEvent.builder()
                .fhirResourceId("Patient/source-id")
                .tenantId("test-tenant")
                .linkedFhirResourceId("Patient/target-id")
                .linkType("replaced-by")
                .build();

        listener.consumeFhirPatientLinked(linkedEvent, acknowledgment);

        verify(patientEventPublisher).publishPatientLinked(linkedEvent);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeFhirPatientLinked_whenPublisherThrows_rethrowsAndDoesNotAcknowledge() {
        doThrow(new RuntimeException("Publisher unavailable"))
                .when(patientEventPublisher).publishPatientLinked(testEvent);

        assertThatThrownBy(() ->
                listener.consumeFhirPatientLinked(testEvent, acknowledgment))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to process FHIR patient.linked event");

        verify(acknowledgment, never()).acknowledge();
    }
}
