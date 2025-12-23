package com.healthdata.fhir.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;

import com.healthdata.fhir.persistence.EncounterEntity;
import com.healthdata.fhir.persistence.EncounterRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@ExtendWith(MockitoExtension.class)
@DisplayName("Encounter Service Tests")
class EncounterServiceTest {

    private static final String TENANT_ID = "tenant-1";
    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);

    @Mock
    private EncounterRepository repository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    @DisplayName("Should create encounter and publish event")
    void shouldCreateEncounter() {
        EncounterService service = new EncounterService(repository, kafkaTemplate);
        UUID encounterId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Encounter encounter = buildEncounter(encounterId, patientId);
        EncounterEntity saved = buildEntity(encounterId, patientId, encounter);
        when(repository.save(any(EncounterEntity.class))).thenReturn(saved);

        Encounter result = service.createEncounter(TENANT_ID, encounter, "creator");

        assertThat(result.getIdElement().getIdPart()).isEqualTo(encounterId.toString());
        verify(kafkaTemplate).send(eq("fhir.encounters.created"), eq(TENANT_ID + ":" + encounterId), anyString());
    }

    @Test
    @DisplayName("Should extract encounter fields")
    void shouldExtractEncounterFields() {
        EncounterService service = new EncounterService(repository, kafkaTemplate);
        UUID encounterId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Encounter encounter = buildEncounter(encounterId, patientId);
        when(repository.save(any(EncounterEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createEncounter(TENANT_ID, encounter, "creator");

        ArgumentCaptor<EncounterEntity> captor = ArgumentCaptor.forClass(EncounterEntity.class);
        verify(repository).save(captor.capture());
        EncounterEntity entity = captor.getValue();
        assertThat(entity.getEncounterClass()).isEqualTo("IMP");
        assertThat(entity.getEncounterTypeCode()).isEqualTo("AMB");
        assertThat(entity.getStatus()).isEqualTo("finished");
        assertThat(entity.getServiceTypeCode()).isEqualTo("service");
        assertThat(entity.getPriority()).isEqualTo("urgent");
        assertThat(entity.getDurationMinutes()).isEqualTo(60);
        assertThat(entity.getReasonCode()).isEqualTo("R1");
        assertThat(entity.getLocationId()).isEqualTo("Location/loc1");
        assertThat(entity.getParticipantId()).isEqualTo("Practitioner/abc");
        assertThat(entity.getServiceProviderId()).isEqualTo("Organization/org1");
        assertThat(entity.getAdmissionSource()).isEqualTo("ER");
        assertThat(entity.getDischargeDisposition()).isEqualTo("home");
    }

    @Test
    @DisplayName("Should return empty for invalid UUID")
    void shouldReturnEmptyForInvalidUuid() {
        EncounterService service = new EncounterService(repository, kafkaTemplate);

        Optional<Encounter> result = service.getEncounter(TENANT_ID, "not-a-uuid");

        assertThat(result).isEmpty();
        verify(repository, never()).findByTenantIdAndId(anyString(), any());
    }

    @Test
    @DisplayName("Should reject missing patient reference")
    void shouldRejectMissingPatientReference() {
        EncounterService service = new EncounterService(repository, kafkaTemplate);
        Encounter encounter = new Encounter();
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> service.createEncounter(TENANT_ID, encounter, "creator"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("patient reference");
    }

    @Test
    @DisplayName("Should assign ID when missing")
    void shouldAssignIdWhenMissing() {
        EncounterService service = new EncounterService(repository, kafkaTemplate);
        UUID patientId = UUID.randomUUID();
        Encounter encounter = new Encounter();
        encounter.setSubject(new Reference("Patient/" + patientId));
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);
        when(repository.save(any(EncounterEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Encounter result = service.createEncounter(TENANT_ID, encounter, "creator");

        assertThat(result.hasId()).isTrue();
        verify(repository).save(any(EncounterEntity.class));
    }

    @Test
    @DisplayName("Should update encounter and publish event")
    void shouldUpdateEncounter() {
        EncounterService service = new EncounterService(repository, kafkaTemplate);
        UUID encounterId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Encounter encounter = buildEncounter(encounterId, patientId);
        EncounterEntity existing = buildEntity(encounterId, patientId, encounter);
        EncounterEntity updated = buildEntity(encounterId, patientId, encounter);

        when(repository.findByTenantIdAndId(TENANT_ID, encounterId)).thenReturn(Optional.of(existing));
        when(repository.save(any(EncounterEntity.class))).thenReturn(updated);

        Encounter result = service.updateEncounter(TENANT_ID, encounterId.toString(), encounter, "updater");

        assertThat(result.getIdElement().getIdPart()).isEqualTo(encounterId.toString());
        verify(kafkaTemplate).send(eq("fhir.encounters.updated"), eq(TENANT_ID + ":" + encounterId), anyString());
    }

    @Test
    @DisplayName("Should delete encounter and publish event")
    void shouldDeleteEncounter() {
        EncounterService service = new EncounterService(repository, kafkaTemplate);
        UUID encounterId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Encounter encounter = buildEncounter(encounterId, patientId);
        EncounterEntity existing = buildEntity(encounterId, patientId, encounter);
        when(repository.findByTenantIdAndId(TENANT_ID, encounterId)).thenReturn(Optional.of(existing));

        service.deleteEncounter(TENANT_ID, encounterId.toString(), "deleter");

        verify(repository).delete(existing);
        verify(kafkaTemplate).send(eq("fhir.encounters.deleted"), eq(TENANT_ID + ":" + encounterId), anyString());
    }

    @Test
    @DisplayName("Should handle encounter with minimal optional fields")
    void shouldHandleMinimalOptionalFields() {
        EncounterService service = new EncounterService(repository, kafkaTemplate);
        UUID encounterId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Encounter encounter = new Encounter();
        encounter.setId(encounterId.toString());
        encounter.setSubject(new Reference(patientId.toString()));
        encounter.setStatus(Encounter.EncounterStatus.PLANNED);
        encounter.setPeriod(new Period().setStart(Date.from(Instant.parse("2024-01-01T00:00:00Z"))));
        when(repository.save(any(EncounterEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createEncounter(TENANT_ID, encounter, "creator");

        ArgumentCaptor<EncounterEntity> captor = ArgumentCaptor.forClass(EncounterEntity.class);
        verify(repository).save(captor.capture());
        EncounterEntity entity = captor.getValue();
        assertThat(entity.getEncounterClass()).isNull();
        assertThat(entity.getEncounterTypeCode()).isNull();
        assertThat(entity.getServiceTypeCode()).isNull();
        assertThat(entity.getPriority()).isNull();
        assertThat(entity.getDurationMinutes()).isNull();
        assertThat(entity.getPeriodStart()).isNotNull();
        assertThat(entity.getPeriodEnd()).isNull();
    }

    @Test
    @DisplayName("Should ignore publish failures")
    void shouldIgnorePublishFailures() {
        EncounterService service = new EncounterService(repository, kafkaTemplate);
        UUID encounterId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Encounter encounter = buildEncounter(encounterId, patientId);
        EncounterEntity saved = buildEntity(encounterId, patientId, encounter);
        when(repository.save(any(EncounterEntity.class))).thenReturn(saved);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("kafka down"));

        Encounter result = service.createEncounter(TENANT_ID, encounter, "creator");

        assertThat(result).isNotNull();
        verify(repository).save(any(EncounterEntity.class));
    }

    @Test
    @DisplayName("Should search encounters by patient")
    void shouldSearchEncountersByPatient() {
        EncounterService service = new EncounterService(repository, kafkaTemplate);
        UUID encounterId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Encounter encounter = buildEncounter(encounterId, patientId);
        EncounterEntity entity = buildEntity(encounterId, patientId, encounter);
        Page<EncounterEntity> page = new PageImpl<>(List.of(entity));
        when(repository.findByTenantIdAndPatientIdOrderByPeriodStartDesc(
                eq(TENANT_ID), eq(patientId), any(PageRequest.class))).thenReturn(page);

        Bundle bundle = service.searchEncountersByPatient(TENANT_ID, patientId.toString(), PageRequest.of(0, 10));

        assertThat(bundle.getTotal()).isEqualTo(1);
        assertThat(bundle.getEntry()).hasSize(1);
    }

    @Test
    @DisplayName("Should search encounters by date range")
    void shouldSearchEncountersByDateRange() {
        EncounterService service = new EncounterService(repository, kafkaTemplate);
        UUID encounterId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        Encounter encounter = buildEncounter(encounterId, patientId);
        EncounterEntity entity = buildEntity(encounterId, patientId, encounter);
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        when(repository.findByPatientAndDateRange(TENANT_ID, patientId, start, end)).thenReturn(List.of(entity));

        Bundle bundle = service.searchEncountersByPatientAndDateRange(
                TENANT_ID, patientId.toString(), start, end);

        assertThat(bundle.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should fetch encounter subsets and counts")
    void shouldFetchEncounterSubsetsAndCounts() {
        EncounterService service = new EncounterService(repository, kafkaTemplate);
        UUID patientId = UUID.randomUUID();
        when(repository.findFinishedEncountersByPatient(TENANT_ID, patientId)).thenReturn(List.of());
        when(repository.findActiveEncountersByPatient(TENANT_ID, patientId)).thenReturn(List.of());
        when(repository.findInpatientEncountersByPatient(TENANT_ID, patientId)).thenReturn(List.of());
        when(repository.findAmbulatoryEncountersByPatient(TENANT_ID, patientId)).thenReturn(List.of());
        when(repository.findEmergencyEncountersByPatient(TENANT_ID, patientId)).thenReturn(List.of());
        when(repository.hasEncounterInDateRange(eq(TENANT_ID), eq(patientId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);
        when(repository.countInpatientEncountersInDateRange(eq(TENANT_ID), eq(patientId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(2L);
        when(repository.countEmergencyEncountersInDateRange(eq(TENANT_ID), eq(patientId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(1L);

        Bundle finished = service.getFinishedEncountersByPatient(TENANT_ID, patientId.toString());
        Bundle active = service.getActiveEncountersByPatient(TENANT_ID, patientId.toString());
        Bundle inpatient = service.getInpatientEncountersByPatient(TENANT_ID, patientId.toString());
        Bundle ambulatory = service.getAmbulatoryEncountersByPatient(TENANT_ID, patientId.toString());
        Bundle emergency = service.getEmergencyEncountersByPatient(TENANT_ID, patientId.toString());
        boolean hasEncounter = service.hasEncounterInDateRange(
                TENANT_ID, patientId.toString(), LocalDateTime.now().minusDays(1), LocalDateTime.now());
        long inpatientCount = service.countInpatientEncounters(
                TENANT_ID, patientId.toString(), LocalDateTime.now().minusDays(1), LocalDateTime.now());
        long emergencyCount = service.countEmergencyEncounters(
                TENANT_ID, patientId.toString(), LocalDateTime.now().minusDays(1), LocalDateTime.now());

        assertThat(finished.getTotal()).isZero();
        assertThat(active.getTotal()).isZero();
        assertThat(inpatient.getTotal()).isZero();
        assertThat(ambulatory.getTotal()).isZero();
        assertThat(emergency.getTotal()).isZero();
        assertThat(hasEncounter).isTrue();
        assertThat(inpatientCount).isEqualTo(2L);
        assertThat(emergencyCount).isEqualTo(1L);
    }

    private Encounter buildEncounter(UUID encounterId, UUID patientId) {
        Encounter encounter = new Encounter();
        encounter.setId(encounterId.toString());
        encounter.setSubject(new Reference("Patient/" + patientId));
        encounter.setStatus(Encounter.EncounterStatus.FINISHED);
        encounter.setClass_(new Coding().setCode("IMP").setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode"));
        encounter.addType(new CodeableConcept().addCoding(
                new Coding().setCode("AMB").setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                        .setDisplay("Ambulatory")));
        encounter.setServiceType(new CodeableConcept().addCoding(
                new Coding().setCode("service").setDisplay("Service")));
        encounter.setPriority(new CodeableConcept().addCoding(new Coding().setCode("urgent")));

        Period period = new Period();
        period.setStart(Date.from(Instant.parse("2023-01-01T00:00:00Z")));
        period.setEnd(Date.from(Instant.parse("2023-01-01T01:00:00Z")));
        encounter.setPeriod(period);

        encounter.addReasonCode(new CodeableConcept().addCoding(
                new Coding().setCode("R1").setSystem("system").setDisplay("Reason")));

        Encounter.EncounterLocationComponent location = new Encounter.EncounterLocationComponent();
        location.setLocation(new Reference("Location/loc1").setDisplay("Main"));
        encounter.addLocation(location);

        Encounter.EncounterParticipantComponent participant = new Encounter.EncounterParticipantComponent();
        participant.setIndividual(new Reference("Practitioner/abc").setDisplay("Dr Test"));
        encounter.addParticipant(participant);

        encounter.setServiceProvider(new Reference("Organization/org1").setDisplay("Org"));

        Encounter.EncounterHospitalizationComponent hosp = new Encounter.EncounterHospitalizationComponent();
        hosp.setAdmitSource(new CodeableConcept().addCoding(new Coding().setCode("ER")));
        hosp.setDischargeDisposition(new CodeableConcept().addCoding(new Coding().setCode("home")));
        encounter.setHospitalization(hosp);
        return encounter;
    }

    private EncounterEntity buildEntity(UUID encounterId, UUID patientId, Encounter encounter) {
        return EncounterEntity.builder()
                .id(encounterId)
                .tenantId(TENANT_ID)
                .resourceType("Encounter")
                .resourceJson(JSON_PARSER.encodeResourceToString(encounter))
                .patientId(patientId)
                .build();
    }
}
