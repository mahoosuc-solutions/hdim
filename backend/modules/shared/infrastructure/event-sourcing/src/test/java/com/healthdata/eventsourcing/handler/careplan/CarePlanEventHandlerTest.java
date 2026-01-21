package com.healthdata.eventsourcing.handler.careplan;

import com.healthdata.eventsourcing.command.careplan.CarePlanCreatedEvent;
import com.healthdata.eventsourcing.projection.careplan.CarePlanProjection;
import com.healthdata.eventsourcing.projection.careplan.CarePlanProjectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarePlanEventHandlerTest {

    @Mock
    private CarePlanProjectionService projectionService;

    @InjectMocks
    private CarePlanEventHandler eventHandler;

    private CarePlanCreatedEvent testEvent;

    @BeforeEach
    void setUp() {
        testEvent = CarePlanCreatedEvent.builder()
            .tenantId("tenant-123")
            .patientId("patient-123")
            .carePlanTitle("Diabetes Management Plan")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .careCoordinatorId("coordinator-456")
            .targetConditions(Arrays.asList("E11.9", "I10"))
            .goals(Arrays.asList("Achieve HbA1c < 7", "Reduce hypertension"))
            .build();
    }

    @Test
    @DisplayName("Should handle CarePlanCreatedEvent and save projection")
    void shouldHandleEventAndSaveProjection() {
        when(projectionService.saveProjection(any(CarePlanProjection.class)))
            .thenReturn(CarePlanProjection.builder().build());

        eventHandler.handleCarePlanCreatedEvent(testEvent);

        verify(projectionService, times(1)).saveProjection(any());
    }

    @Test
    @DisplayName("Should preserve patient and tenant")
    void shouldPreservePatientAndTenant() {
        when(projectionService.saveProjection(any(CarePlanProjection.class)))
            .thenReturn(CarePlanProjection.builder().build());

        eventHandler.handleCarePlanCreatedEvent(testEvent);

        verify(projectionService).saveProjection(argThat(proj ->
            proj.getPatientId().equals("patient-123") &&
            proj.getTenantId().equals("tenant-123")
        ));
    }

    @Test
    @DisplayName("Should preserve care plan title")
    void shouldPreserveCarePlanTitle() {
        when(projectionService.saveProjection(any(CarePlanProjection.class)))
            .thenReturn(CarePlanProjection.builder().build());

        eventHandler.handleCarePlanCreatedEvent(testEvent);

        verify(projectionService).saveProjection(argThat(proj ->
            proj.getTitle().equals("Diabetes Management Plan")
        ));
    }

    @Test
    @DisplayName("Should preserve start and end dates")
    void shouldPreserveStartAndEndDates() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);

        when(projectionService.saveProjection(any(CarePlanProjection.class)))
            .thenReturn(CarePlanProjection.builder().build());

        eventHandler.handleCarePlanCreatedEvent(testEvent);

        verify(projectionService).saveProjection(argThat(proj ->
            proj.getStartDate().equals(startDate) &&
            proj.getEndDate().equals(endDate)
        ));
    }

    @Test
    @DisplayName("Should preserve care coordinator")
    void shouldPreserveCareCoordinator() {
        when(projectionService.saveProjection(any(CarePlanProjection.class)))
            .thenReturn(CarePlanProjection.builder().build());

        eventHandler.handleCarePlanCreatedEvent(testEvent);

        verify(projectionService).saveProjection(argThat(proj ->
            proj.getCoordinatorId().equals("coordinator-456")
        ));
    }

    @Test
    @DisplayName("Should preserve goal count from goals list")
    void shouldPreserveGoalCount() {
        when(projectionService.saveProjection(any(CarePlanProjection.class)))
            .thenReturn(CarePlanProjection.builder().build());

        eventHandler.handleCarePlanCreatedEvent(testEvent);

        verify(projectionService).saveProjection(argThat(proj ->
            proj.getGoalCount().equals(2)  // 2 goals in the test event
        ));
    }

    @Test
    @DisplayName("Should handle multi-tenant isolation")
    void shouldEnforceMultiTenantIsolation() {
        CarePlanCreatedEvent tenant2Event = CarePlanCreatedEvent.builder()
            .tenantId("tenant-789")
            .patientId("patient-789")
            .carePlanTitle("Hypertension Care Plan")
            .startDate(LocalDate.of(2024, 3, 1))
            .endDate(LocalDate.of(2024, 9, 30))
            .careCoordinatorId("coordinator-789")
            .targetConditions(Arrays.asList("I10"))
            .goals(Arrays.asList("Monitor blood pressure"))
            .build();

        when(projectionService.saveProjection(any(CarePlanProjection.class)))
            .thenReturn(CarePlanProjection.builder().build());

        eventHandler.handleCarePlanCreatedEvent(tenant2Event);

        verify(projectionService).saveProjection(argThat(proj ->
            proj.getTenantId().equals("tenant-789")
        ));
    }

    @Test
    @DisplayName("Should handle empty goal list")
    void shouldHandleEmptyGoalList() {
        CarePlanCreatedEvent eventWithoutGoals = CarePlanCreatedEvent.builder()
            .tenantId("tenant-123")
            .patientId("patient-123")
            .carePlanTitle("Basic Care Plan")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .careCoordinatorId("coordinator-456")
            .targetConditions(Collections.singletonList("E11.9"))
            .goals(Collections.emptyList())
            .build();

        when(projectionService.saveProjection(any(CarePlanProjection.class)))
            .thenReturn(CarePlanProjection.builder().build());

        eventHandler.handleCarePlanCreatedEvent(eventWithoutGoals);

        verify(projectionService).saveProjection(argThat(proj ->
            proj.getGoalCount().equals(0)
        ));
    }

    @Test
    @DisplayName("Should handle null goal list")
    void shouldHandleNullGoalList() {
        CarePlanCreatedEvent eventWithoutGoals = CarePlanCreatedEvent.builder()
            .tenantId("tenant-123")
            .patientId("patient-123")
            .carePlanTitle("Care Plan")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .careCoordinatorId("coordinator-456")
            .targetConditions(Collections.singletonList("E11.9"))
            .goals(null)
            .build();

        when(projectionService.saveProjection(any(CarePlanProjection.class)))
            .thenReturn(CarePlanProjection.builder().build());

        eventHandler.handleCarePlanCreatedEvent(eventWithoutGoals);

        verify(projectionService).saveProjection(any());
    }

    @Test
    @DisplayName("Should handle service exception")
    void shouldHandleServiceException() {
        when(projectionService.saveProjection(any(CarePlanProjection.class)))
            .thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> eventHandler.handleCarePlanCreatedEvent(testEvent))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should handle null end date")
    void shouldHandleNullEndDate() {
        CarePlanCreatedEvent ongoingEvent = CarePlanCreatedEvent.builder()
            .tenantId("tenant-123")
            .patientId("patient-123")
            .carePlanTitle("Ongoing Care Plan")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(null)
            .careCoordinatorId("coordinator-456")
            .targetConditions(Arrays.asList("E11.9", "I10"))
            .goals(Arrays.asList("Monitor", "Manage"))
            .build();

        when(projectionService.saveProjection(any(CarePlanProjection.class)))
            .thenReturn(CarePlanProjection.builder().build());

        eventHandler.handleCarePlanCreatedEvent(ongoingEvent);

        verify(projectionService).saveProjection(argThat(proj ->
            proj.getEndDate() == null
        ));
    }

    @Test
    @DisplayName("Should not modify event after handling")
    void shouldNotModifyEvent() {
        String originalTitle = testEvent.getCarePlanTitle();
        when(projectionService.saveProjection(any(CarePlanProjection.class)))
            .thenReturn(CarePlanProjection.builder().build());

        eventHandler.handleCarePlanCreatedEvent(testEvent);

        assertThat(testEvent.getCarePlanTitle()).isEqualTo(originalTitle);
    }

    @Test
    @DisplayName("Should call service once per event")
    void shouldCallServiceOncePerEvent() {
        when(projectionService.saveProjection(any(CarePlanProjection.class)))
            .thenReturn(CarePlanProjection.builder().build());

        eventHandler.handleCarePlanCreatedEvent(testEvent);
        eventHandler.handleCarePlanCreatedEvent(testEvent);

        verify(projectionService, times(2)).saveProjection(any());
    }

    @Test
    @DisplayName("Should handle multiple target conditions")
    void shouldHandleMultipleTargetConditions() {
        CarePlanCreatedEvent multiConditionEvent = CarePlanCreatedEvent.builder()
            .tenantId("tenant-123")
            .patientId("patient-123")
            .carePlanTitle("Complex Care Plan")
            .startDate(LocalDate.of(2024, 1, 1))
            .endDate(LocalDate.of(2024, 12, 31))
            .careCoordinatorId("coordinator-456")
            .targetConditions(Arrays.asList("E11.9", "I10", "J44.9", "F32.9"))
            .goals(Arrays.asList("Goal 1", "Goal 2", "Goal 3"))
            .build();

        when(projectionService.saveProjection(any(CarePlanProjection.class)))
            .thenReturn(CarePlanProjection.builder().build());

        eventHandler.handleCarePlanCreatedEvent(multiConditionEvent);

        verify(projectionService).saveProjection(argThat(proj ->
            proj.getTenantId().equals("tenant-123") &&
            proj.getGoalCount().equals(3)
        ));
    }

    @Test
    @DisplayName("Should preserve care plan dates correctly")
    void shouldPreserveCarePlanDates() {
        LocalDate testStartDate = LocalDate.of(2023, 6, 15);
        LocalDate testEndDate = LocalDate.of(2025, 6, 14);
        CarePlanCreatedEvent timedEvent = CarePlanCreatedEvent.builder()
            .tenantId("tenant-123")
            .patientId("patient-123")
            .carePlanTitle("Timed Care Plan")
            .startDate(testStartDate)
            .endDate(testEndDate)
            .careCoordinatorId("coordinator-456")
            .targetConditions(Collections.singletonList("E11.9"))
            .goals(Collections.singletonList("Monitor"))
            .build();

        when(projectionService.saveProjection(any(CarePlanProjection.class)))
            .thenReturn(CarePlanProjection.builder().build());

        eventHandler.handleCarePlanCreatedEvent(timedEvent);

        verify(projectionService).saveProjection(argThat(proj ->
            proj.getStartDate().equals(testStartDate) &&
            proj.getEndDate().equals(testEndDate)
        ));
    }
}
