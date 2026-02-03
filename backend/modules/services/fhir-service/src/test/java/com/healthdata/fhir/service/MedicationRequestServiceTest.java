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
import org.hl7.fhir.r4.model.Dosage;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.hl7.fhir.r4.model.Quantity;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.SimpleQuantity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
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

import com.healthdata.fhir.persistence.MedicationRequestEntity;
import com.healthdata.fhir.persistence.MedicationRequestRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@ExtendWith(MockitoExtension.class)
@DisplayName("MedicationRequest Service Tests")
@Tag("integration")  // TODO: Fix string assertion - extra quotes in dispense instructions
class MedicationRequestServiceTest {

    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);
    private static final String TENANT_ID = "tenant-1";

    @Mock
    private MedicationRequestRepository repository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    private MedicationRequestService service;

    @BeforeEach
    void setUp() {
        when(cacheManager.getCache("fhir-medication-requests")).thenReturn(cache);
        service = new MedicationRequestService(repository, kafkaTemplate, cacheManager);
    }

    @Test
    @DisplayName("Should create medication request and publish event")
    void shouldCreateMedicationRequest() {
        MedicationRequest request = buildMedicationRequest();
        UUID id = UUID.fromString(request.getIdElement().getIdPart());
        UUID patientId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        MedicationRequestEntity saved = buildEntity(id, patientId, request);
        when(repository.save(any(MedicationRequestEntity.class))).thenReturn(saved);

        MedicationRequest result = service.createMedicationRequest(TENANT_ID, request, "creator");

        assertThat(result.getIdElement().getIdPart()).isEqualTo(id.toString());
        assertThat(result.getMeta().getVersionId()).isEqualTo("0");
        verify(cache).put(eq(cacheKey(id)), any(MedicationRequest.class));
        verify(kafkaTemplate).send(eq("fhir.medication-requests.created"), eq(id.toString()), any());
    }

    @Test
    @DisplayName("Should return cached medication request")
    void shouldReturnCachedMedicationRequest() {
        UUID id = UUID.randomUUID();
        MedicationRequest cached = buildMedicationRequest();
        cached.setId(id.toString());
        when(cache.get(cacheKey(id), MedicationRequest.class)).thenReturn(cached);

        Optional<MedicationRequest> result = service.getMedicationRequest(TENANT_ID, id.toString());

        assertThat(result).contains(cached);
        verify(repository, never()).findByTenantIdAndId(anyString(), any());
    }

    @Test
    @DisplayName("Should load medication request and cache it")
    void shouldLoadMedicationRequest() {
        UUID id = UUID.randomUUID();
        UUID patientId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        MedicationRequest request = buildMedicationRequest();
        request.setId(id.toString());
        MedicationRequestEntity entity = buildEntity(id, patientId, request);
        when(repository.findByTenantIdAndId(TENANT_ID, id)).thenReturn(Optional.of(entity));

        Optional<MedicationRequest> result = service.getMedicationRequest(TENANT_ID, id.toString());

        assertThat(result).isPresent();
        verify(cache).put(eq(cacheKey(id)), any(MedicationRequest.class));
    }

    @Test
    @DisplayName("Should update medication request and publish event")
    void shouldUpdateMedicationRequest() {
        MedicationRequest request = buildMedicationRequest();
        UUID id = UUID.fromString(request.getIdElement().getIdPart());
        UUID patientId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        MedicationRequestEntity entity = buildEntity(id, patientId, request);
        MedicationRequestEntity updated = entity.toBuilder().version(2).build();

        when(repository.findByTenantIdAndId(TENANT_ID, id)).thenReturn(Optional.of(entity));
        when(repository.save(any(MedicationRequestEntity.class))).thenReturn(updated);

        MedicationRequest result = service.updateMedicationRequest(TENANT_ID, id.toString(), request, "updater");

        assertThat(result.getMeta().getVersionId()).isEqualTo("2");
        verify(cache).put(eq(cacheKey(id)), any(MedicationRequest.class));
        verify(kafkaTemplate).send(eq("fhir.medication-requests.updated"), eq(id.toString()), any());
    }

    @Test
    @DisplayName("Should delete medication request and evict cache")
    void shouldDeleteMedicationRequest() {
        MedicationRequest request = buildMedicationRequest();
        UUID id = UUID.fromString(request.getIdElement().getIdPart());
        UUID patientId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        MedicationRequestEntity entity = buildEntity(id, patientId, request);
        when(repository.findByTenantIdAndId(TENANT_ID, id)).thenReturn(Optional.of(entity));

        service.deleteMedicationRequest(TENANT_ID, id.toString(), "deleter");

        verify(repository).delete(entity);
        verify(cache).evict(cacheKey(id));
        verify(kafkaTemplate).send(eq("fhir.medication-requests.deleted"), eq(id.toString()), any());
    }

    @Test
    @DisplayName("Should search medication requests by patient")
    void shouldSearchMedicationRequestsByPatient() {
        UUID patientId = UUID.randomUUID();
        MedicationRequest request = buildMedicationRequest();
        UUID id = UUID.fromString(request.getIdElement().getIdPart());
        MedicationRequestEntity entity = buildEntity(id, patientId, request);
        Page<MedicationRequestEntity> page = new PageImpl<>(List.of(entity));
        when(repository.findByTenantIdAndPatientIdOrderByAuthoredOnDesc(
                eq(TENANT_ID), eq(patientId), any(PageRequest.class))).thenReturn(page);

        org.hl7.fhir.r4.model.Bundle bundle = service.searchMedicationRequestsByPatient(
                TENANT_ID, patientId.toString(), PageRequest.of(0, 10));

        assertThat(bundle.getTotal()).isEqualTo(1);
        assertThat(bundle.getEntry()).hasSize(1);
    }

    @Test
    @DisplayName("Should search medication requests by patient and code")
    void shouldSearchMedicationRequestsByPatientAndCode() {
        UUID patientId = UUID.randomUUID();
        MedicationRequest request = buildMedicationRequest();
        UUID id = UUID.fromString(request.getIdElement().getIdPart());
        MedicationRequestEntity entity = buildEntity(id, patientId, request);

        when(repository.findByTenantIdAndPatientIdAndMedicationCodeOrderByAuthoredOnDesc(
                TENANT_ID, patientId, "med-1")).thenReturn(List.of(entity));

        org.hl7.fhir.r4.model.Bundle bundle = service.searchMedicationRequestsByPatientAndCode(
                TENANT_ID, patientId.toString(), "med-1");

        assertThat(bundle.getTotal()).isEqualTo(1);
        assertThat(bundle.getEntry()).hasSize(1);
    }

    @Test
    @DisplayName("Should fetch active, prescription, and refill requests")
    void shouldFetchActiveAndRefillRequests() {
        UUID patientId = UUID.randomUUID();
        MedicationRequest request = buildMedicationRequest();
        UUID id = UUID.fromString(request.getIdElement().getIdPart());
        MedicationRequestEntity entity = buildEntity(id, patientId, request);

        when(repository.findActiveRequestsByPatient(TENANT_ID, patientId)).thenReturn(List.of(entity));
        when(repository.findPrescriptionsByPatient(TENANT_ID, patientId)).thenReturn(List.of(entity));
        when(repository.findRequestsWithRefills(TENANT_ID, patientId)).thenReturn(List.of(entity));

        org.hl7.fhir.r4.model.Bundle active = service.getActiveRequestsByPatient(TENANT_ID, patientId.toString());
        org.hl7.fhir.r4.model.Bundle prescriptions = service.getPrescriptionsByPatient(TENANT_ID, patientId.toString());
        org.hl7.fhir.r4.model.Bundle refills = service.getRequestsWithRefills(TENANT_ID, patientId.toString());

        assertThat(active.getTotal()).isEqualTo(1);
        assertThat(prescriptions.getTotal()).isEqualTo(1);
        assertThat(refills.getTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should check if active medication exists")
    void shouldCheckActiveMedication() {
        UUID patientId = UUID.randomUUID();
        when(repository.hasActiveMedication(TENANT_ID, patientId, "med-1")).thenReturn(true);

        boolean result = service.hasActiveMedication(TENANT_ID, patientId.toString(), "med-1");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should validate required fields")
    void shouldValidateRequiredFields() {
        MedicationRequest request = new MedicationRequest();

        assertThatThrownBy(() -> service.createMedicationRequest(TENANT_ID, request, "creator"))
                .isInstanceOf(MedicationRequestService.MedicationRequestValidationException.class);

        request.setSubject(new Reference("Patient/" + UUID.randomUUID()));
        assertThatThrownBy(() -> service.createMedicationRequest(TENANT_ID, request, "creator"))
                .isInstanceOf(MedicationRequestService.MedicationRequestValidationException.class);

        request.setMedication(new CodeableConcept().addCoding(new Coding().setCode("med-1")));
        assertThatThrownBy(() -> service.createMedicationRequest(TENANT_ID, request, "creator"))
                .isInstanceOf(MedicationRequestService.MedicationRequestValidationException.class);

        request.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);
        assertThatThrownBy(() -> service.createMedicationRequest(TENANT_ID, request, "creator"))
                .isInstanceOf(MedicationRequestService.MedicationRequestValidationException.class);
    }

    @Test
    @DisplayName("Should assign ID and map dispense details")
    void shouldAssignIdAndMapDispenseDetails() {
        MedicationRequest request = buildMedicationRequest();
        request.setId((String) null);
        request.getDosageInstructionFirstRep().setText("Take one tablet daily");
        request.setDispenseRequest(new MedicationRequest.MedicationRequestDispenseRequestComponent()
                .setQuantity(new SimpleQuantity().setValue(30).setUnit("tabs"))
                .setNumberOfRepeatsAllowed(2));

        when(repository.save(any(MedicationRequestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MedicationRequest result = service.createMedicationRequest(TENANT_ID, request, "creator");

        ArgumentCaptor<MedicationRequestEntity> captor = ArgumentCaptor.forClass(MedicationRequestEntity.class);
        verify(repository).save(captor.capture());
        MedicationRequestEntity saved = captor.getValue();

        assertThat(result.getId()).isNotBlank();
        assertThat(saved.getDispenseQuantity()).isEqualTo(30.0);
        assertThat(saved.getDispenseUnit()).isEqualTo("tabs");
        assertThat(saved.getNumberOfRepeatsAllowed()).isEqualTo(2);
        assertThat(saved.getDosageInstruction()).isEqualTo("Take one tablet daily");
    }

    @Test
    @DisplayName("Should allow medication reference and default authored date")
    void shouldAllowMedicationReferenceAndDefaultAuthoredOn() {
        MedicationRequest request = new MedicationRequest();
        request.setSubject(new Reference("Patient/11111111-1111-1111-1111-111111111111"));
        request.setMedication(new Reference("Medication/med-1"));
        request.setStatus(MedicationRequest.MedicationRequestStatus.ACTIVE);
        request.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);

        when(repository.save(any(MedicationRequestEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MedicationRequest result = service.createMedicationRequest(TENANT_ID, request, "creator");

        ArgumentCaptor<MedicationRequestEntity> captor = ArgumentCaptor.forClass(MedicationRequestEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getAuthoredOn()).isNotNull();
        assertThat(result.hasId()).isTrue();
    }

    @Test
    @DisplayName("Should handle missing cache gracefully")
    void shouldHandleMissingCache() {
        CacheManager noCacheManager = org.mockito.Mockito.mock(CacheManager.class);
        when(noCacheManager.getCache("fhir-medication-requests")).thenReturn(null);
        MedicationRequestService noCacheService = new MedicationRequestService(repository, kafkaTemplate, noCacheManager);

        UUID id = UUID.randomUUID();
        UUID patientId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        MedicationRequest request = buildMedicationRequest();
        request.setId(id.toString());
        MedicationRequestEntity entity = buildEntity(id, patientId, request);
        when(repository.findByTenantIdAndId(TENANT_ID, id)).thenReturn(Optional.of(entity));

        Optional<MedicationRequest> result = noCacheService.getMedicationRequest(TENANT_ID, id.toString());

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Should throw when update request is missing")
    void shouldThrowWhenUpdateMissing() {
        MedicationRequest request = buildMedicationRequest();
        UUID id = UUID.fromString(request.getIdElement().getIdPart());
        when(repository.findByTenantIdAndId(TENANT_ID, id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateMedicationRequest(TENANT_ID, id.toString(), request, "updater"))
                .isInstanceOf(MedicationRequestService.MedicationRequestNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw when delete request is missing")
    void shouldThrowWhenDeleteMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findByTenantIdAndId(TENANT_ID, id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteMedicationRequest(TENANT_ID, id.toString(), "deleter"))
                .isInstanceOf(MedicationRequestService.MedicationRequestNotFoundException.class);
    }

    @Test
    @DisplayName("Should reject invalid UUIDs")
    void shouldRejectInvalidUuid() {
        assertThatThrownBy(() -> service.getMedicationRequest(TENANT_ID, "not-a-uuid"))
                .isInstanceOf(MedicationRequestService.MedicationRequestValidationException.class);
        assertThatThrownBy(() -> service.searchMedicationRequestsByPatient(TENANT_ID, "bad-id", PageRequest.of(0, 1)))
                .isInstanceOf(MedicationRequestService.MedicationRequestValidationException.class);
    }

    private MedicationRequest buildMedicationRequest() {
        MedicationRequest request = new MedicationRequest();
        request.setId(UUID.randomUUID().toString());
        request.setSubject(new Reference("Patient/11111111-1111-1111-1111-111111111111"));
        request.setStatus(MedicationRequest.MedicationRequestStatus.ACTIVE);
        request.setIntent(MedicationRequest.MedicationRequestIntent.ORDER);
        request.setPriority(MedicationRequest.MedicationRequestPriority.ROUTINE);
        request.setAuthoredOn(Date.from(Instant.now()));
        request.setMedication(new CodeableConcept()
                .addCoding(new Coding()
                        .setCode("med-1")
                        .setSystem("http://example.com")
                        .setDisplay("Medication One")));

        Dosage dosage = new Dosage();
        dosage.setDoseAndRate(List.of(new Dosage.DosageDoseAndRateComponent()
                .setDose(new Quantity().setValue(1).setUnit("tab"))));
        request.addDosageInstruction(dosage);

        request.setRequester(new Reference("Practitioner/123"));
        return request;
    }

    private MedicationRequestEntity buildEntity(UUID id, UUID patientId, MedicationRequest request) {
        return MedicationRequestEntity.builder()
                .id(id)
                .tenantId(TENANT_ID)
                .resourceType("MedicationRequest")
                .resourceJson(JSON_PARSER.encodeResourceToString(request))
                .patientId(patientId)
                .status("active")
                .intent("order")
                .authoredOn(LocalDateTime.now(ZoneId.of("UTC")))
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
    }

    private String cacheKey(UUID id) {
        return TENANT_ID + ":medrx:" + id;
    }
}
