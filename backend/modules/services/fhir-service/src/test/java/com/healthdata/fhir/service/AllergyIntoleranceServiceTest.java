package com.healthdata.fhir.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import com.healthdata.fhir.persistence.AllergyIntoleranceEntity;
import com.healthdata.fhir.persistence.AllergyIntoleranceRepository;

class AllergyIntoleranceServiceTest {

    private static final String TENANT = "tenant-1";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID ALLERGY_ID = UUID.randomUUID();
    private static final String ALLERGY_CODE = "227037002";  // Fish

    @Mock
    private AllergyIntoleranceRepository allergyIntoleranceRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private AllergyIntoleranceService allergyIntoleranceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        allergyIntoleranceService = new AllergyIntoleranceService(allergyIntoleranceRepository, kafkaTemplate);
    }

    @Test
    void createAllergyIntoleranceShouldPersistAndPublish() {
        AllergyIntolerance allergyIntolerance = createValidAllergyIntolerance();
        AllergyIntoleranceEntity savedEntity = createAllergyEntity();

        when(allergyIntoleranceRepository.save(any(AllergyIntoleranceEntity.class))).thenReturn(savedEntity);

        AllergyIntolerance result = allergyIntoleranceService.createAllergyIntolerance(
                TENANT, allergyIntolerance, "user-1");

        assertThat(result).isNotNull();
        assertThat(result.hasId()).isTrue();
        verify(allergyIntoleranceRepository).save(any(AllergyIntoleranceEntity.class));
        verify(kafkaTemplate).send(eq("audit-events"), any(String.class));
    }

    @Test
    void createAllergyIntoleranceShouldGenerateIdIfNotPresent() {
        AllergyIntolerance allergyIntolerance = createValidAllergyIntolerance();
        allergyIntolerance.setId((String) null);  // Remove ID

        when(allergyIntoleranceRepository.save(any(AllergyIntoleranceEntity.class)))
                .thenAnswer(invocation -> {
                    AllergyIntoleranceEntity entity = invocation.getArgument(0);
                    return entity;
                });

        AllergyIntolerance result = allergyIntoleranceService.createAllergyIntolerance(
                TENANT, allergyIntolerance, "user-1");

        assertThat(result.hasId()).isTrue();
        assertThat(UUID.fromString(result.getIdElement().getIdPart())).isNotNull();
    }

    @Test
    void createAllergyIntoleranceShouldExtractAllergenCode() {
        AllergyIntolerance allergyIntolerance = createValidAllergyIntolerance();

        ArgumentCaptor<AllergyIntoleranceEntity> entityCaptor = ArgumentCaptor.forClass(AllergyIntoleranceEntity.class);
        when(allergyIntoleranceRepository.save(entityCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        allergyIntoleranceService.createAllergyIntolerance(TENANT, allergyIntolerance, "user-1");

        AllergyIntoleranceEntity captured = entityCaptor.getValue();
        assertThat(captured.getCode()).isEqualTo(ALLERGY_CODE);
        assertThat(captured.getCodeSystem()).isEqualTo("http://snomed.info/sct");
        assertThat(captured.getCodeDisplay()).isEqualTo("Fish (substance)");
    }

    @Test
    void createAllergyIntoleranceShouldExtractClinicalStatus() {
        AllergyIntolerance allergyIntolerance = createValidAllergyIntolerance();

        ArgumentCaptor<AllergyIntoleranceEntity> entityCaptor = ArgumentCaptor.forClass(AllergyIntoleranceEntity.class);
        when(allergyIntoleranceRepository.save(entityCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        allergyIntoleranceService.createAllergyIntolerance(TENANT, allergyIntolerance, "user-1");

        AllergyIntoleranceEntity captured = entityCaptor.getValue();
        assertThat(captured.getClinicalStatus()).isEqualTo("active");
        assertThat(captured.getVerificationStatus()).isEqualTo("confirmed");
    }

    @Test
    void createAllergyIntoleranceShouldExtractCriticality() {
        AllergyIntolerance allergyIntolerance = createValidAllergyIntolerance();

        ArgumentCaptor<AllergyIntoleranceEntity> entityCaptor = ArgumentCaptor.forClass(AllergyIntoleranceEntity.class);
        when(allergyIntoleranceRepository.save(entityCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        allergyIntoleranceService.createAllergyIntolerance(TENANT, allergyIntolerance, "user-1");

        AllergyIntoleranceEntity captured = entityCaptor.getValue();
        assertThat(captured.getCriticality()).isEqualTo("high");
    }

    @Test
    void createAllergyIntoleranceShouldExtractReactionDetails() {
        AllergyIntolerance allergyIntolerance = createAllergyWithReaction();

        ArgumentCaptor<AllergyIntoleranceEntity> entityCaptor = ArgumentCaptor.forClass(AllergyIntoleranceEntity.class);
        when(allergyIntoleranceRepository.save(entityCaptor.capture()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        allergyIntoleranceService.createAllergyIntolerance(TENANT, allergyIntolerance, "user-1");

        AllergyIntoleranceEntity captured = entityCaptor.getValue();
        assertThat(captured.getHasReactions()).isTrue();
        assertThat(captured.getReactionSeverity()).isEqualTo("severe");
    }

    @Test
    void getAllergyIntoleranceShouldReturnResource() {
        AllergyIntoleranceEntity entity = createAllergyEntity();
        when(allergyIntoleranceRepository.findByTenantIdAndId(TENANT, ALLERGY_ID))
                .thenReturn(Optional.of(entity));

        Optional<AllergyIntolerance> result = allergyIntoleranceService
                .getAllergyIntolerance(TENANT, ALLERGY_ID.toString());

        assertThat(result).isPresent();
        assertThat(result.get().getIdElement().getIdPart()).isEqualTo(ALLERGY_ID.toString());
    }

    @Test
    void getAllergyIntoleranceShouldReturnEmptyWhenNotFound() {
        when(allergyIntoleranceRepository.findByTenantIdAndId(TENANT, ALLERGY_ID))
                .thenReturn(Optional.empty());

        Optional<AllergyIntolerance> result = allergyIntoleranceService
                .getAllergyIntolerance(TENANT, ALLERGY_ID.toString());

        assertThat(result).isEmpty();
    }

    @Test
    void getAllergyIntoleranceShouldMapEntityWhenNoStoredResource() {
        AllergyIntoleranceEntity entity = AllergyIntoleranceEntity.builder()
                .id(ALLERGY_ID)
                .tenantId(TENANT)
                .patientId(PATIENT_ID)
                .clinicalStatus("active")
                .verificationStatus("confirmed")
                .type("allergy")
                .category("food")
                .criticality("high")
                .code(ALLERGY_CODE)
                .codeSystem("http://snomed.info/sct")
                .codeDisplay("Fish (substance)")
                .recordedDate(LocalDateTime.now())
                .hasReactions(true)
                .reactionSeverity("mild")
                .fhirResource(null)
                .createdAt(LocalDateTime.now())
                .lastModifiedAt(LocalDateTime.now())
                .version(0)
                .build();
        when(allergyIntoleranceRepository.findByTenantIdAndId(TENANT, ALLERGY_ID))
                .thenReturn(Optional.of(entity));

        Optional<AllergyIntolerance> result = allergyIntoleranceService
                .getAllergyIntolerance(TENANT, ALLERGY_ID.toString());

        assertThat(result).isPresent();
        AllergyIntolerance allergy = result.get();
        assertThat(allergy.getIdElement().getIdPart()).isEqualTo(ALLERGY_ID.toString());
        assertThat(allergy.getClinicalStatus().getCodingFirstRep().getCode()).isEqualTo("active");
        assertThat(allergy.getVerificationStatus().getCodingFirstRep().getCode()).isEqualTo("confirmed");
        assertThat(allergy.getType().toCode()).isEqualTo("allergy");
        assertThat(allergy.getCategory().get(0).getValue().toCode()).isEqualTo("food");
        assertThat(allergy.getCriticality().toCode()).isEqualTo("high");
        assertThat(allergy.getCode().getCodingFirstRep().getCode()).isEqualTo(ALLERGY_CODE);
        assertThat(allergy.getReaction()).hasSize(1);
    }

    @Test
    void updateAllergyIntoleranceShouldPersistChanges() {
        AllergyIntoleranceEntity existing = createAllergyEntity();
        AllergyIntolerance updated = createValidAllergyIntolerance();
        updated.setCriticality(AllergyIntolerance.AllergyIntoleranceCriticality.LOW);

        when(allergyIntoleranceRepository.findByTenantIdAndId(TENANT, ALLERGY_ID))
                .thenReturn(Optional.of(existing));
        when(allergyIntoleranceRepository.save(any(AllergyIntoleranceEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AllergyIntolerance result = allergyIntoleranceService.updateAllergyIntolerance(
                TENANT, ALLERGY_ID.toString(), updated, "user-2");

        assertThat(result).isNotNull();
        verify(allergyIntoleranceRepository).save(any(AllergyIntoleranceEntity.class));
        verify(kafkaTemplate).send(eq("audit-events"), any(String.class));
    }

    @Test
    void updateAllergyIntoleranceShouldThrowWhenNotFound() {
        AllergyIntolerance updated = createValidAllergyIntolerance();
        when(allergyIntoleranceRepository.findByTenantIdAndId(TENANT, ALLERGY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> allergyIntoleranceService.updateAllergyIntolerance(
                TENANT, ALLERGY_ID.toString(), updated, "user-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");

        verify(allergyIntoleranceRepository, never()).save(any());
    }

    @Test
    void deleteAllergyIntoleranceShouldRemoveEntity() {
        AllergyIntoleranceEntity entity = createAllergyEntity();
        when(allergyIntoleranceRepository.findByTenantIdAndId(TENANT, ALLERGY_ID))
                .thenReturn(Optional.of(entity));

        allergyIntoleranceService.deleteAllergyIntolerance(TENANT, ALLERGY_ID.toString(), "user-3");

        verify(allergyIntoleranceRepository).delete(entity);
        verify(kafkaTemplate).send(eq("audit-events"), any(String.class));
    }

    @Test
    void deleteAllergyIntoleranceShouldThrowWhenNotFound() {
        when(allergyIntoleranceRepository.findByTenantIdAndId(TENANT, ALLERGY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> allergyIntoleranceService.deleteAllergyIntolerance(
                TENANT, ALLERGY_ID.toString(), "user-3"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");

        verify(allergyIntoleranceRepository, never()).delete(any());
    }

    @Test
    void getAllergiesByPatientShouldReturnBundle() {
        List<AllergyIntoleranceEntity> entities = List.of(
                createAllergyEntity(),
                createAllergyEntity()
        );
        when(allergyIntoleranceRepository.findByTenantIdAndPatientIdOrderByRecordedDateDesc(
                TENANT, PATIENT_ID))
                .thenReturn(entities);

        Bundle bundle = allergyIntoleranceService.getAllergiesByPatient(
                TENANT, PATIENT_ID.toString(), Pageable.unpaged());

        assertThat(bundle).isNotNull();
        assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.SEARCHSET);
        assertThat(bundle.getTotal()).isEqualTo(2);
        assertThat(bundle.getEntry()).hasSize(2);
    }

    @Test
    void getActiveAllergiesShouldReturnOnlyActiveAllergies() {
        List<AllergyIntoleranceEntity> entities = List.of(createAllergyEntity());
        when(allergyIntoleranceRepository.findActiveAllergiesByPatient(TENANT, PATIENT_ID))
                .thenReturn(entities);

        Bundle bundle = allergyIntoleranceService.getActiveAllergies(TENANT, PATIENT_ID.toString());

        assertThat(bundle).isNotNull();
        assertThat(bundle.getTotal()).isEqualTo(1);
        verify(allergyIntoleranceRepository).findActiveAllergiesByPatient(TENANT, PATIENT_ID);
    }

    @Test
    void getCriticalAllergiesShouldReturnHighCriticalityAllergies() {
        List<AllergyIntoleranceEntity> entities = List.of(createAllergyEntity());
        when(allergyIntoleranceRepository.findCriticalAllergies(TENANT, PATIENT_ID))
                .thenReturn(entities);

        Bundle bundle = allergyIntoleranceService.getCriticalAllergies(TENANT, PATIENT_ID.toString());

        assertThat(bundle).isNotNull();
        assertThat(bundle.getTotal()).isEqualTo(1);
        verify(allergyIntoleranceRepository).findCriticalAllergies(TENANT, PATIENT_ID);
    }

    @Test
    void getMedicationAllergiesShouldReturnMedicationCategory() {
        List<AllergyIntoleranceEntity> entities = List.of(createAllergyEntity());
        when(allergyIntoleranceRepository.findMedicationAllergies(TENANT, PATIENT_ID))
                .thenReturn(entities);

        Bundle bundle = allergyIntoleranceService.getMedicationAllergies(TENANT, PATIENT_ID.toString());

        assertThat(bundle).isNotNull();
        assertThat(bundle.getTotal()).isEqualTo(1);
        verify(allergyIntoleranceRepository).findMedicationAllergies(TENANT, PATIENT_ID);
    }

    @Test
    void getFoodAllergiesShouldReturnFoodCategory() {
        List<AllergyIntoleranceEntity> entities = List.of(createAllergyEntity());
        when(allergyIntoleranceRepository.findFoodAllergies(TENANT, PATIENT_ID))
                .thenReturn(entities);

        Bundle bundle = allergyIntoleranceService.getFoodAllergies(TENANT, PATIENT_ID.toString());

        assertThat(bundle).isNotNull();
        assertThat(bundle.getTotal()).isEqualTo(1);
        verify(allergyIntoleranceRepository).findFoodAllergies(TENANT, PATIENT_ID);
    }

    @Test
    void getAllergiesByCategoryShouldFilterByCategory() {
        String category = "environment";
        List<AllergyIntoleranceEntity> entities = List.of(createAllergyEntity());
        when(allergyIntoleranceRepository.findByCategory(TENANT, PATIENT_ID, category))
                .thenReturn(entities);

        Bundle bundle = allergyIntoleranceService.getAllergiesByCategory(
                TENANT, PATIENT_ID.toString(), category);

        assertThat(bundle).isNotNull();
        assertThat(bundle.getTotal()).isEqualTo(1);
        verify(allergyIntoleranceRepository).findByCategory(TENANT, PATIENT_ID, category);
    }

    @Test
    void getConfirmedAllergiesShouldReturnConfirmedOnly() {
        List<AllergyIntoleranceEntity> entities = List.of(createAllergyEntity());
        when(allergyIntoleranceRepository.findConfirmedAllergies(TENANT, PATIENT_ID))
                .thenReturn(entities);

        Bundle bundle = allergyIntoleranceService.getConfirmedAllergies(TENANT, PATIENT_ID.toString());

        assertThat(bundle).isNotNull();
        assertThat(bundle.getTotal()).isEqualTo(1);
        verify(allergyIntoleranceRepository).findConfirmedAllergies(TENANT, PATIENT_ID);
    }

    @Test
    void hasActiveAllergyShouldReturnTrueWhenAllergyExists() {
        when(allergyIntoleranceRepository.hasActiveAllergy(TENANT, PATIENT_ID, ALLERGY_CODE))
                .thenReturn(true);

        boolean result = allergyIntoleranceService.hasActiveAllergy(
                TENANT, PATIENT_ID.toString(), ALLERGY_CODE);

        assertThat(result).isTrue();
    }

    @Test
    void hasActiveAllergyShouldReturnFalseWhenAllergyDoesNotExist() {
        when(allergyIntoleranceRepository.hasActiveAllergy(TENANT, PATIENT_ID, "NONEXISTENT"))
                .thenReturn(false);

        boolean result = allergyIntoleranceService.hasActiveAllergy(
                TENANT, PATIENT_ID.toString(), "NONEXISTENT");

        assertThat(result).isFalse();
    }

    @Test
    void countActiveAllergiesShouldReturnCount() {
        when(allergyIntoleranceRepository.countActiveAllergies(TENANT, PATIENT_ID))
                .thenReturn(5L);

        long count = allergyIntoleranceService.countActiveAllergies(TENANT, PATIENT_ID.toString());

        assertThat(count).isEqualTo(5L);
    }

    @Test
    void countCriticalAllergiesShouldReturnHighCriticalityCount() {
        when(allergyIntoleranceRepository.countByCriticality(TENANT, PATIENT_ID, "high"))
                .thenReturn(3L);

        long count = allergyIntoleranceService.countCriticalAllergies(TENANT, PATIENT_ID.toString());

        assertThat(count).isEqualTo(3L);
    }

    // Helper methods
    private AllergyIntolerance createValidAllergyIntolerance() {
        AllergyIntolerance allergyIntolerance = new AllergyIntolerance();
        allergyIntolerance.setId(ALLERGY_ID.toString());

        allergyIntolerance.setPatient(new Reference("Patient/" + PATIENT_ID));

        allergyIntolerance.setCode(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://snomed.info/sct")
                        .setCode(ALLERGY_CODE)
                        .setDisplay("Fish (substance)")));

        allergyIntolerance.setClinicalStatus(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-clinical")
                        .setCode("active")));

        allergyIntolerance.setVerificationStatus(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/allergyintolerance-verification")
                        .setCode("confirmed")));

        allergyIntolerance.addCategory(AllergyIntolerance.AllergyIntoleranceCategory.FOOD);
        allergyIntolerance.setCriticality(AllergyIntolerance.AllergyIntoleranceCriticality.HIGH);
        allergyIntolerance.setType(AllergyIntolerance.AllergyIntoleranceType.ALLERGY);

        allergyIntolerance.setRecordedDateElement(new DateTimeType(new java.util.Date()));

        return allergyIntolerance;
    }

    private AllergyIntolerance createAllergyWithReaction() {
        AllergyIntolerance allergyIntolerance = createValidAllergyIntolerance();

        AllergyIntolerance.AllergyIntoleranceReactionComponent reaction =
                new AllergyIntolerance.AllergyIntoleranceReactionComponent();
        reaction.setSeverity(AllergyIntolerance.AllergyIntoleranceSeverity.SEVERE);
        reaction.addManifestation(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://snomed.info/sct")
                        .setCode("39579001")
                        .setDisplay("Anaphylaxis")));

        allergyIntolerance.addReaction(reaction);

        return allergyIntolerance;
    }

    private AllergyIntoleranceEntity createAllergyEntity() {
        return AllergyIntoleranceEntity.builder()
                .id(ALLERGY_ID)
                .tenantId(TENANT)
                .patientId(PATIENT_ID)
                .code(ALLERGY_CODE)
                .codeSystem("http://snomed.info/sct")
                .codeDisplay("Fish (substance)")
                .category("food")
                .clinicalStatus("active")
                .verificationStatus("confirmed")
                .criticality("high")
                .type("allergy")
                .recordedDate(LocalDateTime.now())
                .fhirResource("{\"resourceType\":\"AllergyIntolerance\",\"id\":\"" + ALLERGY_ID + "\"}")
                .createdAt(LocalDateTime.now())
                .lastModifiedAt(LocalDateTime.now())
                .version(0)
                .build();
    }
}
