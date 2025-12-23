package com.healthdata.fhir.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.MedicationAdministration;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;

import com.healthdata.fhir.persistence.MedicationAdministrationEntity;
import com.healthdata.fhir.persistence.MedicationAdministrationRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicationAdministration Service Tests")
class MedicationAdministrationServiceTest {

    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);
    private static final String TENANT_ID = "tenant-1";
    private static final String CREATED_BY = "tester";

    @Mock
    private MedicationAdministrationRepository repository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private MedicationAdministrationService service;

    @BeforeEach
    void setUp() {
        when(cacheManager.getCache("fhir-medication-administrations")).thenReturn(cache);
        service = new MedicationAdministrationService(repository, kafkaTemplate, cacheManager);
    }

    @Test
    @DisplayName("Should create medication administration and publish event")
    void shouldCreateMedicationAdministration() {
        MedicationAdministration administration = buildMedicationAdministration();
        UUID id = UUID.fromString(administration.getIdElement().getIdPart());
        UUID patientId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        MedicationAdministrationEntity saved = buildEntity(id, patientId, administration);
        when(repository.save(any(MedicationAdministrationEntity.class))).thenReturn(saved);

        MedicationAdministration result =
                service.createMedicationAdministration(TENANT_ID, administration, CREATED_BY);

        assertThat(result.getIdElement().getIdPart()).isEqualTo(id.toString());
        assertThat(result.getMeta().getVersionId()).isEqualTo("0");
        verify(cache).put(eq(cacheKey(id)), any(MedicationAdministration.class));
        verify(kafkaTemplate).send(eq("fhir.medication-administrations.created"), eq(id.toString()), any());
    }

    @Test
    @DisplayName("Should return cached medication administration")
    void shouldReturnCachedMedicationAdministration() {
        UUID id = UUID.randomUUID();
        MedicationAdministration cached = buildMedicationAdministration();
        cached.setId(id.toString());
        when(cache.get(cacheKey(id), MedicationAdministration.class)).thenReturn(cached);

        Optional<MedicationAdministration> result =
                service.getMedicationAdministration(TENANT_ID, id.toString());

        assertThat(result).contains(cached);
        verify(repository, never()).findByTenantIdAndId(anyString(), any());
    }

    @Test
    @DisplayName("Should load medication administration and cache it")
    void shouldLoadMedicationAdministration() {
        UUID id = UUID.randomUUID();
        UUID patientId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        MedicationAdministration administration = buildMedicationAdministration();
        administration.setId(id.toString());
        MedicationAdministrationEntity entity = buildEntity(id, patientId, administration);
        when(repository.findByTenantIdAndId(TENANT_ID, id)).thenReturn(Optional.of(entity));

        Optional<MedicationAdministration> result =
                service.getMedicationAdministration(TENANT_ID, id.toString());

        assertThat(result).isPresent();
        verify(cache).put(eq(cacheKey(id)), any(MedicationAdministration.class));
    }

    @Test
    @DisplayName("Should update medication administration and publish event")
    void shouldUpdateMedicationAdministration() {
        MedicationAdministration administration = buildMedicationAdministration();
        UUID id = UUID.fromString(administration.getIdElement().getIdPart());
        UUID patientId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        MedicationAdministrationEntity entity = buildEntity(id, patientId, administration);
        MedicationAdministrationEntity updated = entity.toBuilder().version(2).build();

        when(repository.findByTenantIdAndId(TENANT_ID, id)).thenReturn(Optional.of(entity));
        when(repository.save(any(MedicationAdministrationEntity.class))).thenReturn(updated);

        MedicationAdministration result =
                service.updateMedicationAdministration(TENANT_ID, id.toString(), administration, "updater");

        assertThat(result.getMeta().getVersionId()).isEqualTo("2");
        verify(cache).put(eq(cacheKey(id)), any(MedicationAdministration.class));
        verify(kafkaTemplate).send(eq("fhir.medication-administrations.updated"), eq(id.toString()), any());
    }

    @Test
    @DisplayName("Should throw when updating missing administration")
    void shouldThrowWhenUpdatingMissingAdministration() {
        MedicationAdministration administration = buildMedicationAdministration();
        UUID id = UUID.fromString(administration.getIdElement().getIdPart());
        when(repository.findByTenantIdAndId(TENANT_ID, id)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.updateMedicationAdministration(TENANT_ID, id.toString(), administration, "updater"))
                .isInstanceOf(MedicationAdministrationService.MedicationAdministrationNotFoundException.class);
    }

    @Test
    @DisplayName("Should delete medication administration and evict cache")
    void shouldDeleteMedicationAdministration() {
        MedicationAdministration administration = buildMedicationAdministration();
        UUID id = UUID.fromString(administration.getIdElement().getIdPart());
        UUID patientId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        MedicationAdministrationEntity entity = buildEntity(id, patientId, administration);
        when(repository.findByTenantIdAndId(TENANT_ID, id)).thenReturn(Optional.of(entity));

        service.deleteMedicationAdministration(TENANT_ID, id.toString(), "deleter");

        verify(repository).delete(entity);
        verify(cache).evict(cacheKey(id));
        verify(kafkaTemplate).send(eq("fhir.medication-administrations.deleted"), eq(id.toString()), any());
    }

    @Test
    @DisplayName("Should throw when deleting missing administration")
    void shouldThrowWhenDeletingMissingAdministration() {
        UUID id = UUID.randomUUID();
        when(repository.findByTenantIdAndId(TENANT_ID, id)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.deleteMedicationAdministration(TENANT_ID, id.toString(), "deleter"))
                .isInstanceOf(MedicationAdministrationService.MedicationAdministrationNotFoundException.class);
    }

    @Test
    @DisplayName("Should search administrations by patient and build bundle")
    void shouldSearchAdministrationsByPatient() {
        UUID patientId = UUID.randomUUID();
        MedicationAdministration administration = buildMedicationAdministration();
        UUID id = UUID.fromString(administration.getIdElement().getIdPart());
        MedicationAdministrationEntity entity = buildEntity(id, patientId, administration);
        Page<MedicationAdministrationEntity> page = new PageImpl<>(List.of(entity));
        when(repository.findByTenantIdAndPatientIdOrderByEffectiveDateTimeDesc(
                eq(TENANT_ID), eq(patientId), any(PageRequest.class))).thenReturn(page);

        org.hl7.fhir.r4.model.Bundle bundle = service.searchAdministrationsByPatient(
                TENANT_ID, patientId.toString(), PageRequest.of(0, 10));

        assertThat(bundle.getTotal()).isEqualTo(1);
        assertThat(bundle.getEntry()).hasSize(1);
    }

    @Test
    @DisplayName("Should query administrations by code and by encounter")
    void shouldQueryAdministrationsByCodeAndEncounter() {
        UUID patientId = UUID.randomUUID();
        UUID encounterId = UUID.randomUUID();
        MedicationAdministration administration = buildMedicationAdministration();
        UUID id = UUID.fromString(administration.getIdElement().getIdPart());
        MedicationAdministrationEntity entity = buildEntity(id, patientId, administration);

        when(repository.findByTenantIdAndPatientIdAndMedicationCodeOrderByEffectiveDateTimeDesc(
                TENANT_ID, patientId, "med-1")).thenReturn(List.of(entity));
        when(repository.findByTenantIdAndEncounterIdOrderByEffectiveDateTimeDesc(
                TENANT_ID, encounterId)).thenReturn(List.of(entity));

        assertThat(service.searchAdministrationsByPatientAndCode(
                TENANT_ID, patientId.toString(), "med-1").getEntry()).hasSize(1);
        assertThat(service.searchAdministrationsByEncounter(
                TENANT_ID, encounterId.toString()).getEntry()).hasSize(1);
    }

    @Test
    @DisplayName("Should fetch administration subsets")
    void shouldFetchAdministrationSubsets() {
        UUID patientId = UUID.randomUUID();
        MedicationAdministration administration = buildMedicationAdministration();
        UUID id = UUID.fromString(administration.getIdElement().getIdPart());
        MedicationAdministrationEntity entity = buildEntity(id, patientId, administration);

        when(repository.findCompletedAdministrationsByPatient(TENANT_ID, patientId)).thenReturn(List.of(entity));
        when(repository.findInProgressAdministrationsByPatient(TENANT_ID, patientId)).thenReturn(List.of(entity));
        when(repository.findByPatientAndDateRange(eq(TENANT_ID), eq(patientId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(entity));
        when(repository.findAdministrationHistoryByRequest(eq(TENANT_ID), any(UUID.class))).thenReturn(List.of(entity));
        when(repository.findByTenantIdAndLotNumberOrderByEffectiveDateTimeDesc(TENANT_ID, "LOT-1"))
                .thenReturn(List.of(entity));

        assertThat(service.getCompletedAdministrationsByPatient(TENANT_ID, patientId.toString()).getEntry()).hasSize(1);
        assertThat(service.getInProgressAdministrationsByPatient(TENANT_ID, patientId.toString()).getEntry()).hasSize(1);
        assertThat(service.getAdministrationsByDateRange(TENANT_ID, patientId.toString(),
                LocalDateTime.now().minusDays(1), LocalDateTime.now()).getEntry()).hasSize(1);
        assertThat(service.getAdministrationHistoryByRequest(TENANT_ID, UUID.randomUUID().toString()).getEntry()).hasSize(1);
        assertThat(service.getAdministrationsByLotNumber(TENANT_ID, "LOT-1").getEntry()).hasSize(1);
    }

    @Test
    @DisplayName("Should extract period and handle invalid references")
    void shouldExtractPeriodAndIgnoreInvalidReferences() {
        MedicationAdministration administration = buildMedicationAdministration();
        administration.setRequest(new Reference("MedicationRequest/not-a-uuid"));
        administration.setContext(new Reference("Encounter/not-a-uuid"));
        Period period = new Period();
        period.setStart(Date.from(Instant.now().minusSeconds(3600)));
        period.setEnd(Date.from(Instant.now()));
        administration.setEffective(period);
        UUID id = UUID.fromString(administration.getIdElement().getIdPart());
        when(repository.save(any(MedicationAdministrationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createMedicationAdministration(TENANT_ID, administration, CREATED_BY);

        ArgumentCaptor<MedicationAdministrationEntity> captor = ArgumentCaptor.forClass(MedicationAdministrationEntity.class);
        verify(repository).save(captor.capture());
        MedicationAdministrationEntity entity = captor.getValue();
        assertThat(entity.getEncounterId()).isNull();
        assertThat(entity.getMedicationRequestId()).isNull();
        assertThat(entity.getEffectivePeriodStart()).isNotNull();
        assertThat(entity.getEffectivePeriodEnd()).isNotNull();
    }

    @Test
    @DisplayName("Should return true when medication administered today")
    void shouldReturnMedicationAdministeredToday() {
        UUID patientId = UUID.randomUUID();
        when(repository.hasMedicationBeenAdministeredToday(
                eq(TENANT_ID), eq(patientId), eq("med-1"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        assertThat(service.hasMedicationBeenAdministeredToday(TENANT_ID, patientId.toString(), "med-1")).isTrue();
    }

    @Test
    @DisplayName("Should validate required fields")
    void shouldValidateRequiredFields() {
        MedicationAdministration administration = new MedicationAdministration();

        assertThatThrownBy(() ->
                service.createMedicationAdministration(TENANT_ID, administration, CREATED_BY))
                .isInstanceOf(MedicationAdministrationService.MedicationAdministrationValidationException.class);

        administration.setSubject(new Reference("Patient/" + UUID.randomUUID()));
        assertThatThrownBy(() ->
                service.createMedicationAdministration(TENANT_ID, administration, CREATED_BY))
                .isInstanceOf(MedicationAdministrationService.MedicationAdministrationValidationException.class);

        administration.setMedication(new CodeableConcept().addCoding(new Coding().setCode("med-1")));
        assertThatThrownBy(() ->
                service.createMedicationAdministration(TENANT_ID, administration, CREATED_BY))
                .isInstanceOf(MedicationAdministrationService.MedicationAdministrationValidationException.class);

        administration.setStatus(MedicationAdministration.MedicationAdministrationStatus.COMPLETED);
        assertThatThrownBy(() ->
                service.createMedicationAdministration(TENANT_ID, administration, CREATED_BY))
                .isInstanceOf(MedicationAdministrationService.MedicationAdministrationValidationException.class);
    }

    @Test
    @DisplayName("Should reject invalid UUIDs")
    void shouldRejectInvalidUuid() {
        assertThatThrownBy(() -> service.getMedicationAdministration(TENANT_ID, "not-a-uuid"))
                .isInstanceOf(MedicationAdministrationService.MedicationAdministrationValidationException.class);
    }

    private MedicationAdministration buildMedicationAdministration() {
        MedicationAdministration administration = new MedicationAdministration();
        administration.setId(UUID.randomUUID().toString());
        administration.setSubject(new Reference("Patient/11111111-1111-1111-1111-111111111111"));
        administration.setStatus(MedicationAdministration.MedicationAdministrationStatus.COMPLETED);
        administration.setEffective(new DateTimeType(Date.from(Instant.now())));
        administration.setMedication(new CodeableConcept()
                .addCoding(new Coding()
                        .setCode("med-1")
                        .setSystem("http://example.com")
                        .setDisplay("Medication One")));

        MedicationAdministration.MedicationAdministrationDosageComponent dosage =
                new MedicationAdministration.MedicationAdministrationDosageComponent();
        dosage.setDose(new Quantity().setValue(5).setUnit("mg"));
        dosage.setRate(new Quantity().setValue(2).setUnit("mg/h"));
        dosage.setRoute(new CodeableConcept().addCoding(new Coding().setCode("IV").setDisplay("IV")));
        dosage.setSite(new CodeableConcept().addCoding(new Coding().setCode("ARM").setDisplay("Arm")));
        administration.setDosage(dosage);

        MedicationAdministration.MedicationAdministrationPerformerComponent performer =
                new MedicationAdministration.MedicationAdministrationPerformerComponent();
        performer.setActor(new Reference("Practitioner/123"));
        administration.addPerformer(performer);

        administration.addReasonCode(new CodeableConcept().addCoding(new Coding().setCode("reason")));
        administration.setCategory(new CodeableConcept().addCoding(new Coding().setCode("inpatient")));
        administration.setRequest(new Reference("MedicationRequest/" + UUID.randomUUID()));
        administration.setContext(new Reference("Encounter/" + UUID.randomUUID()));
        return administration;
    }

    private MedicationAdministrationEntity buildEntity(UUID id, UUID patientId,
                                                      MedicationAdministration administration) {
        return MedicationAdministrationEntity.builder()
                .id(id)
                .tenantId(TENANT_ID)
                .resourceType("MedicationAdministration")
                .resourceJson(JSON_PARSER.encodeResourceToString(administration))
                .patientId(patientId)
                .status("completed")
                .effectiveDateTime(LocalDateTime.now(ZoneId.of("UTC")))
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
    }

    private String cacheKey(UUID id) {
        return TENANT_ID + ":medadmin:" + id;
    }
}
