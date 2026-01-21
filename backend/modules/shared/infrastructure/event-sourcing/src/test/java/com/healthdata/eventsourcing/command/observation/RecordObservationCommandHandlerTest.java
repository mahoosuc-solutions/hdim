package com.healthdata.eventsourcing.command.observation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for RecordObservationCommandHandler with LOINC validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecordObservationCommandHandler Tests")
class RecordObservationCommandHandlerTest {

    private final String TENANT_ID = "tenant-123";
    private final String PATIENT_ID = "patient-456";

    @Mock
    private ObservationValidator validator;

    @Mock
    private ObservationEventEmitter eventEmitter;

    private RecordObservationCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RecordObservationCommandHandler(validator, eventEmitter);
    }

    @Test
    @DisplayName("Should record temperature observation with valid value")
    void shouldRecordTemperature() {
        // Given
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("8310-5")
            .value(new BigDecimal("37.5"))
            .unit("°C")
            .observationDate(Instant.now().minusSeconds(300))
            .build();

        when(validator.validate(eq(command), eq(TENANT_ID)))
            .thenReturn(new ObservationValidator.ValidationResult(java.util.List.of()));
        when(eventEmitter.emitObservationRecorded(any()))
            .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectNext("patient-tenant-123-patient-456")
            .expectComplete()
            .verify(Duration.ofSeconds(5));

        verify(eventEmitter).emitObservationRecorded(any(ObservationRecordedEvent.class));
    }

    @Test
    @DisplayName("Should record heart rate observation")
    void shouldRecordHeartRate() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("8867-4")
            .value(new BigDecimal("72"))
            .unit("/min")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        when(validator.validate(any(), any()))
            .thenReturn(new ObservationValidator.ValidationResult(java.util.List.of()));
        when(eventEmitter.emitObservationRecorded(any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectNextCount(1)
            .expectComplete()
            .verify();
    }

    @Test
    @DisplayName("Should record glucose observation")
    void shouldRecordGlucose() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("2339-0")
            .value(new BigDecimal("105"))
            .unit("mg/dL")
            .observationDate(Instant.now().minusSeconds(120))
            .build();

        when(validator.validate(any(), any()))
            .thenReturn(new ObservationValidator.ValidationResult(java.util.List.of()));
        when(eventEmitter.emitObservationRecorded(any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectNextCount(1)
            .expectComplete()
            .verify();
    }

    @Test
    @DisplayName("Should fail when temperature below valid range")
    void shouldFailWhenTemperatureTooLow() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("8310-5")
            .value(new BigDecimal("34.0"))
            .unit("°C")
            .observationDate(Instant.now().minusSeconds(300))
            .build();

        when(validator.validate(eq(command), eq(TENANT_ID)))
            .thenReturn(new ObservationValidator.ValidationResult(
                java.util.List.of("Temperature value 34.0 is below minimum 35.0 °C")));

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectError(RecordObservationCommandHandler.ObservationRecordingException.class)
            .verify();

        verify(eventEmitter, never()).emitObservationRecorded(any());
    }

    @Test
    @DisplayName("Should fail when heart rate exceeds valid range")
    void shouldFailWhenHeartRateTooHigh() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("8867-4")
            .value(new BigDecimal("250"))
            .unit("/min")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        when(validator.validate(eq(command), eq(TENANT_ID)))
            .thenReturn(new ObservationValidator.ValidationResult(
                java.util.List.of("Heart Rate value 250 exceeds maximum 200 /min")));

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectError(RecordObservationCommandHandler.ObservationRecordingException.class)
            .verify();
    }

    @Test
    @DisplayName("Should fail when glucose below minimum")
    void shouldFailWhenGlucoseLow() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("2339-0")
            .value(new BigDecimal("30"))
            .unit("mg/dL")
            .observationDate(Instant.now().minusSeconds(120))
            .build();

        when(validator.validate(eq(command), eq(TENANT_ID)))
            .thenReturn(new ObservationValidator.ValidationResult(
                java.util.List.of("Glucose value 30 is below minimum 40 mg/dL")));

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectError(RecordObservationCommandHandler.ObservationRecordingException.class)
            .verify();
    }

    @Test
    @DisplayName("Should record systolic blood pressure")
    void shouldRecordSystolicBP() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("8480-6")
            .value(new BigDecimal("120"))
            .unit("mmHg")
            .observationDate(Instant.now().minusSeconds(300))
            .build();

        when(validator.validate(any(), any()))
            .thenReturn(new ObservationValidator.ValidationResult(java.util.List.of()));
        when(eventEmitter.emitObservationRecorded(any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectNextCount(1)
            .expectComplete()
            .verify();
    }

    @Test
    @DisplayName("Should record diastolic blood pressure")
    void shouldRecordDiastolicBP() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("8462-4")
            .value(new BigDecimal("80"))
            .unit("mmHg")
            .observationDate(Instant.now().minusSeconds(300))
            .build();

        when(validator.validate(any(), any()))
            .thenReturn(new ObservationValidator.ValidationResult(java.util.List.of()));
        when(eventEmitter.emitObservationRecorded(any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectNextCount(1)
            .expectComplete()
            .verify();
    }

    @Test
    @DisplayName("Should record respiratory rate")
    void shouldRecordRespiratoryRate() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("9279-1")
            .value(new BigDecimal("16"))
            .unit("/min")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        when(validator.validate(any(), any()))
            .thenReturn(new ObservationValidator.ValidationResult(java.util.List.of()));
        when(eventEmitter.emitObservationRecorded(any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectNextCount(1)
            .expectComplete()
            .verify();
    }

    @Test
    @DisplayName("Should record oxygen saturation")
    void shouldRecordOxygenSaturation() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("2708-6")
            .value(new BigDecimal("98"))
            .unit("%")
            .observationDate(Instant.now().minusSeconds(60))
            .build();

        when(validator.validate(any(), any()))
            .thenReturn(new ObservationValidator.ValidationResult(java.util.List.of()));
        when(eventEmitter.emitObservationRecorded(any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectNextCount(1)
            .expectComplete()
            .verify();
    }

    @Test
    @DisplayName("Should record weight")
    void shouldRecordWeight() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .loincCode("29463-7")
            .value(new BigDecimal("75.5"))
            .unit("kg")
            .observationDate(Instant.now().minusSeconds(300))
            .build();

        when(validator.validate(any(), any()))
            .thenReturn(new ObservationValidator.ValidationResult(java.util.List.of()));
        when(eventEmitter.emitObservationRecorded(any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectNextCount(1)
            .expectComplete()
            .verify();
    }

    @Test
    @DisplayName("Should fail when validation returns errors")
    void shouldFailWhenValidationFails() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId(TENANT_ID)
            .patientId("")  // Invalid
            .loincCode("8310-5")
            .value(new BigDecimal("37.5"))
            .unit("°C")
            .observationDate(Instant.now().minusSeconds(300))
            .build();

        when(validator.validate(eq(command), eq(TENANT_ID)))
            .thenReturn(new ObservationValidator.ValidationResult(
                java.util.List.of("Patient ID is required")));

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectError(RecordObservationCommandHandler.ObservationRecordingException.class)
            .verify();

        verify(eventEmitter, never()).emitObservationRecorded(any());
    }

    @Test
    @DisplayName("Should enforce tenant isolation")
    void shouldEnforceTenantIsolation() {
        RecordObservationCommand command = RecordObservationCommand.builder()
            .tenantId("tenant-999")
            .patientId(PATIENT_ID)
            .loincCode("8310-5")
            .value(new BigDecimal("37.5"))
            .unit("°C")
            .observationDate(Instant.now().minusSeconds(300))
            .build();

        when(validator.validate(eq(command), eq(TENANT_ID)))
            .thenReturn(new ObservationValidator.ValidationResult(
                java.util.List.of("Tenant ID mismatch")));

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectError(RecordObservationCommandHandler.ObservationRecordingException.class)
            .verify();
    }
}
