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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.DateTimeType;
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

import com.healthdata.fhir.persistence.ConditionEntity;
import com.healthdata.fhir.persistence.ConditionRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("Condition Service Tests")
class ConditionServiceTest {

    private static final String TENANT_ID = "tenant-1";
    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);

    @Mock
    private ConditionRepository repository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private ObjectMapper objectMapper;

    private ConditionService service;

    @BeforeEach
    void setUp() {
        when(cacheManager.getCache("fhir-conditions")).thenReturn(cache);
        service = new ConditionService(repository, kafkaTemplate, cacheManager, objectMapper);
    }

    @Test
    @DisplayName("Should create condition and publish event")
    void shouldCreateCondition() {
        UUID conditionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Condition condition = buildCondition(conditionId, patientId);
        ConditionEntity savedEntity = buildEntity(conditionId, patientId, condition, 0);
        when(repository.save(any(ConditionEntity.class))).thenReturn(savedEntity);

        Condition result = service.createCondition(TENANT_ID, condition, "creator");

        assertThat(result.getIdElement().getIdPart()).isEqualTo(conditionId.toString());
        assertThat(result.getMeta().getVersionId()).isEqualTo("0");
        verify(cache).put(eq(cacheKey(conditionId)), any(Condition.class));
        verify(kafkaTemplate).send(eq("fhir.conditions.created"), eq(conditionId.toString()), any());
    }

    @Test
    @DisplayName("Should extract condition fields into entity")
    void shouldExtractConditionFields() {
        UUID conditionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Condition condition = buildCondition(conditionId, patientId);
        when(repository.save(any(ConditionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createCondition(TENANT_ID, condition, "creator");

        ArgumentCaptor<ConditionEntity> captor = ArgumentCaptor.forClass(ConditionEntity.class);
        verify(repository).save(captor.capture());
        ConditionEntity entity = captor.getValue();
        assertThat(entity.getCode()).isEqualTo("E11.9");
        assertThat(entity.getCodeSystem()).isEqualTo("http://hl7.org/fhir/sid/icd-10");
        assertThat(entity.getCategory()).isEqualTo("encounter-diagnosis");
        assertThat(entity.getClinicalStatus()).isEqualTo("active");
        assertThat(entity.getVerificationStatus()).isEqualTo("confirmed");
        assertThat(entity.getSeverity()).isEqualTo("severe");
        assertThat(entity.getOnsetDate()).isEqualTo(LocalDate.of(2023, 1, 10));
        assertThat(entity.getAbatementDate()).isEqualTo(LocalDate.of(2023, 2, 10));
    }

    @Test
    @DisplayName("Should return cached condition")
    void shouldReturnCachedCondition() {
        UUID conditionId = UUID.randomUUID();
        Condition cached = buildCondition(conditionId, UUID.randomUUID());
        when(cache.get(cacheKey(conditionId), Condition.class)).thenReturn(cached);

        Optional<Condition> result = service.getCondition(TENANT_ID, conditionId.toString());

        assertThat(result).contains(cached);
        verify(repository, never()).findByTenantIdAndId(anyString(), any());
    }

    @Test
    @DisplayName("Should load condition and cache it")
    void shouldLoadCondition() {
        UUID conditionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Condition condition = buildCondition(conditionId, patientId);
        ConditionEntity entity = buildEntity(conditionId, patientId, condition, 1);
        when(repository.findByTenantIdAndId(TENANT_ID, conditionId)).thenReturn(Optional.of(entity));

        Optional<Condition> result = service.getCondition(TENANT_ID, conditionId.toString());

        assertThat(result).isPresent();
        verify(cache).put(eq(cacheKey(conditionId)), any(Condition.class));
    }

    @Test
    @DisplayName("Should update condition and publish event")
    void shouldUpdateCondition() {
        UUID conditionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Condition condition = buildCondition(conditionId, patientId);
        ConditionEntity existing = buildEntity(conditionId, patientId, condition, 1);
        ConditionEntity updated = buildEntity(conditionId, patientId, condition, 2);

        when(repository.findByTenantIdAndId(TENANT_ID, conditionId)).thenReturn(Optional.of(existing));
        when(repository.save(any(ConditionEntity.class))).thenReturn(updated);

        Condition result = service.updateCondition(TENANT_ID, conditionId.toString(), condition, "updater");

        assertThat(result.getMeta().getVersionId()).isEqualTo("2");
        verify(cache).put(eq(cacheKey(conditionId)), any(Condition.class));
        verify(kafkaTemplate).send(eq("fhir.conditions.updated"), eq(conditionId.toString()), any());
    }

    @Test
    @DisplayName("Should delete condition and evict cache")
    void shouldDeleteCondition() {
        UUID conditionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Condition condition = buildCondition(conditionId, patientId);
        ConditionEntity existing = buildEntity(conditionId, patientId, condition, 1);
        when(repository.findByTenantIdAndId(TENANT_ID, conditionId)).thenReturn(Optional.of(existing));

        service.deleteCondition(TENANT_ID, conditionId.toString(), "deleter");

        verify(repository).delete(existing);
        verify(cache).evict(cacheKey(conditionId));
        verify(kafkaTemplate).send(eq("fhir.conditions.deleted"), eq(conditionId.toString()), any());
    }

    @Test
    @DisplayName("Should search conditions by patient")
    void shouldSearchConditionsByPatient() {
        UUID conditionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Condition condition = buildCondition(conditionId, patientId);
        ConditionEntity entity = buildEntity(conditionId, patientId, condition, 0);
        Page<ConditionEntity> page = new PageImpl<>(List.of(entity));
        when(repository.findByTenantIdAndPatientIdOrderByRecordedDateDesc(
                eq(TENANT_ID), eq(patientId), any(PageRequest.class))).thenReturn(page);

        org.hl7.fhir.r4.model.Bundle bundle = service.searchConditionsByPatient(
                TENANT_ID, patientId.toString(), PageRequest.of(0, 10));

        assertThat(bundle.getTotal()).isEqualTo(1);
        assertThat(bundle.getEntry()).hasSize(1);
    }

    @Test
    @DisplayName("Should search conditions by code")
    void shouldSearchConditionsByCode() {
        UUID conditionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Condition condition = buildCondition(conditionId, patientId);
        ConditionEntity entity = buildEntity(conditionId, patientId, condition, 0);
        when(repository.findByTenantIdAndPatientIdAndCodeOrderByRecordedDateDesc(
                TENANT_ID, patientId, "E11.9")).thenReturn(List.of(entity));

        org.hl7.fhir.r4.model.Bundle bundle = service.searchConditionsByPatientAndCode(
                TENANT_ID, patientId.toString(), "E11.9");

        assertThat(bundle.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should search conditions by category")
    void shouldSearchConditionsByCategory() {
        UUID conditionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Condition condition = buildCondition(conditionId, patientId);
        ConditionEntity entity = buildEntity(conditionId, patientId, condition, 0);
        when(repository.findByTenantIdAndPatientIdAndCategoryOrderByRecordedDateDesc(
                TENANT_ID, patientId, "encounter-diagnosis")).thenReturn(List.of(entity));

        org.hl7.fhir.r4.model.Bundle bundle = service.searchConditionsByPatientAndCategory(
                TENANT_ID, patientId.toString(), "encounter-diagnosis");

        assertThat(bundle.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should fetch active/chronic/diagnosis/problem list bundles")
    void shouldFetchConditionSubsets() {
        UUID patientId = UUID.randomUUID();
        Condition condition = buildCondition(UUID.randomUUID(), patientId);
        ConditionEntity entity = buildEntity(UUID.randomUUID(), patientId, condition, 0);

        when(repository.findActiveConditionsByPatient(TENANT_ID, patientId)).thenReturn(List.of(entity));
        when(repository.findChronicConditionsByPatient(TENANT_ID, patientId)).thenReturn(List.of(entity));
        when(repository.findDiagnosesByPatient(TENANT_ID, patientId)).thenReturn(List.of(entity));
        when(repository.findProblemListByPatient(TENANT_ID, patientId)).thenReturn(List.of(entity));

        assertThat(service.getActiveConditionsByPatient(TENANT_ID, patientId.toString()).getEntry()).hasSize(1);
        assertThat(service.getChronicConditionsByPatient(TENANT_ID, patientId.toString()).getEntry()).hasSize(1);
        assertThat(service.getDiagnosesByPatient(TENANT_ID, patientId.toString()).getEntry()).hasSize(1);
        assertThat(service.getProblemListByPatient(TENANT_ID, patientId.toString()).getEntry()).hasSize(1);
    }

    @Test
    @DisplayName("Should default recorded date when missing")
    void shouldDefaultRecordedDateWhenMissing() {
        UUID conditionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Condition condition = buildCondition(conditionId, patientId);
        condition.setRecordedDate(null);

        when(repository.save(any(ConditionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createCondition(TENANT_ID, condition, "creator");

        ArgumentCaptor<ConditionEntity> captor = ArgumentCaptor.forClass(ConditionEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getRecordedDate()).isNotNull();
    }

    @Test
    @DisplayName("Should check active condition")
    void shouldCheckActiveCondition() {
        UUID patientId = UUID.randomUUID();
        when(repository.hasActiveCondition(TENANT_ID, patientId, "E11.9")).thenReturn(true);

        boolean result = service.hasActiveCondition(TENANT_ID, patientId.toString(), "E11.9");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should validate required fields")
    void shouldValidateCondition() {
        Condition condition = new Condition();

        assertThatThrownBy(() -> service.createCondition(TENANT_ID, condition, "creator"))
                .isInstanceOf(ConditionService.ConditionValidationException.class);

        condition.setSubject(new Reference("Patient/" + UUID.randomUUID()));
        assertThatThrownBy(() -> service.createCondition(TENANT_ID, condition, "creator"))
                .isInstanceOf(ConditionService.ConditionValidationException.class);
    }

    @Test
    @DisplayName("Should reject invalid UUID")
    void shouldRejectInvalidUuid() {
        assertThatThrownBy(() -> service.getCondition(TENANT_ID, "not-a-uuid"))
                .isInstanceOf(ConditionService.ConditionValidationException.class);
    }

    @Test
    @DisplayName("Should handle missing cache gracefully")
    void shouldHandleMissingCache() {
        CacheManager noCacheManager = org.mockito.Mockito.mock(CacheManager.class);
        when(noCacheManager.getCache("fhir-conditions")).thenReturn(null);
        ConditionService noCacheService = new ConditionService(repository, kafkaTemplate, noCacheManager, objectMapper);

        UUID conditionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Condition condition = buildCondition(conditionId, patientId);
        ConditionEntity entity = buildEntity(conditionId, patientId, condition, 1);
        when(repository.findByTenantIdAndId(TENANT_ID, conditionId)).thenReturn(Optional.of(entity));

        Optional<Condition> result = noCacheService.getCondition(TENANT_ID, conditionId.toString());

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Should handle missing code and category coding")
    void shouldHandleMissingCodeAndCategoryCoding() {
        UUID conditionId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Condition condition = new Condition();
        condition.setId(conditionId.toString());
        condition.setSubject(new Reference("Patient/" + patientId));
        condition.setCode(new CodeableConcept().setText("no-coding"));
        condition.addCategory(new CodeableConcept().setText("no-coding"));

        when(repository.save(any(ConditionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Condition result = service.createCondition(TENANT_ID, condition, "creator");

        ArgumentCaptor<ConditionEntity> captor = ArgumentCaptor.forClass(ConditionEntity.class);
        verify(repository).save(captor.capture());
        ConditionEntity entity = captor.getValue();
        assertThat(entity.getCode()).isNull();
        assertThat(entity.getCodeSystem()).isNull();
        assertThat(entity.getCodeDisplay()).isNull();
        assertThat(entity.getCategory()).isNull();
        assertThat(result.getIdElement().getIdPart()).isEqualTo(conditionId.toString());
    }

    private Condition buildCondition(UUID conditionId, UUID patientId) {
        Condition condition = new Condition();
        condition.setId(conditionId.toString());
        condition.setSubject(new Reference("Patient/" + patientId));
        condition.setCode(new CodeableConcept().addCoding(
                new Coding().setCode("E11.9").setSystem("http://hl7.org/fhir/sid/icd-10").setDisplay("Diabetes")));
        condition.addCategory(new CodeableConcept().addCoding(
                new Coding().setCode("encounter-diagnosis").setSystem("http://terminology.hl7.org/CodeSystem/condition-category")));
        condition.setClinicalStatus(new CodeableConcept().addCoding(new Coding().setCode("active")));
        condition.setVerificationStatus(new CodeableConcept().addCoding(new Coding().setCode("confirmed")));
        condition.setSeverity(new CodeableConcept().addCoding(new Coding().setCode("severe")));
        condition.setOnset(new DateTimeType(Date.from(Instant.parse("2023-01-10T00:00:00Z"))));
        condition.setAbatement(new DateTimeType(Date.from(Instant.parse("2023-02-10T00:00:00Z"))));
        condition.setRecordedDate(Date.from(Instant.parse("2023-03-10T00:00:00Z")));
        return condition;
    }

    private ConditionEntity buildEntity(UUID conditionId, UUID patientId, Condition condition, int version) {
        return ConditionEntity.builder()
                .id(conditionId)
                .tenantId(TENANT_ID)
                .resourceType("Condition")
                .resourceJson(JSON_PARSER.encodeResourceToString(condition))
                .patientId(patientId)
                .code("E11.9")
                .codeSystem("http://hl7.org/fhir/sid/icd-10")
                .codeDisplay("Diabetes")
                .category("encounter-diagnosis")
                .clinicalStatus("active")
                .verificationStatus("confirmed")
                .severity("severe")
                .onsetDate(LocalDate.of(2023, 1, 10))
                .abatementDate(LocalDate.of(2023, 2, 10))
                .recordedDate(LocalDate.ofInstant(Instant.parse("2023-03-10T00:00:00Z"), ZoneId.of("UTC")))
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(version)
                .build();
    }

    private String cacheKey(UUID conditionId) {
        return TENANT_ID + ":cond:" + conditionId;
    }
}
