package com.healthdata.eventsourcing.command.condition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiagnoseConditionCommandHandler Tests")
class DiagnoseConditionCommandHandlerTest {
    private final String TENANT_ID = "tenant-123";
    private final String PATIENT_ID = "patient-456";

    @Mock
    private ConditionIncompatibilityMatrix incompatibilityMatrix;

    @Mock
    private ConditionEventEmitter eventEmitter;

    private DiagnoseConditionCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new DiagnoseConditionCommandHandler(incompatibilityMatrix, eventEmitter);
    }

    @Test
    @DisplayName("Should diagnose Type 2 Diabetes")
    void shouldDiagnoseType2Diabetes() {
        DiagnoseConditionCommand command = DiagnoseConditionCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .icdCode("E11.9")
            .onsetDate(LocalDate.of(2024, 1, 1))
            .clinicalStatus("active")
            .verificationStatus("confirmed")
            .build();

        when(eventEmitter.emitConditionDiagnosed(any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectNext("patient-tenant-123-patient-456")
            .expectComplete()
            .verify(Duration.ofSeconds(5));

        verify(eventEmitter).emitConditionDiagnosed(any(ConditionDiagnosedEvent.class));
    }

    @Test
    @DisplayName("Should diagnose multiple compatible conditions")
    void shouldDiagnoseMultipleConditions() {
        DiagnoseConditionCommand diabetesCmd = DiagnoseConditionCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .icdCode("E11.9")
            .onsetDate(LocalDate.of(2024, 1, 1))
            .clinicalStatus("active")
            .verificationStatus("confirmed")
            .build();

        DiagnoseConditionCommand hypertensionCmd = DiagnoseConditionCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .icdCode("I10")
            .onsetDate(LocalDate.of(2023, 6, 1))
            .clinicalStatus("active")
            .verificationStatus("confirmed")
            .build();

        when(eventEmitter.emitConditionDiagnosed(any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(diabetesCmd, TENANT_ID))
            .expectNextCount(1)
            .expectComplete()
            .verify();

        StepVerifier.create(handler.handle(hypertensionCmd, TENANT_ID))
            .expectNextCount(1)
            .expectComplete()
            .verify();

        verify(eventEmitter, times(2)).emitConditionDiagnosed(any());
    }

    @Test
    @DisplayName("Should handle emitter errors")
    void shouldHandleEmitterErrors() {
        DiagnoseConditionCommand command = DiagnoseConditionCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .icdCode("E11.9")
            .onsetDate(LocalDate.of(2024, 1, 1))
            .clinicalStatus("active")
            .verificationStatus("confirmed")
            .build();

        when(eventEmitter.emitConditionDiagnosed(any()))
            .thenReturn(Mono.error(new RuntimeException("Kafka error")));

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectError(RuntimeException.class)
            .verify();
    }

    @Test
    @DisplayName("Should diagnose Type 1 Diabetes")
    void shouldDiagnoseType1Diabetes() {
        DiagnoseConditionCommand command = DiagnoseConditionCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .icdCode("E10.9")
            .onsetDate(LocalDate.of(2015, 3, 15))
            .clinicalStatus("active")
            .verificationStatus("confirmed")
            .build();

        when(eventEmitter.emitConditionDiagnosed(any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectNextCount(1)
            .expectComplete()
            .verify();
    }
}
