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
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
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

import com.healthdata.fhir.persistence.ObservationEntity;
import com.healthdata.fhir.persistence.ObservationRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@ExtendWith(MockitoExtension.class)
@DisplayName("Observation Service Tests")
class ObservationServiceTest {

    private static final String TENANT_ID = "tenant-1";
    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);

    @Mock
    private ObservationRepository repository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private ObservationService service;

    @BeforeEach
    void setUp() {
        when(cacheManager.getCache("fhir-observations")).thenReturn(cache);
        service = new ObservationService(repository, kafkaTemplate, cacheManager);
    }

    @Test
    @DisplayName("Should create observation and publish event")
    void shouldCreateObservation() {
        UUID observationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Observation observation = buildObservation(observationId, patientId);
        ObservationEntity savedEntity = buildEntity(observationId, patientId, observation, 0);
        when(repository.save(any(ObservationEntity.class))).thenReturn(savedEntity);

        Observation result = service.createObservation(TENANT_ID, observation, "creator");

        assertThat(result.getIdElement().getIdPart()).isEqualTo(observationId.toString());
        assertThat(result.getMeta().getVersionId()).isEqualTo("0");
        verify(cache).put(eq(cacheKey(observationId)), any(Observation.class));
        verify(kafkaTemplate).send(eq("fhir.observations.created"), eq(observationId.toString()), any());
    }

    @Test
    @DisplayName("Should extract observation values")
    void shouldExtractObservationValues() {
        UUID observationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Observation observation = buildObservation(observationId, patientId);
        when(repository.save(any(ObservationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createObservation(TENANT_ID, observation, "creator");

        ArgumentCaptor<ObservationEntity> captor = ArgumentCaptor.forClass(ObservationEntity.class);
        verify(repository).save(captor.capture());
        ObservationEntity entity = captor.getValue();
        assertThat(entity.getCode()).isEqualTo("8480-6");
        assertThat(entity.getCategory()).isEqualTo("vital-signs");
        assertThat(entity.getValueQuantity()).isEqualTo(120.0);
        assertThat(entity.getValueUnit()).isEqualTo("mmHg");
    }

    @Test
    @DisplayName("Should return cached observation")
    void shouldReturnCachedObservation() {
        UUID observationId = UUID.randomUUID();
        Observation cached = buildObservation(observationId, UUID.randomUUID());
        when(cache.get(cacheKey(observationId), Observation.class)).thenReturn(cached);

        Optional<Observation> result = service.getObservation(TENANT_ID, observationId.toString());

        assertThat(result).contains(cached);
        verify(repository, never()).findByTenantIdAndId(anyString(), any());
    }

    @Test
    @DisplayName("Should load observation and cache it")
    void shouldLoadObservation() {
        UUID observationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Observation observation = buildObservation(observationId, patientId);
        ObservationEntity entity = buildEntity(observationId, patientId, observation, 1);
        when(repository.findByTenantIdAndId(TENANT_ID, observationId)).thenReturn(Optional.of(entity));

        Optional<Observation> result = service.getObservation(TENANT_ID, observationId.toString());

        assertThat(result).isPresent();
        verify(cache).put(eq(cacheKey(observationId)), any(Observation.class));
    }

    @Test
    @DisplayName("Should update observation and publish event")
    void shouldUpdateObservation() {
        UUID observationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Observation observation = buildObservation(observationId, patientId);
        ObservationEntity existing = buildEntity(observationId, patientId, observation, 1);
        ObservationEntity updated = buildEntity(observationId, patientId, observation, 2);

        when(repository.findByTenantIdAndId(TENANT_ID, observationId)).thenReturn(Optional.of(existing));
        when(repository.save(any(ObservationEntity.class))).thenReturn(updated);

        Observation result = service.updateObservation(TENANT_ID, observationId.toString(), observation, "updater");

        assertThat(result.getMeta().getVersionId()).isEqualTo("2");
        verify(cache).put(eq(cacheKey(observationId)), any(Observation.class));
        verify(kafkaTemplate).send(eq("fhir.observations.updated"), eq(observationId.toString()), any());
    }

    @Test
    @DisplayName("Should update observation with string value")
    void shouldUpdateObservationWithStringValue() {
        UUID observationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Observation observation = buildObservation(observationId, patientId);
        observation.setValue(new StringType("normal"));
        ObservationEntity existing = buildEntity(observationId, patientId, observation, 1);
        when(repository.findByTenantIdAndId(TENANT_ID, observationId)).thenReturn(Optional.of(existing));
        when(repository.save(any(ObservationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateObservation(TENANT_ID, observationId.toString(), observation, "updater");

        ArgumentCaptor<ObservationEntity> captor = ArgumentCaptor.forClass(ObservationEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getValueString()).isEqualTo("normal");
    }

    @Test
    @DisplayName("Should map string value on create")
    void shouldMapStringValueOnCreate() {
        UUID observationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Observation observation = buildObservation(observationId, patientId);
        observation.setValue(new StringType("stable"));
        when(repository.save(any(ObservationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createObservation(TENANT_ID, observation, "creator");

        ArgumentCaptor<ObservationEntity> captor = ArgumentCaptor.forClass(ObservationEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getValueString()).isEqualTo("stable");
        assertThat(captor.getValue().getValueQuantity()).isNull();
    }

    @Test
    @DisplayName("Should handle quantity with value but no unit")
    void shouldHandleQuantityWithValueOnly() {
        UUID observationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Observation observation = buildObservation(observationId, patientId);
        observation.setValue(new Quantity().setValue(5));
        when(repository.save(any(ObservationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createObservation(TENANT_ID, observation, "creator");

        ArgumentCaptor<ObservationEntity> captor = ArgumentCaptor.forClass(ObservationEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getValueQuantity()).isEqualTo(5.0);
        assertThat(captor.getValue().getValueUnit()).isNull();
    }

    @Test
    @DisplayName("Should handle quantity with unit but no value")
    void shouldHandleQuantityWithUnitOnly() {
        UUID observationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Observation observation = buildObservation(observationId, patientId);
        observation.setValue(new Quantity().setUnit("mg"));
        when(repository.save(any(ObservationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createObservation(TENANT_ID, observation, "creator");

        ArgumentCaptor<ObservationEntity> captor = ArgumentCaptor.forClass(ObservationEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getValueQuantity()).isNull();
        assertThat(captor.getValue().getValueUnit()).isEqualTo("mg");
    }

    @Test
    @DisplayName("Should delete observation and evict cache")
    void shouldDeleteObservation() {
        UUID observationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Observation observation = buildObservation(observationId, patientId);
        ObservationEntity existing = buildEntity(observationId, patientId, observation, 1);
        when(repository.findByTenantIdAndId(TENANT_ID, observationId)).thenReturn(Optional.of(existing));

        service.deleteObservation(TENANT_ID, observationId.toString(), "deleter");

        verify(repository).delete(existing);
        verify(cache).evict(cacheKey(observationId));
        verify(kafkaTemplate).send(eq("fhir.observations.deleted"), eq(observationId.toString()), any());
    }

    @Test
    @DisplayName("Should search observations by patient")
    void shouldSearchObservationsByPatient() {
        UUID observationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Observation observation = buildObservation(observationId, patientId);
        ObservationEntity entity = buildEntity(observationId, patientId, observation, 0);
        Page<ObservationEntity> page = new PageImpl<>(List.of(entity));
        when(repository.findByTenantIdAndPatientIdOrderByEffectiveDateTimeDesc(
                eq(TENANT_ID), eq(patientId), any(PageRequest.class))).thenReturn(page);

        org.hl7.fhir.r4.model.Bundle bundle = service.searchObservationsByPatient(
                TENANT_ID, patientId.toString(), PageRequest.of(0, 10));

        assertThat(bundle.getTotal()).isEqualTo(1);
        assertThat(bundle.getEntry()).hasSize(1);
    }

    @Test
    @DisplayName("Should search observations by code")
    void shouldSearchObservationsByCode() {
        UUID observationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Observation observation = buildObservation(observationId, patientId);
        ObservationEntity entity = buildEntity(observationId, patientId, observation, 0);
        when(repository.findByTenantIdAndPatientIdAndCodeOrderByEffectiveDateTimeDesc(
                TENANT_ID, patientId, "8480-6")).thenReturn(List.of(entity));

        org.hl7.fhir.r4.model.Bundle bundle = service.searchObservationsByPatientAndCode(
                TENANT_ID, patientId.toString(), "8480-6");

        assertThat(bundle.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should search observations by category")
    void shouldSearchObservationsByCategory() {
        UUID observationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Observation observation = buildObservation(observationId, patientId);
        ObservationEntity entity = buildEntity(observationId, patientId, observation, 0);
        when(repository.findByTenantIdAndPatientIdAndCategoryOrderByEffectiveDateTimeDesc(
                TENANT_ID, patientId, "vital-signs")).thenReturn(List.of(entity));

        org.hl7.fhir.r4.model.Bundle bundle = service.searchObservationsByPatientAndCategory(
                TENANT_ID, patientId.toString(), "vital-signs");

        assertThat(bundle.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should search observations by date range")
    void shouldSearchObservationsByDateRange() {
        UUID observationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Observation observation = buildObservation(observationId, patientId);
        ObservationEntity entity = buildEntity(observationId, patientId, observation, 0);
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        when(repository.findByPatientAndDateRange(TENANT_ID, patientId, start, end)).thenReturn(List.of(entity));

        org.hl7.fhir.r4.model.Bundle bundle = service.searchObservationsByPatientAndDateRange(
                TENANT_ID, patientId.toString(), start, end);

        assertThat(bundle.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return lab results and vital signs")
    void shouldReturnLabAndVitalResults() {
        UUID observationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Observation observation = buildObservation(observationId, patientId);
        ObservationEntity entity = buildEntity(observationId, patientId, observation, 0);

        when(repository.findLabResultsByPatient(TENANT_ID, patientId)).thenReturn(List.of(entity));
        when(repository.findVitalSignsByPatient(TENANT_ID, patientId)).thenReturn(List.of(entity));

        assertThat(service.getLabResultsByPatient(TENANT_ID, patientId.toString()).getEntry()).hasSize(1);
        assertThat(service.getVitalSignsByPatient(TENANT_ID, patientId.toString()).getEntry()).hasSize(1);
    }

    @Test
    @DisplayName("Should return latest observation by patient and code")
    void shouldReturnLatestObservation() {
        UUID observationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Observation observation = buildObservation(observationId, patientId);
        ObservationEntity entity = buildEntity(observationId, patientId, observation, 1);
        when(repository.findLatestByPatientAndCode(TENANT_ID, patientId, "8480-6"))
                .thenReturn(Optional.of(entity));

        Optional<Observation> result = service.getLatestObservationByPatientAndCode(
                TENANT_ID, patientId.toString(), "8480-6");

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Should validate required fields")
    void shouldValidateObservation() {
        Observation observation = new Observation();

        assertThatThrownBy(() -> service.createObservation(TENANT_ID, observation, "creator"))
                .isInstanceOf(ObservationService.ObservationValidationException.class);

        observation.setSubject(new Reference("Patient/" + UUID.randomUUID()));
        assertThatThrownBy(() -> service.createObservation(TENANT_ID, observation, "creator"))
                .isInstanceOf(ObservationService.ObservationValidationException.class);

        observation.setCode(new CodeableConcept().addCoding(new Coding().setCode("8480-6")));
        assertThatThrownBy(() -> service.createObservation(TENANT_ID, observation, "creator"))
                .isInstanceOf(ObservationService.ObservationValidationException.class);
    }

    @Test
    @DisplayName("Should reject invalid observation ID")
    void shouldRejectInvalidObservationId() {
        UUID patientId = UUID.randomUUID();
        Observation observation = buildObservation(UUID.randomUUID(), patientId);
        observation.setId("not-a-uuid");

        assertThatThrownBy(() -> service.createObservation(TENANT_ID, observation, "creator"))
                .isInstanceOf(ObservationService.ObservationValidationException.class);
    }

    @Test
    @DisplayName("Should reject invalid UUID")
    void shouldRejectInvalidUuid() {
        assertThatThrownBy(() -> service.getObservation(TENANT_ID, "not-a-uuid"))
                .isInstanceOf(ObservationService.ObservationValidationException.class);
    }

    @Test
    @DisplayName("Should handle missing cache gracefully")
    void shouldHandleMissingCache() {
        CacheManager noCacheManager = org.mockito.Mockito.mock(CacheManager.class);
        when(noCacheManager.getCache("fhir-observations")).thenReturn(null);
        ObservationService noCacheService = new ObservationService(repository, kafkaTemplate, noCacheManager);

        UUID observationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Observation observation = buildObservation(observationId, patientId);
        ObservationEntity entity = buildEntity(observationId, patientId, observation, 1);
        when(repository.findByTenantIdAndId(TENANT_ID, observationId)).thenReturn(Optional.of(entity));

        Optional<Observation> result = noCacheService.getObservation(TENANT_ID, observationId.toString());

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Should handle category without coding")
    void shouldHandleCategoryWithoutCoding() {
        UUID observationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Observation observation = buildObservation(observationId, patientId);
        observation.getCategory().clear();
        observation.addCategory(new CodeableConcept());
        when(repository.save(any(ObservationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createObservation(TENANT_ID, observation, "creator");

        ArgumentCaptor<ObservationEntity> captor = ArgumentCaptor.forClass(ObservationEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCategory()).isNull();
    }

    @Test
    @DisplayName("Should handle missing effective date time")
    void shouldHandleMissingEffectiveDateTime() {
        UUID observationId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Observation observation = buildObservation(observationId, patientId);
        observation.setEffective(null);
        when(repository.save(any(ObservationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createObservation(TENANT_ID, observation, "creator");

        ArgumentCaptor<ObservationEntity> captor = ArgumentCaptor.forClass(ObservationEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEffectiveDateTime()).isNull();
    }

    private Observation buildObservation(UUID observationId, UUID patientId) {
        Observation observation = new Observation();
        observation.setId(observationId.toString());
        observation.setSubject(new Reference("Patient/" + patientId));
        observation.setStatus(Observation.ObservationStatus.FINAL);
        observation.setCode(new CodeableConcept().addCoding(
                new Coding().setCode("8480-6").setSystem("http://loinc.org").setDisplay("Systolic")));
        observation.addCategory(new CodeableConcept().addCoding(
                new Coding().setCode("vital-signs").setSystem("http://terminology.hl7.org/CodeSystem/observation-category")));
        observation.setEffective(new org.hl7.fhir.r4.model.DateTimeType(Date.from(Instant.parse("2023-05-10T12:00:00Z"))));
        observation.setValue(new Quantity().setValue(120).setUnit("mmHg"));
        return observation;
    }

    private ObservationEntity buildEntity(UUID observationId, UUID patientId, Observation observation, int version) {
        return ObservationEntity.builder()
                .id(observationId)
                .tenantId(TENANT_ID)
                .resourceType("Observation")
                .resourceJson(JSON_PARSER.encodeResourceToString(observation))
                .patientId(patientId)
                .code("8480-6")
                .codeSystem("http://loinc.org")
                .category("vital-signs")
                .status("final")
                .effectiveDateTime(LocalDateTime.ofInstant(Instant.parse("2023-05-10T12:00:00Z"), ZoneId.of("UTC")))
                .valueQuantity(120.0)
                .valueUnit("mmHg")
                .valueString(null)
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(version)
                .build();
    }

    private String cacheKey(UUID observationId) {
        return TENANT_ID + ":obs:" + observationId;
    }
}
