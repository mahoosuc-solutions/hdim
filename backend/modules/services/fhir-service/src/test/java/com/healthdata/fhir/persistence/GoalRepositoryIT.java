package com.healthdata.fhir.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for GoalRepository.
 * Tests all custom query methods for finding and tracking patient goals.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Transactional
class GoalRepositoryIT {

    private static final String H2_URL = "jdbc:h2:mem:healthdata_fhir_goal;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";
    private static final String TENANT_ID = "tenant-1";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID CONDITION_ID = UUID.randomUUID();

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> H2_URL);
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private GoalRepository goalRepository;

    @Autowired
    private PatientRepository patientRepository;

    @BeforeEach
    void setUp() {
        PatientEntity patient = PatientEntity.builder()
                .id(PATIENT_ID)
                .tenantId(TENANT_ID)
                .resourceType("Patient")
                .resourceJson("{\"resourceType\":\"Patient\",\"id\":\"" + PATIENT_ID + "\"}")
                .firstName("John")
                .lastName("Doe")
                .gender("male")
                .birthDate(LocalDate.of(1980, 1, 1))
                .build();
        patientRepository.save(patient);
    }

    @Test
    void shouldPersistAndRetrieveGoal() {
        // Given
        GoalEntity entity = createGoal("active", "in-progress", "high-priority", "Reduce A1c", LocalDate.now().plusMonths(3));

        // When
        GoalEntity saved = goalRepository.save(entity);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        GoalEntity found = goalRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getDescriptionText()).isEqualTo("Reduce A1c");
        assertThat(found.getLifecycleStatus()).isEqualTo("active");
    }

    @Test
    void shouldFindByTenantAndPatient() {
        // Given
        createGoal("active", "in-progress", "high-priority", "Reduce A1c", LocalDate.now().plusMonths(3));
        createGoal("completed", "achieved", "medium-priority", "Lose weight", LocalDate.now().minusDays(10));

        // When
        List<GoalEntity> results = goalRepository
                .findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByTargetDateAsc(TENANT_ID, PATIENT_ID);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void shouldFindActiveGoals() {
        // Given
        createGoal("active", "in-progress", "high-priority", "Reduce A1c", LocalDate.now().plusMonths(3));
        createGoal("completed", "achieved", "medium-priority", "Lose weight", LocalDate.now().minusDays(10));
        createGoal("active", "sustaining", "low-priority", "Exercise regularly", LocalDate.now().plusMonths(6));

        // When
        List<GoalEntity> active = goalRepository.findActiveGoalsForPatient(TENANT_ID, PATIENT_ID);

        // Then
        assertThat(active).hasSize(2);
        assertThat(active).allMatch(g -> g.getLifecycleStatus().equals("active"));
    }

    @Test
    void shouldFindOverdueGoals() {
        // Given
        createGoal("active", "in-progress", "high-priority", "Overdue goal", LocalDate.now().minusDays(10));
        createGoal("active", "in-progress", "medium-priority", "Future goal", LocalDate.now().plusMonths(3));

        // When
        List<GoalEntity> overdue = goalRepository.findOverdueGoals(TENANT_ID, PATIENT_ID, LocalDate.now());

        // Then
        assertThat(overdue).hasSize(1);
        assertThat(overdue.get(0).getDescriptionText()).isEqualTo("Overdue goal");
    }

    @Test
    void shouldFindHighPriorityGoals() {
        // Given
        createGoal("active", "in-progress", "high-priority", "High priority goal", LocalDate.now().plusMonths(3));
        createGoal("active", "in-progress", "medium-priority", "Medium priority goal", LocalDate.now().plusMonths(3));
        createGoal("active", "in-progress", "low-priority", "Low priority goal", LocalDate.now().plusMonths(3));

        // When
        List<GoalEntity> highPriority = goalRepository.findHighPriorityGoals(TENANT_ID, PATIENT_ID);

        // Then
        assertThat(highPriority).hasSize(1);
        assertThat(highPriority.get(0).getPriorityCode()).isEqualTo("high-priority");
    }

    @Test
    void shouldFindByAddressesCondition() {
        // Given
        GoalEntity withCondition = createGoal("active", "in-progress", "high-priority", "Related to condition", LocalDate.now().plusMonths(3));
        withCondition.setAddressesConditionId(CONDITION_ID);
        goalRepository.save(withCondition);

        createGoal("active", "in-progress", "medium-priority", "Not related", LocalDate.now().plusMonths(3));

        // When
        List<GoalEntity> results = goalRepository.findByTenantIdAndAddressesConditionIdAndDeletedAtIsNull(TENANT_ID, CONDITION_ID);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAddressesConditionId()).isEqualTo(CONDITION_ID);
    }

    @Test
    void shouldFindGoalsDueInRange() {
        // Given
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusMonths(2);

        createGoal("active", "in-progress", "high-priority", "Due soon", LocalDate.now().plusMonths(1));
        createGoal("active", "in-progress", "medium-priority", "Due later", LocalDate.now().plusMonths(6));
        createGoal("active", "in-progress", "low-priority", "Already past", LocalDate.now().minusMonths(1));

        // When
        List<GoalEntity> dueSoon = goalRepository.findGoalsDueInRange(TENANT_ID, startDate, endDate);

        // Then
        assertThat(dueSoon).hasSize(1);
        assertThat(dueSoon.get(0).getDescriptionText()).isEqualTo("Due soon");
    }

    @Test
    void shouldFindByAchievementStatus() {
        // Given
        createGoal("active", "in-progress", "high-priority", "In progress", LocalDate.now().plusMonths(3));
        createGoal("active", "achieved", "medium-priority", "Achieved", LocalDate.now().plusMonths(3));
        createGoal("active", "sustaining", "low-priority", "Sustaining", LocalDate.now().plusMonths(3));

        // When
        List<GoalEntity> achieved = goalRepository.findByTenantIdAndPatientIdAndAchievementStatusAndDeletedAtIsNull(
                TENANT_ID, PATIENT_ID, "achieved");

        // Then
        assertThat(achieved).hasSize(1);
        assertThat(achieved.get(0).getAchievementStatus()).isEqualTo("achieved");
    }

    @Test
    void shouldHandleMultiTenantIsolation() {
        // Given
        String tenant2 = "tenant-2";
        UUID patient2 = UUID.randomUUID();

        PatientEntity patient = PatientEntity.builder()
                .id(patient2)
                .tenantId(tenant2)
                .resourceType("Patient")
                .resourceJson("{\"resourceType\":\"Patient\",\"id\":\"" + patient2 + "\"}")
                .firstName("Jane")
                .lastName("Smith")
                .gender("female")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
        patientRepository.save(patient);

        createGoal("active", "in-progress", "high-priority", "Tenant 1 goal", LocalDate.now().plusMonths(3));

        UUID tenant2GoalId = UUID.randomUUID();
        GoalEntity tenant2Goal = GoalEntity.builder()
                .id(tenant2GoalId)
                .tenantId(tenant2)
                .patientId(patient2)
                .resourceJson("{\"resourceType\":\"Goal\",\"id\":\"" + tenant2GoalId + "\"}")
                .lifecycleStatus("active")
                .achievementStatus("in-progress")
                .priorityCode("high-priority")
                .descriptionText("Tenant 2 goal")
                .targetDate(LocalDate.now().plusMonths(3))
                .build();
        goalRepository.save(tenant2Goal);

        // When
        List<GoalEntity> tenant1Results = goalRepository
                .findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByTargetDateAsc(TENANT_ID, PATIENT_ID);
        List<GoalEntity> tenant2Results = goalRepository
                .findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByTargetDateAsc(tenant2, patient2);

        // Then
        assertThat(tenant1Results).hasSize(1);
        assertThat(tenant2Results).hasSize(1);
        assertThat(tenant1Results.get(0).getTenantId()).isEqualTo(TENANT_ID);
        assertThat(tenant2Results.get(0).getTenantId()).isEqualTo(tenant2);
    }

    private GoalEntity createGoal(String lifecycleStatus, String achievementStatus, String priority,
                                   String description, LocalDate targetDate) {
        UUID goalId = UUID.randomUUID();
        GoalEntity entity = GoalEntity.builder()
                .id(goalId)
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .resourceJson("{\"resourceType\":\"Goal\",\"id\":\"" + goalId + "\"}")
                .lifecycleStatus(lifecycleStatus)
                .achievementStatus(achievementStatus)
                .priorityCode(priority)
                .descriptionText(description)
                .categoryCode("physiologic")
                .categoryDisplay("Physiologic")
                .startDate(LocalDate.now())
                .targetDate(targetDate)
                .build();

        return goalRepository.save(entity);
    }
}
