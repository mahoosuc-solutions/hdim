package com.healthdata.eventsourcing.command.careplan;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateCarePlanCommandHandler Tests")
class CreateCarePlanCommandHandlerTest {
    private final String TENANT_ID = "tenant-123";
    private final String PATIENT_ID = "patient-456";

    @Mock
    private CarePlanEventEmitter eventEmitter;

    private CreateCarePlanCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateCarePlanCommandHandler(eventEmitter);
    }

    @Test
    @DisplayName("Should create diabetes care plan")
    void shouldCreateDiabetesCarePlan() {
        CreateCarePlanCommand command = CreateCarePlanCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .carePlanTitle("Diabetes Management")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .careCoordinatorId("coordinator-789")
            .targetConditions(List.of("E11.9"))
            .goals(List.of("Maintain HbA1c < 7%", "Daily glucose monitoring"))
            .build();

        when(eventEmitter.emitCarePlanCreated(any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectNext("patient-tenant-123-patient-456")
            .expectComplete()
            .verify(Duration.ofSeconds(5));

        verify(eventEmitter).emitCarePlanCreated(any(CarePlanCreatedEvent.class));
    }

    @Test
    @DisplayName("Should create hypertension care plan")
    void shouldCreateHypertensionCarePlan() {
        CreateCarePlanCommand command = CreateCarePlanCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .carePlanTitle("Hypertension Management")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .careCoordinatorId("coordinator-789")
            .targetConditions(List.of("I10"))
            .goals(List.of("Maintain BP < 130/80", "Medication compliance"))
            .build();

        when(eventEmitter.emitCarePlanCreated(any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectNextCount(1)
            .expectComplete()
            .verify();
    }

    @Test
    @DisplayName("Should create care plan with multiple target conditions")
    void shouldCreateCarePlanWithMultipleConditions() {
        CreateCarePlanCommand command = CreateCarePlanCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .carePlanTitle("Chronic Disease Management")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .careCoordinatorId("coordinator-789")
            .targetConditions(List.of("E11.9", "I10", "J44.0"))
            .goals(List.of("Manage multiple chronic conditions", "Improve quality of life"))
            .build();

        when(eventEmitter.emitCarePlanCreated(any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectNextCount(1)
            .expectComplete()
            .verify();
    }

    @Test
    @DisplayName("Should create care plan with multiple goals")
    void shouldCreateCarePlanWithMultipleGoals() {
        CreateCarePlanCommand command = CreateCarePlanCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .carePlanTitle("Comprehensive Care Plan")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .careCoordinatorId("coordinator-789")
            .targetConditions(List.of("E11.9"))
            .goals(List.of("Goal 1", "Goal 2", "Goal 3", "Goal 4"))
            .build();

        when(eventEmitter.emitCarePlanCreated(any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectNextCount(1)
            .expectComplete()
            .verify();
    }

    @Test
    @DisplayName("Should handle emitter errors")
    void shouldHandleEmitterErrors() {
        CreateCarePlanCommand command = CreateCarePlanCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .carePlanTitle("Test Plan")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .careCoordinatorId("coordinator-789")
            .targetConditions(List.of("I10"))
            .goals(List.of("Goal"))
            .build();

        when(eventEmitter.emitCarePlanCreated(any()))
            .thenReturn(Mono.error(new RuntimeException("Kafka error")));

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectError(RuntimeException.class)
            .verify();
    }

    @Test
    @DisplayName("Should emit event with all care plan details")
    void shouldEmitEventWithFullDetails() {
        CreateCarePlanCommand command = CreateCarePlanCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .carePlanTitle("Complete Care Plan")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .careCoordinatorId("coordinator-789")
            .targetConditions(List.of("E11.9", "I10"))
            .goals(List.of("Goal A", "Goal B"))
            .build();

        when(eventEmitter.emitCarePlanCreated(any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectNextCount(1)
            .expectComplete()
            .verify();

        verify(eventEmitter).emitCarePlanCreated(argThat(event ->
            event.getTenantId().equals(TENANT_ID) &&
            event.getPatientId().equals(PATIENT_ID) &&
            event.getCarePlanTitle().equals("Complete Care Plan") &&
            event.getTargetConditions().size() == 2 &&
            event.getGoals().size() == 2
        ));
    }

    @Test
    @DisplayName("Should generate correct aggregate ID")
    void shouldGenerateCorrectAggregateId() {
        CreateCarePlanCommand command = CreateCarePlanCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .carePlanTitle("Test")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .careCoordinatorId("coordinator-789")
            .targetConditions(List.of())
            .goals(List.of())
            .build();

        when(eventEmitter.emitCarePlanCreated(any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .assertNext(aggregateId ->
                assertThat(aggregateId)
                    .isEqualTo("patient-tenant-123-patient-456")
                    .contains(TENANT_ID)
                    .contains(PATIENT_ID)
            )
            .expectComplete()
            .verify();
    }

    @Test
    @DisplayName("Should create care plan with future dates")
    void shouldCreateCarePlanWithFutureDates() {
        LocalDate futureStart = LocalDate.now().plusMonths(1);
        LocalDate futureEnd = futureStart.plusMonths(12);

        CreateCarePlanCommand command = CreateCarePlanCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .carePlanTitle("Future Care Plan")
            .startDate(futureStart)
            .endDate(futureEnd)
            .careCoordinatorId("coordinator-789")
            .targetConditions(List.of("I10"))
            .goals(List.of("Goal"))
            .build();

        when(eventEmitter.emitCarePlanCreated(any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectNextCount(1)
            .expectComplete()
            .verify();
    }

    @Test
    @DisplayName("Should handle empty goals list")
    void shouldHandleEmptyGoalsList() {
        CreateCarePlanCommand command = CreateCarePlanCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .carePlanTitle("Plan without goals")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .careCoordinatorId("coordinator-789")
            .targetConditions(List.of("I10"))
            .goals(List.of())
            .build();

        when(eventEmitter.emitCarePlanCreated(any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectNextCount(1)
            .expectComplete()
            .verify();
    }

    @Test
    @DisplayName("Should handle empty target conditions list")
    void shouldHandleEmptyConditionsList() {
        CreateCarePlanCommand command = CreateCarePlanCommand.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .carePlanTitle("Plan without conditions")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .careCoordinatorId("coordinator-789")
            .targetConditions(List.of())
            .goals(List.of("Goal"))
            .build();

        when(eventEmitter.emitCarePlanCreated(any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(command, TENANT_ID))
            .expectNextCount(1)
            .expectComplete()
            .verify();
    }
}
