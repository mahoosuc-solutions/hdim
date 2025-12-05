package com.healthdata.fhir.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Enumerations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.core.KafkaTemplate;

import com.healthdata.fhir.persistence.PatientEntity;
import com.healthdata.fhir.persistence.PatientRepository;
import com.healthdata.fhir.validation.PatientValidator;

class PatientServiceTest {

    private static final String TENANT = "tenant-1";
    private static final String PATIENT_ID = "8b7e0540-2f8a-4f49-9f82-c0f4a6b46b95";
    private static final String CACHE_KEY = TENANT + ":" + PATIENT_ID;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @Mock
    private PatientValidator validator;

    private PatientService patientService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(cacheManager.getCache("fhir-patients")).thenReturn(cache);
        patientService = new PatientService(patientRepository, validator, kafkaTemplate, cacheManager);
    }

    @Test
    void createPatientShouldPersistAndPublish() {
        Patient patient = new Patient();
        patient.setId(PATIENT_ID);
        patient.addName()
                .setFamily("Chen")
                .addGiven("Maya");
        patient.setGender(Enumerations.AdministrativeGender.FEMALE);

        PatientEntity saved = PatientEntity.builder()
                .id(UUID.fromString(PATIENT_ID))
                .tenantId(TENANT)
                .resourceType("Patient")
                .resourceJson("{\"resourceType\":\"Patient\",\"id\":\"" + PATIENT_ID + "\"}")
                .firstName("Maya")
                .lastName("Chen")
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();

        when(validator.validate(patient)).thenReturn(PatientValidator.ValidationResult.ok());
        when(patientRepository.save(any(PatientEntity.class))).thenReturn(saved);

        Patient result = patientService.createPatient(TENANT, patient, "user-1");

        assertThat(result.getId()).isEqualTo(PATIENT_ID);
        verify(kafkaTemplate).send(eq("fhir.patients.created"), eq(PATIENT_ID), any());
        verify(cache).put(eq(CACHE_KEY), any(Patient.class));
    }

    @Test
    void createPatientShouldRejectInvalidPayload() {
        Patient patient = new Patient();
        when(validator.validate(patient))
                .thenReturn(PatientValidator.ValidationResult.error("must contain name"));

        assertThatThrownBy(() -> patientService.createPatient(TENANT, patient, "user-1"))
                .isInstanceOf(PatientService.PatientValidationException.class)
                .hasMessageContaining("must contain name");

        verify(patientRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(any(), any(), any());
    }

    @Test
    void getPatientShouldUseCacheBeforeRepository() {
        Patient cached = new Patient();
        cached.setId(PATIENT_ID);
        when(cache.get(CACHE_KEY, Patient.class)).thenReturn(cached);

        Optional<Patient> result = patientService.getPatient(TENANT, PATIENT_ID);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(PATIENT_ID);
        verify(patientRepository, never()).findActiveByTenantIdAndId(eq(TENANT), any());
    }

    @Test
    void getPatientShouldLoadFromRepositoryWhenCacheMiss() {
        when(cache.get(CACHE_KEY, Patient.class)).thenReturn(null);

        PatientEntity entity = PatientEntity.builder()
                .id(UUID.fromString(PATIENT_ID))
                .tenantId(TENANT)
                .resourceJson("{\"resourceType\":\"Patient\",\"id\":\"" + PATIENT_ID + "\"}")
                .build();
        when(patientRepository.findActiveByTenantIdAndId(TENANT, UUID.fromString(PATIENT_ID))).thenReturn(Optional.of(entity));

        Optional<Patient> result = patientService.getPatient(TENANT, PATIENT_ID);

        assertThat(result).isPresent();
        verify(cache).put(eq(CACHE_KEY), any(Patient.class));
    }

    @Test
    void updatePatientShouldPersistAndPublish() {
        when(cache.get(CACHE_KEY, Patient.class)).thenReturn(null);
        Patient existing = new Patient();
        existing.setId(PATIENT_ID);
        when(validator.validate(existing)).thenReturn(PatientValidator.ValidationResult.ok());
        when(patientRepository.findByTenantIdAndId(TENANT, UUID.fromString(PATIENT_ID))).thenReturn(Optional.of(
                PatientEntity.builder()
                        .id(UUID.fromString(PATIENT_ID))
                        .tenantId(TENANT)
                        .resourceJson("{}")
                        .resourceType("Patient")
                        .createdAt(Instant.now())
                        .lastModifiedAt(Instant.now())
                        .version(1)
                        .build()
        ));
        when(patientRepository.save(any(PatientEntity.class))).thenAnswer(invocation -> {
            PatientEntity entity = invocation.getArgument(0);
            return entity.toBuilder().version(entity.getVersion() + 1).build();
        });

        Patient updated = patientService.updatePatient(TENANT, PATIENT_ID, existing, "user-2");

        verify(patientRepository).save(any(PatientEntity.class));
        verify(kafkaTemplate).send(eq("fhir.patients.updated"), eq(PATIENT_ID), any());
        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(cache).put(eq(CACHE_KEY), patientCaptor.capture());
        assertThat(patientCaptor.getValue().getMeta().getVersionId()).isNotBlank();
        assertThat(updated.getMeta().getVersionId()).isNotBlank();
    }

    @Test
    void deletePatientShouldEvictCacheAndPublish() {
        when(patientRepository.findByTenantIdAndId(TENANT, UUID.fromString(PATIENT_ID))).thenReturn(Optional.of(
                PatientEntity.builder()
                        .id(UUID.fromString(PATIENT_ID))
                        .tenantId(TENANT)
                        .resourceJson("{}")
                        .resourceType("Patient")
                        .createdAt(Instant.now())
                        .lastModifiedAt(Instant.now())
                        .version(1)
                        .build()
        ));
        when(patientRepository.save(any(PatientEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        patientService.deletePatient(TENANT, PATIENT_ID, "user-3");

        verify(patientRepository).save(any(PatientEntity.class));
        verify(cache).evict(CACHE_KEY);
        verify(kafkaTemplate).send(eq("fhir.patients.deleted"), eq(PATIENT_ID), any());
    }

    @Test
    void searchPatientsShouldReturnBundle() {
        when(patientRepository.findByTenantIdAndLastNameContainingIgnoreCaseOrderByLastNameAsc(eq(TENANT), any()))
                .thenReturn(java.util.List.of(
                        PatientEntity.builder()
                                .id(UUID.fromString(PATIENT_ID))
                                .tenantId(TENANT)
                                .resourceJson("{\"resourceType\":\"Patient\",\"id\":\"" + PATIENT_ID + "\"}")
                                .resourceType("Patient")
                                .createdAt(Instant.now())
                                .lastModifiedAt(Instant.now())
                                .version(1)
                                .build()
                ));

        var bundle = patientService.searchPatients(TENANT, "Chen", 10);

        assertThat(bundle.getEntry()).hasSize(1);
        assertThat(bundle.getEntryFirstRep().getResource().getIdElement().getIdPart()).isEqualTo(PATIENT_ID);
    }
}
