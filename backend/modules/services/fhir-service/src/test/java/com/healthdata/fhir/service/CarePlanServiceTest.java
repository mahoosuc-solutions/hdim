package com.healthdata.fhir.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
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

import com.healthdata.fhir.persistence.CarePlanEntity;
import com.healthdata.fhir.persistence.CarePlanRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Unit tests for CarePlanService.
 * Tests service layer logic including FHIR conversions, business rules, and event publishing.
 */
class CarePlanServiceTest {

    private static final String TENANT = "tenant-1";
    private static final UUID PATIENT_ID = UUID.fromString("8b7e0540-2f8a-4f49-9f82-c0f4a6b46b95");
    private static final UUID CARE_PLAN_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID ENCOUNTER_ID = UUID.fromString("223e4567-e89b-12d3-a456-426614174001");
    private static final UUID CONDITION_ID = UUID.fromString("323e4567-e89b-12d3-a456-426614174002");
    private static final UUID GOAL_ID = UUID.fromString("423e4567-e89b-12d3-a456-426614174003");
    private static final UUID PARENT_CARE_PLAN_ID = UUID.fromString("523e4567-e89b-12d3-a456-426614174004");

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser();

    @Mock
    private CarePlanRepository carePlanRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private CarePlanService carePlanService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        carePlanService = new CarePlanService(carePlanRepository, kafkaTemplate);
    }

    @Test
    void createCarePlanShouldPersistAndPublishEvent() {
        // Given
        CarePlan carePlan = createFhirCarePlan();
        CarePlanEntity savedEntity = createCarePlanEntity();

        when(carePlanRepository.save(any(CarePlanEntity.class))).thenReturn(savedEntity);

        // When
        CarePlan result = carePlanService.createCarePlan(TENANT, carePlan, "user-1");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(CARE_PLAN_ID.toString());

        verify(carePlanRepository).save(any(CarePlanEntity.class));
        verify(kafkaTemplate).send(eq("fhir.care-plans.created"), eq(CARE_PLAN_ID.toString()), any());
    }

    @Test
    void createCarePlanShouldAssignIdIfNotPresent() {
        // Given
        CarePlan carePlan = new CarePlan();
        carePlan.setStatus(CarePlan.CarePlanStatus.ACTIVE);
        carePlan.setIntent(CarePlan.CarePlanIntent.PLAN);
        carePlan.setSubject(new Reference("Patient/" + PATIENT_ID));
        carePlan.setTitle("Diabetes Management Plan");

        CarePlanEntity savedEntity = CarePlanEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT)
                .patientId(PATIENT_ID)
                .status("active")
                .intent("plan")
                .title("Diabetes Management Plan")
                .build();

        when(carePlanRepository.save(any(CarePlanEntity.class))).thenReturn(savedEntity);

        // When
        CarePlan result = carePlanService.createCarePlan(TENANT, carePlan, "user-1");

        // Then
        assertThat(result.hasId()).isTrue();
        verify(carePlanRepository).save(any(CarePlanEntity.class));
    }

    @Test
    void createCarePlanShouldRejectMissingSubject() {
        // Given
        CarePlan carePlan = new CarePlan();
        carePlan.setId(CARE_PLAN_ID.toString());
        carePlan.setStatus(CarePlan.CarePlanStatus.ACTIVE);
        // No subject set

        // When/Then
        assertThatThrownBy(() -> carePlanService.createCarePlan(TENANT, carePlan, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subject");
    }

    @Test
    void createCarePlanShouldRejectInvalidPatientReference() {
        CarePlan carePlan = createFhirCarePlan();
        carePlan.setSubject(new Reference("Patient/not-a-uuid"));

        assertThatThrownBy(() -> carePlanService.createCarePlan(TENANT, carePlan, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subject");
    }

    @Test
    void createCarePlanShouldCaptureOptionalFields() {
        CarePlan carePlan = createFhirCarePlan();
        carePlan.setEncounter(new Reference("Encounter/" + ENCOUNTER_ID));
        carePlan.getCategoryFirstRep().setText("Care plan category");
        carePlan.addCareTeam(new Reference("CareTeam/team-1"));
        carePlan.addSupportingInfo(new Reference("DocumentReference/doc-1"));
        carePlan.addAddresses(new Reference("Condition/" + CONDITION_ID));
        carePlan.addGoal(new Reference("Goal/" + GOAL_ID));
        carePlan.addActivity().setDetail(new CarePlan.CarePlanActivityDetailComponent()
                .setStatus(CarePlan.CarePlanActivityStatus.INPROGRESS));
        carePlan.addPartOf(new Reference("CarePlan/" + PARENT_CARE_PLAN_ID));
        carePlan.addReplaces(new Reference("CarePlan/" + UUID.randomUUID()));

        when(carePlanRepository.save(any(CarePlanEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        carePlanService.createCarePlan(TENANT, carePlan, "user-1");

        ArgumentCaptor<CarePlanEntity> captor = ArgumentCaptor.forClass(CarePlanEntity.class);
        verify(carePlanRepository).save(captor.capture());
        CarePlanEntity entity = captor.getValue();
        assertThat(entity.getEncounterId()).isEqualTo(ENCOUNTER_ID);
        assertThat(entity.getCategoryDisplay()).isEqualTo("Care plan category");
        assertThat(entity.getCareTeamReferences()).contains("CareTeam/team-1");
        assertThat(entity.getSupportingInfoReferences()).contains("DocumentReference/doc-1");
        assertThat(entity.getAddressesReferences()).contains("Condition/" + CONDITION_ID);
        assertThat(entity.getGoalReferences()).contains("Goal/" + GOAL_ID);
        assertThat(entity.getActivityCount()).isEqualTo(2);
        assertThat(entity.getPartOfReference()).contains("CarePlan/" + PARENT_CARE_PLAN_ID);
        assertThat(entity.getReplacesReference()).contains("CarePlan/");
    }

    @Test
    void createCarePlanShouldIgnoreInvalidEncounterReference() {
        CarePlan carePlan = createFhirCarePlan();
        carePlan.setEncounter(new Reference("Encounter/not-a-uuid"));

        when(carePlanRepository.save(any(CarePlanEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        carePlanService.createCarePlan(TENANT, carePlan, "user-1");

        ArgumentCaptor<CarePlanEntity> captor = ArgumentCaptor.forClass(CarePlanEntity.class);
        verify(carePlanRepository).save(captor.capture());
        assertThat(captor.getValue().getEncounterId()).isNull();
    }

    @Test
    void createCarePlanShouldHandlePublishFailure() {
        CarePlan carePlan = createFhirCarePlan();
        CarePlanEntity savedEntity = createCarePlanEntity();

        when(carePlanRepository.save(any(CarePlanEntity.class))).thenReturn(savedEntity);
        doThrow(new RuntimeException("boom"))
                .when(kafkaTemplate).send(eq("fhir.care-plans.created"), eq(CARE_PLAN_ID.toString()), any());

        CarePlan result = carePlanService.createCarePlan(TENANT, carePlan, "user-1");

        assertThat(result).isNotNull();
        verify(carePlanRepository).save(any(CarePlanEntity.class));
    }

    @Test
    void getCarePlanShouldReturnFhirResource() {
        // Given
        CarePlanEntity entity = createCarePlanEntityWithJson();

        when(carePlanRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, CARE_PLAN_ID))
                .thenReturn(Optional.of(entity));

        // When
        Optional<CarePlan> result = carePlanService.getCarePlan(TENANT, CARE_PLAN_ID);

        // Then
        assertThat(result).isPresent();
        CarePlan carePlan = result.get();
        assertThat(carePlan.getIdElement().getIdPart()).isEqualTo(CARE_PLAN_ID.toString());
        assertThat(carePlan.getStatus().toCode()).isEqualTo("active");
    }

    @Test
    void getCarePlanShouldReturnEmptyWhenNotFound() {
        // Given
        when(carePlanRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, CARE_PLAN_ID))
                .thenReturn(Optional.empty());

        // When
        Optional<CarePlan> result = carePlanService.getCarePlan(TENANT, CARE_PLAN_ID);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void updateCarePlanShouldUpdateAndPublishEvent() {
        // Given
        CarePlanEntity existingEntity = createCarePlanEntityWithJson();
        CarePlan updatedCarePlan = createFhirCarePlan();
        updatedCarePlan.setStatus(CarePlan.CarePlanStatus.COMPLETED);

        when(carePlanRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, CARE_PLAN_ID))
                .thenReturn(Optional.of(existingEntity));
        when(carePlanRepository.save(any(CarePlanEntity.class))).thenReturn(existingEntity);

        // When
        CarePlan result = carePlanService.updateCarePlan(TENANT, CARE_PLAN_ID, updatedCarePlan, "user-2");

        // Then
        assertThat(result).isNotNull();
        verify(carePlanRepository).save(any(CarePlanEntity.class));
        verify(kafkaTemplate).send(eq("fhir.care-plans.updated"), eq(CARE_PLAN_ID.toString()), any());
    }

    @Test
    void updateCarePlanShouldThrowWhenNotFound() {
        // Given
        CarePlan carePlan = createFhirCarePlan();
        when(carePlanRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, CARE_PLAN_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> carePlanService.updateCarePlan(TENANT, CARE_PLAN_ID, carePlan, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void deleteCarePlanShouldSoftDeleteAndPublishEvent() {
        // Given
        CarePlanEntity entity = createCarePlanEntityWithJson();
        when(carePlanRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, CARE_PLAN_ID))
                .thenReturn(Optional.of(entity));
        when(carePlanRepository.save(any(CarePlanEntity.class))).thenReturn(entity);

        // When
        carePlanService.deleteCarePlan(TENANT, CARE_PLAN_ID, "user-3");

        // Then
        ArgumentCaptor<CarePlanEntity> captor = ArgumentCaptor.forClass(CarePlanEntity.class);
        verify(carePlanRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
        verify(kafkaTemplate).send(eq("fhir.care-plans.deleted"), eq(CARE_PLAN_ID.toString()), any());
    }

    @Test
    void getCarePlansByPatientShouldReturnList() {
        // Given
        List<CarePlanEntity> entities = List.of(
                createCarePlanEntityWithJson(),
                createCarePlanEntityWithJson()
        );

        when(carePlanRepository.findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByCreatedDateDesc(TENANT, PATIENT_ID))
                .thenReturn(entities);

        // When
        List<CarePlan> results = carePlanService.getCarePlansByPatient(TENANT, PATIENT_ID);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void getActiveCarePlansShouldReturnOnlyActive() {
        // Given
        List<CarePlanEntity> entities = List.of(createCarePlanEntityWithJson());

        when(carePlanRepository.findActiveCarePlansForPatient(eq(TENANT), eq(PATIENT_ID), any(Instant.class)))
                .thenReturn(entities);

        // When
        List<CarePlan> results = carePlanService.getActiveCarePlans(TENANT, PATIENT_ID);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void getPrimaryCarePlansShouldReturnTopLevel() {
        // Given
        List<CarePlanEntity> entities = List.of(createCarePlanEntityWithJson());

        when(carePlanRepository.findPrimaryCarePlansForPatient(TENANT, PATIENT_ID))
                .thenReturn(entities);

        // When
        List<CarePlan> results = carePlanService.getPrimaryCarePlans(TENANT, PATIENT_ID);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void getCarePlansByEncounterShouldReturnList() {
        // Given
        List<CarePlanEntity> entities = List.of(createCarePlanEntityWithJson());

        when(carePlanRepository.findByTenantIdAndEncounterIdAndDeletedAtIsNullOrderByCreatedDateDesc(TENANT, ENCOUNTER_ID))
                .thenReturn(entities);

        // When
        List<CarePlan> results = carePlanService.getCarePlansByEncounter(TENANT, ENCOUNTER_ID);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void getCarePlansByCategoryShouldReturnFilteredList() {
        // Given
        List<CarePlanEntity> entities = List.of(createCarePlanEntityWithJson());

        when(carePlanRepository.findByTenantIdAndPatientIdAndCategoryCodeAndDeletedAtIsNull(TENANT, PATIENT_ID, "assess-plan"))
                .thenReturn(entities);

        // When
        List<CarePlan> results = carePlanService.getCarePlansByCategory(TENANT, PATIENT_ID, "assess-plan");

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void getCarePlansByConditionShouldReturnRelatedPlans() {
        // Given
        List<CarePlanEntity> entities = List.of(createCarePlanEntityWithJson());
        String conditionRef = "Condition/" + CONDITION_ID;

        when(carePlanRepository.findByAddresses(TENANT, conditionRef))
                .thenReturn(entities);

        // When
        List<CarePlan> results = carePlanService.getCarePlansByCondition(TENANT, CONDITION_ID);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void getCarePlansByGoalShouldReturnRelatedPlans() {
        // Given
        List<CarePlanEntity> entities = List.of(createCarePlanEntityWithJson());
        String goalRef = "Goal/" + GOAL_ID;

        when(carePlanRepository.findByGoal(TENANT, goalRef))
                .thenReturn(entities);

        // When
        List<CarePlan> results = carePlanService.getCarePlansByGoal(TENANT, GOAL_ID);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void getChildCarePlansShouldReturnSubPlans() {
        // Given
        List<CarePlanEntity> entities = List.of(createCarePlanEntityWithJson());
        String parentRef = "CarePlan/" + PARENT_CARE_PLAN_ID;

        when(carePlanRepository.findByTenantIdAndPartOfReferenceAndDeletedAtIsNull(TENANT, parentRef))
                .thenReturn(entities);

        // When
        List<CarePlan> results = carePlanService.getChildCarePlans(TENANT, PARENT_CARE_PLAN_ID);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void getCarePlansWithActivitiesShouldReturnPlansWithActivities() {
        // Given
        List<CarePlanEntity> entities = List.of(createCarePlanEntityWithJson());

        when(carePlanRepository.findWithActivities(TENANT, PATIENT_ID))
                .thenReturn(entities);

        // When
        List<CarePlan> results = carePlanService.getCarePlansWithActivities(TENANT, PATIENT_ID);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void getExpiringCarePlansShouldReturnPlansExpiringSoon() {
        // Given
        Instant startDate = Instant.now();
        Instant endDate = Instant.now().plusSeconds(30 * 24 * 60 * 60);
        List<CarePlanEntity> entities = List.of(createCarePlanEntityWithJson());

        when(carePlanRepository.findExpiringCarePlans(TENANT, startDate, endDate))
                .thenReturn(entities);

        // When
        List<CarePlan> results = carePlanService.getExpiringCarePlans(TENANT, startDate, endDate);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void searchCarePlansShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<CarePlanEntity> entities = List.of(createCarePlanEntityWithJson());
        Page<CarePlanEntity> entityPage = new PageImpl<>(entities, pageable, 1);

        when(carePlanRepository.searchCarePlans(eq(TENANT), eq(PATIENT_ID), any(), eq("active"), any(), any(), eq(pageable)))
                .thenReturn(entityPage);

        // When
        Page<CarePlan> results = carePlanService.searchCarePlans(
                TENANT, PATIENT_ID, null, "active", null, null, pageable);

        // Then
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getTotalElements()).isEqualTo(1);
    }

    @Test
    void searchByTextShouldReturnMatchingPlans() {
        // Given
        List<CarePlanEntity> entities = List.of(createCarePlanEntityWithJson());

        when(carePlanRepository.searchByText(TENANT, PATIENT_ID, "diabetes"))
                .thenReturn(entities);

        // When
        List<CarePlan> results = carePlanService.searchByText(TENANT, PATIENT_ID, "diabetes");

        // Then
        assertThat(results).hasSize(1);
    }

    // Helper methods

    private CarePlan createFhirCarePlan() {
        CarePlan carePlan = new CarePlan();
        carePlan.setId(CARE_PLAN_ID.toString());
        carePlan.setStatus(CarePlan.CarePlanStatus.ACTIVE);
        carePlan.setIntent(CarePlan.CarePlanIntent.PLAN);
        carePlan.setSubject(new Reference("Patient/" + PATIENT_ID));
        carePlan.setTitle("Diabetes Management Plan");
        carePlan.setDescription("Comprehensive care plan for type 2 diabetes management");
        carePlan.addCategory(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://hl7.org/fhir/us/core/CodeSystem/careplan-category")
                        .setCode("assess-plan")
                        .setDisplay("Assessment and Plan of Treatment")));
        carePlan.setPeriod(new Period()
                .setStart(new Date())
                .setEnd(Date.from(Instant.now().plusSeconds(365 * 24 * 60 * 60))));
        carePlan.setCreated(new Date());
        carePlan.setAuthor(new Reference("Practitioner/dr-smith").setDisplay("Dr. Smith"));
        carePlan.addGoal(new Reference("Goal/" + GOAL_ID));
        carePlan.addAddresses(new Reference("Condition/" + CONDITION_ID));
        carePlan.addActivity()
                .setDetail(new CarePlan.CarePlanActivityDetailComponent()
                        .setStatus(CarePlan.CarePlanActivityStatus.NOTSTARTED)
                        .setDescription("Monitor blood glucose levels daily"));
        return carePlan;
    }

    private CarePlanEntity createCarePlanEntity() {
        return CarePlanEntity.builder()
                .id(CARE_PLAN_ID)
                .tenantId(TENANT)
                .patientId(PATIENT_ID)
                .status("active")
                .intent("plan")
                .title("Diabetes Management Plan")
                .description("Comprehensive care plan for type 2 diabetes management")
                .categoryCode("assess-plan")
                .categoryDisplay("Assessment and Plan of Treatment")
                .periodStart(Instant.now())
                .periodEnd(Instant.now().plusSeconds(365 * 24 * 60 * 60))
                .createdDate(Instant.now())
                .authorReference("Practitioner/dr-smith")
                .activityCount(1)
                .goalReferences("Goal/" + GOAL_ID)
                .addressesReferences("Condition/" + CONDITION_ID)
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
    }

    private CarePlanEntity createCarePlanEntityWithJson() {
        CarePlan carePlan = createFhirCarePlan();
        String json = JSON_PARSER.encodeResourceToString(carePlan);

        return CarePlanEntity.builder()
                .id(CARE_PLAN_ID)
                .tenantId(TENANT)
                .patientId(PATIENT_ID)
                .resourceJson(json)
                .status("active")
                .intent("plan")
                .title("Diabetes Management Plan")
                .description("Comprehensive care plan for type 2 diabetes management")
                .categoryCode("assess-plan")
                .categoryDisplay("Assessment and Plan of Treatment")
                .periodStart(Instant.now())
                .periodEnd(Instant.now().plusSeconds(365 * 24 * 60 * 60))
                .createdDate(Instant.now())
                .authorReference("Practitioner/dr-smith")
                .activityCount(1)
                .goalReferences("Goal/" + GOAL_ID)
                .addressesReferences("Condition/" + CONDITION_ID)
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
    }
}
