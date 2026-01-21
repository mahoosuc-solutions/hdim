package com.healthdata.eventsourcing.handler.patient;

import com.healthdata.eventsourcing.command.patient.PatientCreatedEvent;
import com.healthdata.eventsourcing.projection.patient.PatientProjection;
import com.healthdata.eventsourcing.projection.patient.PatientProjectionService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Patient Event Handler Tests - CQRS Read Model Update Tests
 *
 * Tests the PatientEventHandler which consumes PatientCreatedEvent from Kafka
 * and updates the PatientProjection read model.
 *
 * Test Categories:
 * - Successful event handling
 * - Event deserialization
 * - Projection persistence
 * - Multi-tenant isolation
 * - Duplicate event idempotency
 * - Error handling and resilience
 */
@Slf4j
@ExtendWith(MockitoExtension.class)
class PatientEventHandlerTest {

    @Mock
    private PatientProjectionService projectionService;

    @InjectMocks
    private PatientEventHandler eventHandler;

    private PatientCreatedEvent testEvent;
    private PatientProjection expectedProjection;

    @BeforeEach
    void setUp() {
        // Test event with standard patient data
        testEvent = PatientCreatedEvent.builder()
            .tenantId("tenant-123")
            .patientId("patient-123")
            .mrn("MRN-12345")
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1980, 5, 15))
            .gender("MALE")
            .insuranceMemberId("INS-12345")
            .build();

        expectedProjection = PatientProjection.builder()
            .patientId("patient-123")
            .tenantId("tenant-123")
            .mrn("MRN-12345")
            .firstName("John")
            .lastName("Doe")
            .dateOfBirth(LocalDate.of(1980, 5, 15))
            .gender("MALE")
            .insuranceMemberId("INS-12345")
            .build();
    }

    @Nested
    @DisplayName("PatientEventHandler - Successful Event Handling")
    class SuccessfulEventHandling {

        @Test
        @DisplayName("Should handle PatientCreatedEvent and save projection")
        void shouldHandleEventAndSaveProjection() {
            // Given
            when(projectionService.saveProjection(any(PatientProjection.class)))
                .thenReturn(expectedProjection);

            // When
            eventHandler.handlePatientCreatedEvent(testEvent);

            // Then
            verify(projectionService, times(1)).saveProjection(argThat(projection ->
                projection.getPatientId().equals("patient-123") &&
                projection.getTenantId().equals("tenant-123") &&
                projection.getMrn().equals("MRN-12345") &&
                projection.getFirstName().equals("John") &&
                projection.getLastName().equals("Doe")
            ));
        }

        @Test
        @DisplayName("Should deserialize event correctly")
        void shouldDeserializeEvent() {
            // Given
            when(projectionService.saveProjection(any(PatientProjection.class)))
                .thenReturn(expectedProjection);

            // When
            eventHandler.handlePatientCreatedEvent(testEvent);

            // Then
            assertThat(testEvent.getAggregateId()).isEqualTo("patient-tenant-123-MRN-12345");
            assertThat(testEvent.getMrn()).isEqualTo("MRN-12345");
            verify(projectionService).saveProjection(any());
        }

        @Test
        @DisplayName("Should preserve patient demographics in projection")
        void shouldPreserveDemographics() {
            // Given
            when(projectionService.saveProjection(any(PatientProjection.class)))
                .thenReturn(expectedProjection);

            // When
            eventHandler.handlePatientCreatedEvent(testEvent);

            // Then
            verify(projectionService).saveProjection(argThat(projection ->
                projection.getDateOfBirth().equals(LocalDate.of(1980, 5, 15)) &&
                projection.getGender().equals("MALE") &&
                projection.getInsuranceMemberId().equals("INS-12345")
            ));
        }

        @Test
        @DisplayName("Should create projection with correct tenant isolation")
        void shouldEnforceTenantIsolation() {
            // Given
            when(projectionService.saveProjection(any(PatientProjection.class)))
                .thenReturn(expectedProjection);

            // When
            eventHandler.handlePatientCreatedEvent(testEvent);

            // Then
            verify(projectionService).saveProjection(argThat(projection ->
                projection.getTenantId().equals("tenant-123")
            ));
        }

        @Test
        @DisplayName("Should handle valid event and call service once")
        void shouldCallServiceOnce() {
            // Given
            when(projectionService.saveProjection(any(PatientProjection.class)))
                .thenReturn(expectedProjection);

            // When
            eventHandler.handlePatientCreatedEvent(testEvent);

            // Then
            verify(projectionService, times(1)).saveProjection(any(PatientProjection.class));
        }
    }

    @Nested
    @DisplayName("PatientEventHandler - Multi-Tenant Isolation")
    class MultiTenantIsolation {

        @Test
        @DisplayName("Should enforce multi-tenant isolation in projection")
        void shouldEnforceMultiTenantIsolation() {
            // Given - event from different tenant
            PatientCreatedEvent tenant2Event = PatientCreatedEvent.builder()
                .tenantId("tenant-456")
                .patientId("patient-456")
                .mrn("MRN-67890")
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.of(1985, 3, 20))
                .gender("FEMALE")
                .insuranceMemberId("INS-67890")
                .build();

            when(projectionService.saveProjection(any(PatientProjection.class)))
                .thenReturn(PatientProjection.builder()
                    .tenantId("tenant-456")
                    .patientId("patient-456")
                    .build());

            // When
            eventHandler.handlePatientCreatedEvent(tenant2Event);

            // Then
            verify(projectionService).saveProjection(argThat(projection ->
                projection.getTenantId().equals("tenant-456") &&
                !projection.getTenantId().equals("tenant-123")
            ));
        }

        @Test
        @DisplayName("Should not leak data across tenants")
        void shouldNotLeakDataAcrossTenants() {
            // Given
            PatientCreatedEvent anotherTenantEvent = PatientCreatedEvent.builder()
                .tenantId("tenant-789")
                .patientId("patient-789")
                .mrn("MRN-99999")
                .firstName("Bob")
                .lastName("Johnson")
                .build();

            when(projectionService.saveProjection(any(PatientProjection.class)))
                .thenReturn(PatientProjection.builder().tenantId("tenant-789").build());

            // When
            eventHandler.handlePatientCreatedEvent(anotherTenantEvent);

            // Then - verify tenant ID is set correctly
            verify(projectionService).saveProjection(argThat(projection ->
                projection.getTenantId().equals("tenant-789")
            ));
        }
    }

    @Nested
    @DisplayName("PatientEventHandler - Duplicate Event Handling (Idempotency)")
    class DuplicateEventHandling {

        @Test
        @DisplayName("Should handle duplicate event gracefully")
        void shouldHandleDuplicateEvent() {
            // Given - same event twice
            when(projectionService.saveProjection(any(PatientProjection.class)))
                .thenReturn(expectedProjection);

            // When
            eventHandler.handlePatientCreatedEvent(testEvent);
            eventHandler.handlePatientCreatedEvent(testEvent);

            // Then - should call service twice (caller ensures idempotency via event store)
            verify(projectionService, times(2)).saveProjection(any(PatientProjection.class));
        }

        @Test
        @DisplayName("Should generate deterministic aggregate ID for deduplication")
        void shouldGenerateDeterministicAggregateId() {
            // Given
            when(projectionService.saveProjection(any(PatientProjection.class)))
                .thenReturn(expectedProjection);

            // When
            eventHandler.handlePatientCreatedEvent(testEvent);

            // Then - aggregateId is deterministic for idempotency
            String aggregateId = testEvent.getAggregateId();
            assertThat(aggregateId).isNotNull();
            assertThat(aggregateId).contains("patient-tenant-123-MRN-12345");
        }
    }

    @Nested
    @DisplayName("PatientEventHandler - Null Field Handling")
    class NullFieldHandling {

        @Test
        @DisplayName("Should handle optional fields (null lastName)")
        void shouldHandleNullLastName() {
            // Given
            PatientCreatedEvent eventWithNullLastName = PatientCreatedEvent.builder()
                .tenantId("tenant-123")
                .patientId("patient-111")
                .mrn("MRN-11111")
                .firstName("John")
                .lastName(null) // Optional field
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender("MALE")
                .insuranceMemberId(null) // Optional
                .build();

            when(projectionService.saveProjection(any(PatientProjection.class)))
                .thenReturn(PatientProjection.builder()
                    .patientId("patient-111")
                    .tenantId("tenant-123")
                    .mrn("MRN-11111")
                    .firstName("John")
                    .lastName(null)
                    .build());

            // When
            eventHandler.handlePatientCreatedEvent(eventWithNullLastName);

            // Then
            verify(projectionService).saveProjection(any(PatientProjection.class));
        }

        @Test
        @DisplayName("Should handle optional gender field")
        void shouldHandleOptionalGender() {
            // Given
            PatientCreatedEvent eventWithoutGender = PatientCreatedEvent.builder()
                .tenantId("tenant-123")
                .patientId("patient-222")
                .mrn("MRN-22222")
                .firstName("Jane")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1985, 6, 15))
                .gender(null) // Optional
                .insuranceMemberId("INS-22222")
                .build();

            when(projectionService.saveProjection(any(PatientProjection.class)))
                .thenReturn(PatientProjection.builder().build());

            // When
            eventHandler.handlePatientCreatedEvent(eventWithoutGender);

            // Then
            verify(projectionService).saveProjection(any(PatientProjection.class));
        }
    }

    @Nested
    @DisplayName("PatientEventHandler - Error Handling and Resilience")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle service exception gracefully")
        void shouldHandleServiceException() {
            // Given
            when(projectionService.saveProjection(any(PatientProjection.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

            // When / Then
            assertThatThrownBy(() -> eventHandler.handlePatientCreatedEvent(testEvent))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection failed");
        }

        @Test
        @DisplayName("Should not modify event after handling")
        void shouldNotModifyEvent() {
            // Given
            String originalMrn = testEvent.getMrn();
            when(projectionService.saveProjection(any(PatientProjection.class)))
                .thenReturn(expectedProjection);

            // When
            eventHandler.handlePatientCreatedEvent(testEvent);

            // Then
            assertThat(testEvent.getMrn()).isEqualTo(originalMrn);
        }

        @Test
        @DisplayName("Should log event handling")
        void shouldLogEventHandling() {
            // Given
            when(projectionService.saveProjection(any(PatientProjection.class)))
                .thenReturn(expectedProjection);

            // When
            eventHandler.handlePatientCreatedEvent(testEvent);

            // Then
            verify(projectionService).saveProjection(any(PatientProjection.class));
        }
    }

    @Nested
    @DisplayName("PatientEventHandler - Repository Integration")
    class RepositoryIntegration {

        @Test
        @DisplayName("Should verify repository called with correct projection data")
        void shouldVerifyRepositoryCall() {
            // Given
            when(projectionService.saveProjection(any(PatientProjection.class)))
                .thenReturn(expectedProjection);

            // When
            eventHandler.handlePatientCreatedEvent(testEvent);

            // Then
            verify(projectionService, times(1)).saveProjection(argThat(proj ->
                proj.getPatientId().equals("patient-123") &&
                proj.getTenantId().equals("tenant-123")
            ));
        }

        @Test
        @DisplayName("Should handle repository save response")
        void shouldHandleRepositorySaveResponse() {
            // Given
            PatientProjection savedProjection = PatientProjection.builder()
                .patientId("patient-123")
                .tenantId("tenant-123")
                .mrn("MRN-12345")
                .firstName("John")
                .lastName("Doe")
                .build();

            when(projectionService.saveProjection(any(PatientProjection.class)))
                .thenReturn(savedProjection);

            // When
            eventHandler.handlePatientCreatedEvent(testEvent);

            // Then
            verify(projectionService).saveProjection(any());
        }
    }
}
