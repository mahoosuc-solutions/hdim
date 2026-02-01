package com.healthdata.eventsourcing.command.careplan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CarePlanCreatedEvent Tests")
class CarePlanEventTest {
    private final String TENANT_ID = "tenant-123";
    private final String PATIENT_ID = "patient-456";

    @Test
    @DisplayName("Should create event with all fields")
    void shouldCreateEventWithAllFields() {
        CarePlanCreatedEvent event = CarePlanCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .carePlanTitle("Test Plan")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .careCoordinatorId("coordinator-789")
            .targetConditions(List.of("I10", "E11.9"))
            .goals(List.of("Goal 1", "Goal 2"))
            .build();

        assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(event.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(event.getCarePlanTitle()).isEqualTo("Test Plan");
        assertThat(event.getTargetConditions()).hasSize(2);
        assertThat(event.getGoals()).hasSize(2);
    }

    @Test
    @DisplayName("Should return correct event type")
    void shouldReturnCorrectEventType() {
        CarePlanCreatedEvent event = CarePlanCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .carePlanTitle("Test")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .careCoordinatorId("coordinator-789")
            .targetConditions(List.of())
            .goals(List.of())
            .build();

        assertThat(event.getEventType()).isEqualTo("CarePlanCreated");
    }

    @Test
    @DisplayName("Should return correct resource type")
    void shouldReturnCorrectResourceType() {
        CarePlanCreatedEvent event = CarePlanCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .carePlanTitle("Test")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .careCoordinatorId("coordinator-789")
            .targetConditions(List.of())
            .goals(List.of())
            .build();

        assertThat(event.getResourceType()).isEqualTo("CarePlan");
    }

    @Test
    @DisplayName("Should generate correct aggregate ID")
    void shouldGenerateCorrectAggregateId() {
        CarePlanCreatedEvent event = CarePlanCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .carePlanTitle("Test")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .careCoordinatorId("coordinator-789")
            .targetConditions(List.of())
            .goals(List.of())
            .build();

        assertThat(event.getAggregateId())
            .isEqualTo("patient-tenant-123-patient-456");
    }

    @Test
    @DisplayName("Should handle empty goal and condition lists")
    void shouldHandleEmptyLists() {
        CarePlanCreatedEvent event = CarePlanCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .carePlanTitle("Empty Plan")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .careCoordinatorId("coordinator-789")
            .targetConditions(List.of())
            .goals(List.of())
            .build();

        assertThat(event.getTargetConditions()).isEmpty();
        assertThat(event.getGoals()).isEmpty();
    }

    @Test
    @DisplayName("Should create event without builder")
    void shouldCreateEventWithoutBuilder() {
        CarePlanCreatedEvent event = new CarePlanCreatedEvent();
        assertThat(event).isNotNull();
    }

    @Test
    @DisplayName("Should set all required fields")
    void shouldSetAllRequiredFields() {
        CarePlanCreatedEvent event = new CarePlanCreatedEvent(
            TENANT_ID,
            PATIENT_ID,
            "Plan Title",
            LocalDate.of(2024, 1, 1),
            LocalDate.of(2024, 12, 31),
            "coordinator-789",
            List.of("I10"),
            List.of("Goal")
        );

        assertThat(event.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(event.getPatientId()).isEqualTo(PATIENT_ID);
        assertThat(event.getCarePlanTitle()).isEqualTo("Plan Title");
    }

    @Test
    @DisplayName("Should handle event creation with null coordinator")
    void shouldHandleNullCoordinator() {
        CarePlanCreatedEvent event = CarePlanCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .carePlanTitle("Test")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .careCoordinatorId(null)
            .targetConditions(List.of())
            .goals(List.of())
            .build();

        assertThat(event.getCareCoordinatorId()).isNull();
    }

    @Test
    @DisplayName("Should preserve date ranges")
    void shouldPreserveDateRanges() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 12, 31);

        CarePlanCreatedEvent event = CarePlanCreatedEvent.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .carePlanTitle("Test")
            .startDate(start)
            .endDate(end)
            .careCoordinatorId("coordinator-789")
            .targetConditions(List.of())
            .goals(List.of())
            .build();

        assertThat(event.getStartDate()).isEqualTo(start);
        assertThat(event.getEndDate()).isEqualTo(end);
    }
}
