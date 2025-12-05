package com.healthdata.fhir.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.ZoneId;
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
import org.springframework.kafka.core.KafkaTemplate;

import com.healthdata.fhir.persistence.ImmunizationEntity;
import com.healthdata.fhir.persistence.ImmunizationRepository;

/**
 * Unit tests for ImmunizationService.
 * Tests service layer logic including FHIR conversions, business rules, and event publishing.
 */
class ImmunizationServiceTest {

    private static final String TENANT = "tenant-1";
    private static final String PATIENT_ID = "8b7e0540-2f8a-4f49-9f82-c0f4a6b46b95";
    private static final String IMMUNIZATION_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String CVX_CODE_COVID = "213";

    @Mock
    private ImmunizationRepository immunizationRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private ImmunizationService immunizationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        immunizationService = new ImmunizationService(immunizationRepository, kafkaTemplate);
    }

    @Test
    void createImmunizationShouldPersistAndPublishAuditEvent() {
        // Given
        Immunization immunization = createFhirImmunization();

        ImmunizationEntity savedEntity = ImmunizationEntity.builder()
                .id(UUID.fromString(IMMUNIZATION_ID))
                .tenantId(TENANT)
                .patientId(UUID.fromString(PATIENT_ID))
                .vaccineCode(CVX_CODE_COVID)
                .status("completed")
                .occurrenceDate(LocalDate.now())
                .build();

        when(immunizationRepository.save(any(ImmunizationEntity.class))).thenReturn(savedEntity);

        // When
        Immunization result = immunizationService.createImmunization(TENANT, immunization, "user-1");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(IMMUNIZATION_ID);

        verify(immunizationRepository).save(any(ImmunizationEntity.class));
        verify(kafkaTemplate).send(eq("audit-events"), any(String.class));
    }

    @Test
    void createImmunizationShouldAssignIdIfNotPresent() {
        // Given
        Immunization immunization = new Immunization();
        immunization.setStatus(Immunization.ImmunizationStatus.COMPLETED);
        immunization.setPatient(new Reference("Patient/" + PATIENT_ID));
        immunization.setVaccineCode(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://hl7.org/fhir/sid/cvx")
                        .setCode(CVX_CODE_COVID)
                        .setDisplay("COVID-19 vaccine")));
        immunization.setOccurrence(new DateTimeType(new Date()));

        ImmunizationEntity savedEntity = ImmunizationEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT)
                .patientId(UUID.fromString(PATIENT_ID))
                .vaccineCode(CVX_CODE_COVID)
                .status("completed")
                .occurrenceDate(LocalDate.now())
                .build();

        when(immunizationRepository.save(any(ImmunizationEntity.class))).thenReturn(savedEntity);

        // When
        Immunization result = immunizationService.createImmunization(TENANT, immunization, "user-1");

        // Then
        assertThat(result.hasId()).isTrue();
        verify(immunizationRepository).save(any(ImmunizationEntity.class));
    }

    @Test
    void getImmunizationShouldReturnFhirResource() {
        // Given
        ImmunizationEntity entity = ImmunizationEntity.builder()
                .id(UUID.fromString(IMMUNIZATION_ID))
                .tenantId(TENANT)
                .patientId(UUID.fromString(PATIENT_ID))
                .vaccineCode(CVX_CODE_COVID)
                .vaccineSystem("http://hl7.org/fhir/sid/cvx")
                .vaccineDisplay("COVID-19 vaccine")
                .status("completed")
                .occurrenceDate(LocalDate.now())
                .primarySource(true)
                .lotNumber("LOT123")
                .expirationDate(LocalDate.now().plusYears(1))
                .build();

        when(immunizationRepository.findByTenantIdAndId(TENANT, UUID.fromString(IMMUNIZATION_ID)))
                .thenReturn(Optional.of(entity));

        // When
        Optional<Immunization> result = immunizationService.getImmunization(TENANT, IMMUNIZATION_ID);

        // Then
        assertThat(result).isPresent();
        Immunization immunization = result.get();
        assertThat(immunization.getId()).isEqualTo(IMMUNIZATION_ID);
        assertThat(immunization.getStatus().toCode()).isEqualTo("completed");
        assertThat(immunization.getVaccineCode().getCodingFirstRep().getCode()).isEqualTo(CVX_CODE_COVID);
        assertThat(immunization.getLotNumber()).isEqualTo("LOT123");
        assertThat(immunization.getPrimarySource()).isTrue();
    }

    @Test
    void getImmunizationShouldReturnEmptyWhenNotFound() {
        // Given
        when(immunizationRepository.findByTenantIdAndId(TENANT, UUID.fromString(IMMUNIZATION_ID)))
                .thenReturn(Optional.empty());

        // When
        Optional<Immunization> result = immunizationService.getImmunization(TENANT, IMMUNIZATION_ID);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void updateImmunizationShouldPersistChangesAndPublishAuditEvent() {
        // Given
        Immunization updatedImmunization = createFhirImmunization();
        updatedImmunization.setLotNumber("NEW-LOT-456");

        ImmunizationEntity existingEntity = ImmunizationEntity.builder()
                .id(UUID.fromString(IMMUNIZATION_ID))
                .tenantId(TENANT)
                .patientId(UUID.fromString(PATIENT_ID))
                .vaccineCode(CVX_CODE_COVID)
                .status("completed")
                .occurrenceDate(LocalDate.now())
                .build();

        when(immunizationRepository.findByTenantIdAndId(TENANT, UUID.fromString(IMMUNIZATION_ID)))
                .thenReturn(Optional.of(existingEntity));
        when(immunizationRepository.save(any(ImmunizationEntity.class))).thenReturn(existingEntity);

        // When
        Immunization result = immunizationService.updateImmunization(TENANT, IMMUNIZATION_ID,
                updatedImmunization, "user-2");

        // Then
        assertThat(result).isNotNull();

        ArgumentCaptor<ImmunizationEntity> entityCaptor = ArgumentCaptor.forClass(ImmunizationEntity.class);
        verify(immunizationRepository).save(entityCaptor.capture());

        ImmunizationEntity savedEntity = entityCaptor.getValue();
        assertThat(savedEntity.getLastModifiedBy()).isEqualTo("user-2");

        verify(kafkaTemplate).send(eq("audit-events"), any(String.class));
    }

    @Test
    void updateImmunizationShouldThrowExceptionWhenNotFound() {
        // Given
        Immunization immunization = createFhirImmunization();

        when(immunizationRepository.findByTenantIdAndId(TENANT, UUID.fromString(IMMUNIZATION_ID)))
                .thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> immunizationService.updateImmunization(TENANT, IMMUNIZATION_ID,
                immunization, "user-2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void deleteImmunizationShouldRemoveAndPublishAuditEvent() {
        // Given
        ImmunizationEntity entity = ImmunizationEntity.builder()
                .id(UUID.fromString(IMMUNIZATION_ID))
                .tenantId(TENANT)
                .patientId(UUID.fromString(PATIENT_ID))
                .vaccineCode(CVX_CODE_COVID)
                .status("completed")
                .occurrenceDate(LocalDate.now())
                .build();

        when(immunizationRepository.findByTenantIdAndId(TENANT, UUID.fromString(IMMUNIZATION_ID)))
                .thenReturn(Optional.of(entity));

        // When
        immunizationService.deleteImmunization(TENANT, IMMUNIZATION_ID, "user-3");

        // Then
        verify(immunizationRepository).delete(entity);
        verify(kafkaTemplate).send(eq("audit-events"), any(String.class));
    }

    @Test
    void getImmunizationsByPatientShouldReturnBundle() {
        // Given
        List<ImmunizationEntity> entities = List.of(
                createEntity(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(30)),
                createEntity("141", "completed", LocalDate.now().minusDays(15))
        );

        when(immunizationRepository.findByTenantIdAndPatientIdOrderByOccurrenceDateDesc(
                eq(TENANT), eq(UUID.fromString(PATIENT_ID))))
                .thenReturn(entities);

        // When
        Bundle bundle = immunizationService.getImmunizationsByPatient(TENANT, PATIENT_ID, null);

        // Then
        assertThat(bundle).isNotNull();
        assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.SEARCHSET);
        assertThat(bundle.getTotal()).isEqualTo(2);
        assertThat(bundle.getEntry()).hasSize(2);
    }

    @Test
    void getCompletedImmunizationsShouldReturnOnlyCompleted() {
        // Given
        List<ImmunizationEntity> completedEntities = List.of(
                createEntity(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(30)),
                createEntity("141", "completed", LocalDate.now().minusDays(15))
        );

        when(immunizationRepository.findCompletedImmunizations(TENANT, UUID.fromString(PATIENT_ID)))
                .thenReturn(completedEntities);

        // When
        Bundle bundle = immunizationService.getCompletedImmunizations(TENANT, PATIENT_ID);

        // Then
        assertThat(bundle).isNotNull();
        assertThat(bundle.getEntry()).hasSize(2);
        assertThat(bundle.getEntry()).allMatch(entry -> {
            Immunization imm = (Immunization) entry.getResource();
            return imm.getStatus() == Immunization.ImmunizationStatus.COMPLETED;
        });
    }

    @Test
    void getImmunizationsByVaccineCodeShouldFilterByCode() {
        // Given
        List<ImmunizationEntity> covidEntities = List.of(
                createEntity(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(60)),
                createEntity(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(30))
        );

        when(immunizationRepository.findByVaccineCode(TENANT, UUID.fromString(PATIENT_ID), CVX_CODE_COVID))
                .thenReturn(covidEntities);

        // When
        Bundle bundle = immunizationService.getImmunizationsByVaccineCode(TENANT, PATIENT_ID, CVX_CODE_COVID);

        // Then
        assertThat(bundle).isNotNull();
        assertThat(bundle.getEntry()).hasSize(2);
        assertThat(bundle.getEntry()).allMatch(entry -> {
            Immunization imm = (Immunization) entry.getResource();
            return imm.getVaccineCode().getCodingFirstRep().getCode().equals(CVX_CODE_COVID);
        });
    }

    @Test
    void hasImmunizationShouldReturnTrueWhenExists() {
        // Given
        when(immunizationRepository.hasImmunization(TENANT, UUID.fromString(PATIENT_ID), CVX_CODE_COVID))
                .thenReturn(true);

        // When
        boolean result = immunizationService.hasImmunization(TENANT, PATIENT_ID, CVX_CODE_COVID);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void hasImmunizationShouldReturnFalseWhenNotExists() {
        // Given
        when(immunizationRepository.hasImmunization(TENANT, UUID.fromString(PATIENT_ID), CVX_CODE_COVID))
                .thenReturn(false);

        // When
        boolean result = immunizationService.hasImmunization(TENANT, PATIENT_ID, CVX_CODE_COVID);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void countByVaccineCodeShouldReturnCorrectCount() {
        // Given
        when(immunizationRepository.countByVaccineCode(TENANT, UUID.fromString(PATIENT_ID), CVX_CODE_COVID))
                .thenReturn(3L);

        // When
        long count = immunizationService.countByVaccineCode(TENANT, PATIENT_ID, CVX_CODE_COVID);

        // Then
        assertThat(count).isEqualTo(3L);
    }

    @Test
    void getVaccineSeriesShouldReturnOrderedDoses() {
        // Given
        List<ImmunizationEntity> seriesEntities = List.of(
                createEntityWithDose(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(60), 1),
                createEntityWithDose(CVX_CODE_COVID, "completed", LocalDate.now().minusDays(30), 2)
        );

        when(immunizationRepository.findVaccineSeries(TENANT, UUID.fromString(PATIENT_ID), CVX_CODE_COVID))
                .thenReturn(seriesEntities);

        // When
        Bundle bundle = immunizationService.getVaccineSeries(TENANT, PATIENT_ID, CVX_CODE_COVID);

        // Then
        assertThat(bundle).isNotNull();
        assertThat(bundle.getEntry()).hasSize(2);
    }

    @Test
    void isSeriesCompleteShouldReturnTrueWhenComplete() {
        // Given
        when(immunizationRepository.isSeriesComplete(TENANT, UUID.fromString(PATIENT_ID), CVX_CODE_COVID, 2))
                .thenReturn(true);

        // When
        boolean result = immunizationService.isSeriesComplete(TENANT, PATIENT_ID, CVX_CODE_COVID, 2);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void isSeriesCompleteShouldReturnFalseWhenIncomplete() {
        // Given
        when(immunizationRepository.isSeriesComplete(TENANT, UUID.fromString(PATIENT_ID), CVX_CODE_COVID, 3))
                .thenReturn(false);

        // When
        boolean result = immunizationService.isSeriesComplete(TENANT, PATIENT_ID, CVX_CODE_COVID, 3);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void getImmunizationsWithReactionsShouldReturnOnlyThoseWithReactions() {
        // Given
        ImmunizationEntity withReaction = createEntity(CVX_CODE_COVID, "completed", LocalDate.now());
        withReaction.setHadReaction(true);
        withReaction.setReactionDetail("Mild fever");

        when(immunizationRepository.findImmunizationsWithReactions(TENANT, UUID.fromString(PATIENT_ID)))
                .thenReturn(List.of(withReaction));

        // When
        Bundle bundle = immunizationService.getImmunizationsWithReactions(TENANT, PATIENT_ID);

        // Then
        assertThat(bundle).isNotNull();
        assertThat(bundle.getEntry()).hasSize(1);
    }

    @Test
    void countCompletedImmunizationsShouldReturnCorrectCount() {
        // Given
        when(immunizationRepository.countCompletedImmunizations(TENANT, UUID.fromString(PATIENT_ID)))
                .thenReturn(5L);

        // When
        long count = immunizationService.countCompletedImmunizations(TENANT, PATIENT_ID);

        // Then
        assertThat(count).isEqualTo(5L);
    }

    @Test
    void shouldHandleFhirResourceConversionWithAllFields() {
        // Given
        ImmunizationEntity entity = ImmunizationEntity.builder()
                .id(UUID.fromString(IMMUNIZATION_ID))
                .tenantId(TENANT)
                .patientId(UUID.fromString(PATIENT_ID))
                .vaccineCode(CVX_CODE_COVID)
                .vaccineSystem("http://hl7.org/fhir/sid/cvx")
                .vaccineDisplay("COVID-19 vaccine")
                .status("completed")
                .occurrenceDate(LocalDate.now())
                .primarySource(true)
                .lotNumber("LOT-123")
                .expirationDate(LocalDate.now().plusYears(1))
                .site("LA")
                .siteDisplay("Left arm")
                .route("IM")
                .routeDisplay("Intramuscular")
                .doseNumber(1)
                .seriesDoses(2)
                .performerId("Practitioner/123")
                .performerDisplay("Dr. Smith")
                .locationId("Location/456")
                .locationDisplay("Main Clinic")
                .manufacturer("Pfizer")
                .hadReaction(false)
                .build();

        when(immunizationRepository.findByTenantIdAndId(TENANT, UUID.fromString(IMMUNIZATION_ID)))
                .thenReturn(Optional.of(entity));

        // When
        Optional<Immunization> result = immunizationService.getImmunization(TENANT, IMMUNIZATION_ID);

        // Then
        assertThat(result).isPresent();
        Immunization imm = result.get();
        assertThat(imm.getVaccineCode().getCodingFirstRep().getCode()).isEqualTo(CVX_CODE_COVID);
        assertThat(imm.getVaccineCode().getCodingFirstRep().getSystem()).isEqualTo("http://hl7.org/fhir/sid/cvx");
        assertThat(imm.getVaccineCode().getCodingFirstRep().getDisplay()).isEqualTo("COVID-19 vaccine");
        assertThat(imm.getStatus().toCode()).isEqualTo("completed");
        assertThat(imm.getPrimarySource()).isTrue();
        assertThat(imm.getLotNumber()).isEqualTo("LOT-123");
        assertThat(imm.hasExpirationDate()).isTrue();
    }

    // Helper methods
    private Immunization createFhirImmunization() {
        Immunization immunization = new Immunization();
        immunization.setId(IMMUNIZATION_ID);
        immunization.setStatus(Immunization.ImmunizationStatus.COMPLETED);
        immunization.setPatient(new Reference("Patient/" + PATIENT_ID));

        immunization.setVaccineCode(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://hl7.org/fhir/sid/cvx")
                        .setCode(CVX_CODE_COVID)
                        .setDisplay("COVID-19 vaccine")));

        immunization.setOccurrence(new DateTimeType(new Date()));
        immunization.setPrimarySource(true);
        immunization.setLotNumber("LOT123");

        return immunization;
    }

    private ImmunizationEntity createEntity(String vaccineCode, String status, LocalDate occurrenceDate) {
        return ImmunizationEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT)
                .patientId(UUID.fromString(PATIENT_ID))
                .vaccineCode(vaccineCode)
                .vaccineSystem("http://hl7.org/fhir/sid/cvx")
                .vaccineDisplay(vaccineCode.equals(CVX_CODE_COVID) ? "COVID-19 vaccine" : "Influenza vaccine")
                .status(status)
                .occurrenceDate(occurrenceDate)
                .primarySource(true)
                .build();
    }

    private ImmunizationEntity createEntityWithDose(String vaccineCode, String status,
                                                     LocalDate occurrenceDate, Integer doseNumber) {
        ImmunizationEntity entity = createEntity(vaccineCode, status, occurrenceDate);
        entity.setDoseNumber(doseNumber);
        entity.setSeriesDoses(2);
        return entity;
    }
}
