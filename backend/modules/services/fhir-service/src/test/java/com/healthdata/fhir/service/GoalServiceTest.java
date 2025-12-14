package com.healthdata.fhir.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import com.healthdata.fhir.persistence.GoalEntity;
import com.healthdata.fhir.persistence.GoalRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Unit tests for GoalService.
 * Tests service layer logic including FHIR conversions, business rules, and event publishing.
 */
class GoalServiceTest {

    private static final String TENANT = "tenant-1";
    private static final UUID PATIENT_ID = UUID.fromString("8b7e0540-2f8a-4f49-9f82-c0f4a6b46b95");
    private static final UUID GOAL_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID CONDITION_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser();

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private GoalService goalService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        goalService = new GoalService(goalRepository, kafkaTemplate);
    }

    @Test
    void createGoalShouldPersistAndPublishEvent() {
        // Given
        Goal goal = createFhirGoal();
        GoalEntity savedEntity = createGoalEntity();

        when(goalRepository.save(any(GoalEntity.class))).thenReturn(savedEntity);

        // When
        Goal result = goalService.createGoal(TENANT, goal, "user-1");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(GOAL_ID.toString());

        verify(goalRepository).save(any(GoalEntity.class));
        verify(kafkaTemplate).send(eq("fhir.goals.created"), eq(GOAL_ID.toString()), any());
    }

    @Test
    void createGoalShouldAssignIdIfNotPresent() {
        // Given
        Goal goal = new Goal();
        goal.setLifecycleStatus(Goal.GoalLifecycleStatus.ACTIVE);
        goal.setSubject(new Reference("Patient/" + PATIENT_ID));
        goal.setDescription(new CodeableConcept().setText("Lose weight"));

        GoalEntity savedEntity = GoalEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT)
                .patientId(PATIENT_ID)
                .lifecycleStatus("active")
                .descriptionText("Lose weight")
                .build();

        when(goalRepository.save(any(GoalEntity.class))).thenReturn(savedEntity);

        // When
        Goal result = goalService.createGoal(TENANT, goal, "user-1");

        // Then
        assertThat(result.hasId()).isTrue();
        verify(goalRepository).save(any(GoalEntity.class));
    }

    @Test
    void createGoalShouldRejectMissingSubject() {
        // Given
        Goal goal = new Goal();
        goal.setId(GOAL_ID.toString());
        goal.setLifecycleStatus(Goal.GoalLifecycleStatus.ACTIVE);
        // No subject set

        // When/Then
        assertThatThrownBy(() -> goalService.createGoal(TENANT, goal, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subject");
    }

    @Test
    void getGoalShouldReturnFhirResource() {
        // Given
        GoalEntity entity = createGoalEntityWithJson();

        when(goalRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, GOAL_ID))
                .thenReturn(Optional.of(entity));

        // When
        Optional<Goal> result = goalService.getGoal(TENANT, GOAL_ID);

        // Then
        assertThat(result).isPresent();
        Goal goal = result.get();
        assertThat(goal.getIdElement().getIdPart()).isEqualTo(GOAL_ID.toString());
        assertThat(goal.getLifecycleStatus().toCode()).isEqualTo("active");
    }

    @Test
    void getGoalShouldReturnEmptyWhenNotFound() {
        // Given
        when(goalRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, GOAL_ID))
                .thenReturn(Optional.empty());

        // When
        Optional<Goal> result = goalService.getGoal(TENANT, GOAL_ID);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void updateGoalShouldUpdateAndPublishEvent() {
        // Given
        GoalEntity existingEntity = createGoalEntityWithJson();
        Goal updatedGoal = createFhirGoal();
        updatedGoal.setLifecycleStatus(Goal.GoalLifecycleStatus.COMPLETED);

        when(goalRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, GOAL_ID))
                .thenReturn(Optional.of(existingEntity));
        when(goalRepository.save(any(GoalEntity.class))).thenReturn(existingEntity);

        // When
        Goal result = goalService.updateGoal(TENANT, GOAL_ID, updatedGoal, "user-2");

        // Then
        assertThat(result).isNotNull();
        verify(goalRepository).save(any(GoalEntity.class));
        verify(kafkaTemplate).send(eq("fhir.goals.updated"), eq(GOAL_ID.toString()), any());
    }

    @Test
    void updateGoalShouldThrowWhenNotFound() {
        // Given
        Goal goal = createFhirGoal();
        when(goalRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, GOAL_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> goalService.updateGoal(TENANT, GOAL_ID, goal, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void deleteGoalShouldSoftDeleteAndPublishEvent() {
        // Given
        GoalEntity entity = createGoalEntityWithJson();
        when(goalRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, GOAL_ID))
                .thenReturn(Optional.of(entity));
        when(goalRepository.save(any(GoalEntity.class))).thenReturn(entity);

        // When
        goalService.deleteGoal(TENANT, GOAL_ID, "user-3");

        // Then
        ArgumentCaptor<GoalEntity> captor = ArgumentCaptor.forClass(GoalEntity.class);
        verify(goalRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
        verify(kafkaTemplate).send(eq("fhir.goals.deleted"), eq(GOAL_ID.toString()), any());
    }

    @Test
    void getGoalsByPatientShouldReturnList() {
        // Given
        List<GoalEntity> entities = List.of(
                createGoalEntityWithJson(),
                createGoalEntityWithJson()
        );

        when(goalRepository.findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByTargetDateAsc(TENANT, PATIENT_ID))
                .thenReturn(entities);

        // When
        List<Goal> results = goalService.getGoalsByPatient(TENANT, PATIENT_ID);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void getActiveGoalsShouldReturnOnlyActive() {
        // Given
        List<GoalEntity> entities = List.of(createGoalEntityWithJson());

        when(goalRepository.findActiveGoalsForPatient(TENANT, PATIENT_ID))
                .thenReturn(entities);

        // When
        List<Goal> results = goalService.getActiveGoals(TENANT, PATIENT_ID);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void getOverdueGoalsShouldReturnOverdue() {
        // Given
        List<GoalEntity> entities = List.of(createGoalEntityWithJson());

        when(goalRepository.findOverdueGoals(eq(TENANT), eq(PATIENT_ID), any(LocalDate.class)))
                .thenReturn(entities);

        // When
        List<Goal> results = goalService.getOverdueGoals(TENANT, PATIENT_ID);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void getHighPriorityGoalsShouldReturnHighPriority() {
        // Given
        List<GoalEntity> entities = List.of(createGoalEntityWithJson());

        when(goalRepository.findHighPriorityGoals(TENANT, PATIENT_ID))
                .thenReturn(entities);

        // When
        List<Goal> results = goalService.getHighPriorityGoals(TENANT, PATIENT_ID);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void getGoalsByConditionShouldReturnRelatedGoals() {
        // Given
        List<GoalEntity> entities = List.of(createGoalEntityWithJson());

        when(goalRepository.findByTenantIdAndAddressesConditionIdAndDeletedAtIsNull(TENANT, CONDITION_ID))
                .thenReturn(entities);

        // When
        List<Goal> results = goalService.getGoalsByCondition(TENANT, CONDITION_ID);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void getGoalsDueInRangeShouldReturnGoalsInRange() {
        // Given
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusMonths(1);
        List<GoalEntity> entities = List.of(createGoalEntityWithJson());

        when(goalRepository.findGoalsDueInRange(TENANT, startDate, endDate))
                .thenReturn(entities);

        // When
        List<Goal> results = goalService.getGoalsDueInRange(TENANT, startDate, endDate);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void searchGoalsShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<GoalEntity> entities = List.of(createGoalEntityWithJson());
        Page<GoalEntity> entityPage = new PageImpl<>(entities, pageable, 1);

        when(goalRepository.searchGoals(eq(TENANT), eq(PATIENT_ID), eq("active"), any(), any(), any(), eq(pageable)))
                .thenReturn(entityPage);

        // When
        Page<Goal> results = goalService.searchGoals(TENANT, PATIENT_ID, "active", null, null, null, pageable);

        // Then
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getTotalElements()).isEqualTo(1);
    }

    // Helper methods

    private Goal createFhirGoal() {
        Goal goal = new Goal();
        goal.setId(GOAL_ID.toString());
        goal.setLifecycleStatus(Goal.GoalLifecycleStatus.ACTIVE);
        goal.setSubject(new Reference("Patient/" + PATIENT_ID));
        goal.setDescription(new CodeableConcept().setText("Reduce A1c to below 7%"));
        goal.setPriority(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/goal-priority")
                        .setCode("high-priority")
                        .setDisplay("High Priority")));
        goal.addCategory(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/goal-category")
                        .setCode("physiologic")
                        .setDisplay("Physiologic")));
        goal.setAchievementStatus(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/goal-achievement")
                        .setCode("in-progress")
                        .setDisplay("In Progress")));
        goal.setStart(new DateType(new Date()));
        goal.addTarget()
                .setDue(new DateType(Date.from(Instant.now().plusSeconds(90 * 24 * 60 * 60))));
        return goal;
    }

    private GoalEntity createGoalEntity() {
        return GoalEntity.builder()
                .id(GOAL_ID)
                .tenantId(TENANT)
                .patientId(PATIENT_ID)
                .lifecycleStatus("active")
                .achievementStatus("in-progress")
                .priorityCode("high-priority")
                .descriptionText("Reduce A1c to below 7%")
                .categoryCode("physiologic")
                .categoryDisplay("Physiologic")
                .startDate(LocalDate.now())
                .targetDate(LocalDate.now().plusMonths(3))
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
    }

    private GoalEntity createGoalEntityWithJson() {
        Goal goal = createFhirGoal();
        String json = JSON_PARSER.encodeResourceToString(goal);

        return GoalEntity.builder()
                .id(GOAL_ID)
                .tenantId(TENANT)
                .patientId(PATIENT_ID)
                .resourceJson(json)
                .lifecycleStatus("active")
                .achievementStatus("in-progress")
                .priorityCode("high-priority")
                .descriptionText("Reduce A1c to below 7%")
                .categoryCode("physiologic")
                .categoryDisplay("Physiologic")
                .startDate(LocalDate.now())
                .targetDate(LocalDate.now().plusMonths(3))
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
    }
}
