package com.healthdata.eventsourcing.command.patient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for CreatePatientCommandHandler
 *
 * Uses reactor-test StepVerifier for async validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePatientCommandHandler Tests")
class CreatePatientCommandHandlerTest {

    private final String TENANT_ID = "tenant-123";
    private final String FIRST_NAME = "John";
    private final String LAST_NAME = "Doe";
    private final LocalDate DOB = LocalDate.of(1990, 1, 15);
    private final String MRN = "MRN-12345";
    private final String INSURANCE_ID = "INS-98765";

    @Mock
    private CreatePatientValidator validator;

    @Mock
    private PatientEventEmitter eventEmitter;

    private CreatePatientCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreatePatientCommandHandler(validator, eventEmitter);
    }

    // ============ SUCCESS SCENARIO TESTS ============

    @Nested
    @DisplayName("Successful Patient Creation")
    class SuccessfulCreationTests {

        @Test
        @DisplayName("Should create patient with valid command")
        void shouldCreatePatientWithValidCommand() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .dateOfBirth(DOB)
                .gender("MALE")
                .mrn(MRN)
                .insuranceMemberId(INSURANCE_ID)
                .build();

            when(validator.validate(eq(command), eq(TENANT_ID)))
                .thenReturn(new CreatePatientValidator.ValidationResult(java.util.List.of()));

            when(eventEmitter.emitPatientCreated(any()))
                .thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(handler.handle(command, TENANT_ID))
                .expectNext("patient-tenant-123-MRN-12345")
                .expectComplete()
                .verify(Duration.ofSeconds(5));

            verify(validator).validate(eq(command), eq(TENANT_ID));
            verify(eventEmitter).emitPatientCreated(any(PatientCreatedEvent.class));
        }

        @Test
        @DisplayName("Should generate deterministic aggregate ID")
        void shouldGenerateDeterministicAggregateId() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .dateOfBirth(DOB)
                .mrn(MRN)
                .build();

            when(validator.validate(any(), any()))
                .thenReturn(new CreatePatientValidator.ValidationResult(java.util.List.of()));
            when(eventEmitter.emitPatientCreated(any()))
                .thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(handler.handle(command, TENANT_ID))
                .assertNext(aggregateId -> assertThat(aggregateId)
                    .startsWith("patient-")
                    .contains(TENANT_ID)
                    .contains(MRN))
                .expectComplete()
                .verify();
        }

        @Test
        @DisplayName("Should emit PatientCreatedEvent with correct data")
        void shouldEmitEventWithCorrectData() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .dateOfBirth(DOB)
                .gender("FEMALE")
                .mrn(MRN)
                .insuranceMemberId(INSURANCE_ID)
                .build();

            when(validator.validate(any(), any()))
                .thenReturn(new CreatePatientValidator.ValidationResult(java.util.List.of()));
            when(eventEmitter.emitPatientCreated(any()))
                .thenReturn(Mono.empty());

            // When
            StepVerifier.create(handler.handle(command, TENANT_ID))
                .expectNextCount(1)
                .expectComplete()
                .verify();

            // Then
            verify(eventEmitter).emitPatientCreated(argThat(event ->
                event.getTenantId().equals(TENANT_ID) &&
                event.getFirstName().equals(FIRST_NAME) &&
                event.getLastName().equals(LAST_NAME) &&
                event.getDateOfBirth().equals(DOB) &&
                event.getGender().equals("FEMALE") &&
                event.getMrn().equals(MRN) &&
                event.getInsuranceMemberId().equals(INSURANCE_ID)
            ));
        }

        @Test
        @DisplayName("Should mark event as HIPAA compliant")
        void shouldMarkEventAsHipaaCompliant() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .dateOfBirth(DOB)
                .mrn(MRN)
                .build();

            when(validator.validate(any(), any()))
                .thenReturn(new CreatePatientValidator.ValidationResult(java.util.List.of()));
            when(eventEmitter.emitPatientCreated(any()))
                .thenReturn(Mono.empty());

            // When
            StepVerifier.create(handler.handle(command, TENANT_ID))
                .expectNextCount(1)
                .expectComplete()
                .verify();

            // Then
            verify(eventEmitter).emitPatientCreated(argThat(event ->
                event.isHipaaCompliant() &&
                event.getSensitivityLevel().equals("SENSITIVE")
            ));
        }
    }

    // ============ VALIDATION FAILURE TESTS ============

    @Nested
    @DisplayName("Validation Failures")
    class ValidationFailureTests {

        @Test
        @DisplayName("Should fail when validation returns errors")
        void shouldFailWhenValidationErrors() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("")  // Invalid
                .lastName(LAST_NAME)
                .dateOfBirth(DOB)
                .mrn(MRN)
                .build();

            when(validator.validate(eq(command), eq(TENANT_ID)))
                .thenReturn(new CreatePatientValidator.ValidationResult(
                    java.util.List.of("First name is required")));

            // When & Then
            StepVerifier.create(handler.handle(command, TENANT_ID))
                .expectError(CreatePatientCommandHandler.PatientCreationException.class)
                .verify();

            verify(eventEmitter, never()).emitPatientCreated(any());
        }

        @Test
        @DisplayName("Should propagate validation error message")
        void shouldPropagateValidationErrorMessage() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .dateOfBirth(DOB)
                .mrn("")  // Invalid
                .build();

            String expectedError = "MRN is required";
            when(validator.validate(any(), any()))
                .thenReturn(new CreatePatientValidator.ValidationResult(
                    java.util.List.of(expectedError)));

            // When & Then
            StepVerifier.create(handler.handle(command, TENANT_ID))
                .expectErrorMatches(error -> error.getMessage() != null && error.getMessage().contains(expectedError))
                .verify();
        }

        @Test
        @DisplayName("Should collect multiple validation errors")
        void shouldCollectMultipleValidationErrors() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("")
                .lastName("")
                .dateOfBirth(null)
                .mrn(MRN)
                .build();

            java.util.List<String> errors = java.util.List.of(
                "First name is required",
                "Last name is required",
                "Date of birth is required"
            );

            when(validator.validate(any(), any()))
                .thenReturn(new CreatePatientValidator.ValidationResult(errors));

            // When & Then
            StepVerifier.create(handler.handle(command, TENANT_ID))
                .expectError(CreatePatientCommandHandler.PatientCreationException.class)
                .verify();
        }
    }

    // ============ TENANT ISOLATION TESTS ============

    @Nested
    @DisplayName("Multi-Tenant Isolation")
    class TenantIsolationTests {

        @Test
        @DisplayName("Should validate tenant context")
        void shouldValidateTenantContext() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId("tenant-123")
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .dateOfBirth(DOB)
                .mrn(MRN)
                .build();

            when(validator.validate(eq(command), eq("tenant-456")))
                .thenReturn(new CreatePatientValidator.ValidationResult(
                    java.util.List.of("Tenant ID mismatch")));

            // When & Then
            StepVerifier.create(handler.handle(command, "tenant-456"))
                .expectError(CreatePatientCommandHandler.PatientCreationException.class)
                .verify();
        }

        @Test
        @DisplayName("Should isolate patient creation per tenant")
        void shouldIsolatePatientPerTenant() {
            // Given
            String tenant1 = "tenant-1";
            String tenant2 = "tenant-2";

            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(tenant1)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .dateOfBirth(DOB)
                .mrn(MRN)
                .build();

            when(validator.validate(any(), eq(tenant1)))
                .thenReturn(new CreatePatientValidator.ValidationResult(java.util.List.of()));
            when(eventEmitter.emitPatientCreated(any()))
                .thenReturn(Mono.empty());

            // When
            StepVerifier.create(handler.handle(command, tenant1))
                .expectNextCount(1)
                .expectComplete()
                .verify();

            // Then - verify tenant is in aggregate ID
            verify(eventEmitter).emitPatientCreated(argThat(event ->
                event.getAggregateId().contains(tenant1)
            ));
        }
    }

    // ============ EVENT EMISSION ERROR TESTS ============

    @Nested
    @DisplayName("Event Emission Errors")
    class EventEmissionErrorTests {

        @Test
        @DisplayName("Should propagate event emission errors")
        void shouldPropagateEventEmissionErrors() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .dateOfBirth(DOB)
                .mrn(MRN)
                .build();

            when(validator.validate(any(), any()))
                .thenReturn(new CreatePatientValidator.ValidationResult(java.util.List.of()));
            when(eventEmitter.emitPatientCreated(any()))
                .thenReturn(Mono.error(new RuntimeException("Kafka unavailable")));

            // When & Then
            StepVerifier.create(handler.handle(command, TENANT_ID))
                .expectError(RuntimeException.class)
                .verify();
        }

        @Test
        @DisplayName("Should timeout if event emission takes too long")
        void shouldTimeoutIfEmissionSlow() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .dateOfBirth(DOB)
                .mrn(MRN)
                .build();

            when(validator.validate(any(), any()))
                .thenReturn(new CreatePatientValidator.ValidationResult(java.util.List.of()));
            when(eventEmitter.emitPatientCreated(any()))
                .thenReturn(Mono.delay(Duration.ofSeconds(10)).then());

            // When & Then
            StepVerifier.create(handler.handle(command, TENANT_ID))
                .expectTimeout(Duration.ofSeconds(5))
                .verify();
        }
    }

    // ============ IDEMPOTENCY TESTS ============

    @Nested
    @DisplayName("Idempotency")
    class IdempotencyTests {

        @Test
        @DisplayName("Should generate same aggregate ID for same MRN")
        void shouldGenerateSameAggregateIdForSameMrn() {
            // Given
            CreatePatientCommand command1 = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .dateOfBirth(DOB)
                .mrn(MRN)
                .build();

            CreatePatientCommand command2 = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName("Jane")  // Different first name
                .lastName("Smith")  // Different last name
                .dateOfBirth(LocalDate.of(1995, 5, 20))
                .mrn(MRN)  // Same MRN
                .build();

            when(validator.validate(any(), any()))
                .thenReturn(new CreatePatientValidator.ValidationResult(java.util.List.of()));
            when(eventEmitter.emitPatientCreated(any()))
                .thenReturn(Mono.empty());

            // When
            String id1 = handler.handle(command1, TENANT_ID).block();
            String id2 = handler.handle(command2, TENANT_ID).block();

            // Then - aggregate IDs should be identical (idempotent)
            assertThat(id1).isEqualTo(id2).isEqualTo("patient-tenant-123-MRN-12345");
        }

        @Test
        @DisplayName("Should allow safe retries with same command")
        void shouldAllowSafeRetries() {
            // Given
            CreatePatientCommand command = CreatePatientCommand.builder()
                .tenantId(TENANT_ID)
                .firstName(FIRST_NAME)
                .lastName(LAST_NAME)
                .dateOfBirth(DOB)
                .mrn(MRN)
                .build();

            when(validator.validate(any(), any()))
                .thenReturn(new CreatePatientValidator.ValidationResult(java.util.List.of()));
            when(eventEmitter.emitPatientCreated(any()))
                .thenReturn(Mono.empty());

            // When - execute same command 3 times
            for (int i = 0; i < 3; i++) {
                StepVerifier.create(handler.handle(command, TENANT_ID))
                    .expectNext("patient-tenant-123-MRN-12345")
                    .expectComplete()
                    .verify();
            }

            // Then - event emitter should be called 3 times (idempotency at event level)
            verify(eventEmitter, times(3)).emitPatientCreated(any());
        }
    }
}
