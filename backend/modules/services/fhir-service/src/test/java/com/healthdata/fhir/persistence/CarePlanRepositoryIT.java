package com.healthdata.fhir.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for CarePlanRepository.
 * Tests all custom query methods for finding and tracking care plans.
 */
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Transactional
@Tag("integration")
class CarePlanRepositoryIT {

    private static final String H2_URL = "jdbc:tc:postgresql:15-alpine:///testdb";
    private static final String TENANT_ID = "tenant-1";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID ENCOUNTER_ID = UUID.randomUUID();
    private static final UUID CONDITION_ID = UUID.randomUUID();
    private static final UUID GOAL_ID = UUID.randomUUID();

    @DynamicPropertySource
    static void overrideDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> H2_URL);
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.testcontainers.jdbc.ContainerDatabaseDriver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private CarePlanRepository carePlanRepository;

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
    void shouldPersistAndRetrieveCarePlan() {
        // Given
        CarePlanEntity entity = createCarePlan("active", "plan", "Diabetes Management", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));

        // When
        CarePlanEntity saved = carePlanRepository.save(entity);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();

        CarePlanEntity found = carePlanRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getTitle()).isEqualTo("Diabetes Management");
        assertThat(found.getStatus()).isEqualTo("active");
    }

    @Test
    void shouldFindByTenantAndPatient() {
        // Given
        createCarePlan("active", "plan", "Plan 1", Instant.now().minusSeconds(60 * 60), Instant.now().plusSeconds(365 * 24 * 60 * 60));
        createCarePlan("completed", "plan", "Plan 2", Instant.now().minusSeconds(365 * 24 * 60 * 60), Instant.now().minusSeconds(30 * 24 * 60 * 60));

        // When
        List<CarePlanEntity> results = carePlanRepository
                .findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByCreatedDateDesc(TENANT_ID, PATIENT_ID);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void shouldFindActiveCarePlans() {
        // Given
        Instant now = Instant.now();
        createCarePlan("active", "plan", "Active plan", now.minusSeconds(30 * 24 * 60 * 60), now.plusSeconds(365 * 24 * 60 * 60));
        createCarePlan("completed", "plan", "Completed plan", now.minusSeconds(365 * 24 * 60 * 60), now.minusSeconds(30 * 24 * 60 * 60));
        createCarePlan("revoked", "plan", "Revoked plan", now.minusSeconds(60 * 24 * 60 * 60), now.plusSeconds(365 * 24 * 60 * 60));

        // When
        List<CarePlanEntity> active = carePlanRepository.findActiveCarePlansForPatient(TENANT_ID, PATIENT_ID, now);

        // Then
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getStatus()).isEqualTo("active");
    }

    @Test
    void shouldFindPrimaryCarePlans() {
        // Given
        CarePlanEntity primary = createCarePlan("active", "plan", "Primary plan", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));
        // No partOfReference means it's a primary/top-level plan

        CarePlanEntity child = createCarePlan("active", "plan", "Child plan", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));
        child.setPartOfReference("CarePlan/" + primary.getId());
        carePlanRepository.save(child);

        // When
        List<CarePlanEntity> primaryPlans = carePlanRepository.findPrimaryCarePlansForPatient(TENANT_ID, PATIENT_ID);

        // Then
        assertThat(primaryPlans).hasSize(1);
        assertThat(primaryPlans.get(0).getPartOfReference()).isNull();
    }

    @Test
    void shouldFindByEncounter() {
        // Given
        CarePlanEntity withEncounter = createCarePlan("active", "plan", "With encounter", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));
        withEncounter.setEncounterId(ENCOUNTER_ID);
        carePlanRepository.save(withEncounter);

        createCarePlan("active", "plan", "Without encounter", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));

        // When
        List<CarePlanEntity> results = carePlanRepository
                .findByTenantIdAndEncounterIdAndDeletedAtIsNullOrderByCreatedDateDesc(TENANT_ID, ENCOUNTER_ID);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEncounterId()).isEqualTo(ENCOUNTER_ID);
    }

    @Test
    void shouldFindByCategory() {
        // Given
        CarePlanEntity assessPlan = createCarePlan("active", "plan", "Assessment plan", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));
        assessPlan.setCategoryCode("assess-plan");
        carePlanRepository.save(assessPlan);

        CarePlanEntity longitudinal = createCarePlan("active", "plan", "Longitudinal plan", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));
        longitudinal.setCategoryCode("longitudinal");
        carePlanRepository.save(longitudinal);

        // When
        List<CarePlanEntity> assessPlans = carePlanRepository
                .findByTenantIdAndPatientIdAndCategoryCodeAndDeletedAtIsNull(TENANT_ID, PATIENT_ID, "assess-plan");

        // Then
        assertThat(assessPlans).hasSize(1);
        assertThat(assessPlans.get(0).getCategoryCode()).isEqualTo("assess-plan");
    }

    @Test
    void shouldFindByAddresses() {
        // Given
        CarePlanEntity addressingCondition = createCarePlan("active", "plan", "Addresses condition", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));
        addressingCondition.setAddressesReferences("Condition/" + CONDITION_ID);
        carePlanRepository.save(addressingCondition);

        createCarePlan("active", "plan", "No addresses", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));

        // When
        List<CarePlanEntity> results = carePlanRepository.findByAddresses(TENANT_ID, "Condition/" + CONDITION_ID);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAddressesReferences()).contains(CONDITION_ID.toString());
    }

    @Test
    void shouldFindByGoal() {
        // Given
        CarePlanEntity withGoal = createCarePlan("active", "plan", "With goal", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));
        withGoal.setGoalReferences("Goal/" + GOAL_ID);
        carePlanRepository.save(withGoal);

        createCarePlan("active", "plan", "No goal", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));

        // When
        List<CarePlanEntity> results = carePlanRepository.findByGoal(TENANT_ID, "Goal/" + GOAL_ID);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getGoalReferences()).contains(GOAL_ID.toString());
    }

    @Test
    void shouldFindChildCarePlans() {
        // Given
        CarePlanEntity parent = createCarePlan("active", "plan", "Parent plan", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));

        CarePlanEntity child1 = createCarePlan("active", "plan", "Child 1", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));
        child1.setPartOfReference("CarePlan/" + parent.getId());
        carePlanRepository.save(child1);

        CarePlanEntity child2 = createCarePlan("active", "plan", "Child 2", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));
        child2.setPartOfReference("CarePlan/" + parent.getId());
        carePlanRepository.save(child2);

        // When
        List<CarePlanEntity> children = carePlanRepository
                .findByTenantIdAndPartOfReferenceAndDeletedAtIsNull(TENANT_ID, "CarePlan/" + parent.getId());

        // Then
        assertThat(children).hasSize(2);
    }

    @Test
    void shouldFindWithActivities() {
        // Given
        CarePlanEntity withActivities = createCarePlan("active", "plan", "Has activities", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));
        withActivities.setActivityCount(3);
        carePlanRepository.save(withActivities);

        CarePlanEntity noActivities = createCarePlan("active", "plan", "No activities", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));
        noActivities.setActivityCount(0);
        carePlanRepository.save(noActivities);

        // When
        List<CarePlanEntity> results = carePlanRepository.findWithActivities(TENANT_ID, PATIENT_ID);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getActivityCount()).isGreaterThan(0);
    }

    @Test
    void shouldFindExpiringCarePlans() {
        // Given
        Instant now = Instant.now();
        Instant thirtyDaysFromNow = now.plusSeconds(30 * 24 * 60 * 60);

        createCarePlan("active", "plan", "Expiring soon", now.minusSeconds(300 * 24 * 60 * 60), now.plusSeconds(15 * 24 * 60 * 60));
        createCarePlan("active", "plan", "Expiring later", now.minusSeconds(300 * 24 * 60 * 60), now.plusSeconds(60 * 24 * 60 * 60));

        // When
        List<CarePlanEntity> expiring = carePlanRepository.findExpiringCarePlans(TENANT_ID, now, thirtyDaysFromNow);

        // Then
        assertThat(expiring).hasSize(1);
        assertThat(expiring.get(0).getTitle()).isEqualTo("Expiring soon");
    }

    @Test
    void shouldSearchByText() {
        // Given
        createCarePlan("active", "plan", "Diabetes management comprehensive plan", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));
        createCarePlan("active", "plan", "Hypertension control plan", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));
        createCarePlan("active", "plan", "Diabetic foot care", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));

        // When
        List<CarePlanEntity> diabetesPlans = carePlanRepository.searchByText(TENANT_ID, PATIENT_ID, "diabet");

        // Then
        assertThat(diabetesPlans).hasSize(2);
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

        createCarePlan("active", "plan", "Tenant 1 plan", Instant.now(), Instant.now().plusSeconds(365 * 24 * 60 * 60));

        UUID tenant2PlanId = UUID.randomUUID();
        CarePlanEntity tenant2Plan = CarePlanEntity.builder()
                .id(tenant2PlanId)
                .tenantId(tenant2)
                .patientId(patient2)
                .resourceJson("{\"resourceType\":\"CarePlan\",\"id\":\"" + tenant2PlanId + "\"}")
                .status("active")
                .intent("plan")
                .title("Tenant 2 plan")
                .periodStart(Instant.now())
                .periodEnd(Instant.now().plusSeconds(365 * 24 * 60 * 60))
                .build();
        carePlanRepository.save(tenant2Plan);

        // When
        List<CarePlanEntity> tenant1Results = carePlanRepository
                .findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByCreatedDateDesc(TENANT_ID, PATIENT_ID);
        List<CarePlanEntity> tenant2Results = carePlanRepository
                .findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByCreatedDateDesc(tenant2, patient2);

        // Then
        assertThat(tenant1Results).hasSize(1);
        assertThat(tenant2Results).hasSize(1);
    }

    private CarePlanEntity createCarePlan(String status, String intent, String title, Instant periodStart, Instant periodEnd) {
        UUID carePlanId = UUID.randomUUID();
        CarePlanEntity entity = CarePlanEntity.builder()
                .id(carePlanId)
                .tenantId(TENANT_ID)
                .patientId(PATIENT_ID)
                .resourceJson("{\"resourceType\":\"CarePlan\",\"id\":\"" + carePlanId + "\"}")
                .status(status)
                .intent(intent)
                .title(title)
                .description("Care plan description for " + title)
                .categoryCode("assess-plan")
                .categoryDisplay("Assessment and Plan")
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .createdDate(periodStart)
                .activityCount(0)
                .build();

        return carePlanRepository.save(entity);
    }
}
