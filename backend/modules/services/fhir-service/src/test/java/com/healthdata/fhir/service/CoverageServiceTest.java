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
import org.springframework.kafka.core.KafkaTemplate;

import com.healthdata.fhir.persistence.CoverageEntity;
import com.healthdata.fhir.persistence.CoverageRepository;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

/**
 * Unit tests for CoverageService.
 * Tests service layer logic including FHIR conversions, business rules, and event publishing.
 */
class CoverageServiceTest {

    private static final String TENANT = "tenant-1";
    private static final UUID PATIENT_ID = UUID.fromString("8b7e0540-2f8a-4f49-9f82-c0f4a6b46b95");
    private static final UUID COVERAGE_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final String SUBSCRIBER_ID = "MEM123456";
    private static final String GROUP_NUMBER = "GRP001";

    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4();
    private static final IParser JSON_PARSER = FHIR_CONTEXT.newJsonParser();

    @Mock
    private CoverageRepository coverageRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private CoverageService coverageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        coverageService = new CoverageService(coverageRepository, kafkaTemplate);
    }

    @Test
    void createCoverageShouldPersistAndPublishEvent() {
        // Given
        Coverage coverage = createFhirCoverage();
        CoverageEntity savedEntity = createCoverageEntity();

        when(coverageRepository.save(any(CoverageEntity.class))).thenReturn(savedEntity);

        // When
        Coverage result = coverageService.createCoverage(TENANT, coverage, "user-1");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(COVERAGE_ID.toString());

        verify(coverageRepository).save(any(CoverageEntity.class));
        verify(kafkaTemplate).send(eq("fhir.coverages.created"), eq(COVERAGE_ID.toString()), any());
    }

    @Test
    void createCoverageShouldAssignIdIfNotPresent() {
        // Given
        Coverage coverage = new Coverage();
        coverage.setStatus(Coverage.CoverageStatus.ACTIVE);
        coverage.setBeneficiary(new Reference("Patient/" + PATIENT_ID));
        coverage.addPayor(new Reference("Organization/payor-1"));
        coverage.setSubscriberId(SUBSCRIBER_ID);

        CoverageEntity savedEntity = CoverageEntity.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT)
                .patientId(PATIENT_ID)
                .status("active")
                .subscriberId(SUBSCRIBER_ID)
                .build();

        when(coverageRepository.save(any(CoverageEntity.class))).thenReturn(savedEntity);

        // When
        Coverage result = coverageService.createCoverage(TENANT, coverage, "user-1");

        // Then
        assertThat(result.hasId()).isTrue();
        verify(coverageRepository).save(any(CoverageEntity.class));
    }

    @Test
    void createCoverageShouldRejectMissingBeneficiary() {
        // Given
        Coverage coverage = new Coverage();
        coverage.setId(COVERAGE_ID.toString());
        coverage.setStatus(Coverage.CoverageStatus.ACTIVE);
        // No beneficiary set

        // When/Then
        assertThatThrownBy(() -> coverageService.createCoverage(TENANT, coverage, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("beneficiary");
    }

    @Test
    void getCoverageShouldReturnFhirResource() {
        // Given
        CoverageEntity entity = createCoverageEntityWithJson();

        when(coverageRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, COVERAGE_ID))
                .thenReturn(Optional.of(entity));

        // When
        Optional<Coverage> result = coverageService.getCoverage(TENANT, COVERAGE_ID);

        // Then
        assertThat(result).isPresent();
        Coverage coverage = result.get();
        assertThat(coverage.getIdElement().getIdPart()).isEqualTo(COVERAGE_ID.toString());
        assertThat(coverage.getStatus().toCode()).isEqualTo("active");
    }

    @Test
    void getCoverageShouldReturnEmptyWhenNotFound() {
        // Given
        when(coverageRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, COVERAGE_ID))
                .thenReturn(Optional.empty());

        // When
        Optional<Coverage> result = coverageService.getCoverage(TENANT, COVERAGE_ID);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void updateCoverageShouldUpdateAndPublishEvent() {
        // Given
        CoverageEntity existingEntity = createCoverageEntityWithJson();
        Coverage updatedCoverage = createFhirCoverage();
        updatedCoverage.setStatus(Coverage.CoverageStatus.CANCELLED);

        when(coverageRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, COVERAGE_ID))
                .thenReturn(Optional.of(existingEntity));
        when(coverageRepository.save(any(CoverageEntity.class))).thenReturn(existingEntity);

        // When
        Coverage result = coverageService.updateCoverage(TENANT, COVERAGE_ID, updatedCoverage, "user-2");

        // Then
        assertThat(result).isNotNull();
        verify(coverageRepository).save(any(CoverageEntity.class));
        verify(kafkaTemplate).send(eq("fhir.coverages.updated"), eq(COVERAGE_ID.toString()), any());
    }

    @Test
    void updateCoverageShouldThrowWhenNotFound() {
        // Given
        Coverage coverage = createFhirCoverage();
        when(coverageRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, COVERAGE_ID))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> coverageService.updateCoverage(TENANT, COVERAGE_ID, coverage, "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void deleteCoverageShouldSoftDeleteAndPublishEvent() {
        // Given
        CoverageEntity entity = createCoverageEntityWithJson();
        when(coverageRepository.findByTenantIdAndIdAndDeletedAtIsNull(TENANT, COVERAGE_ID))
                .thenReturn(Optional.of(entity));
        when(coverageRepository.save(any(CoverageEntity.class))).thenReturn(entity);

        // When
        coverageService.deleteCoverage(TENANT, COVERAGE_ID, "user-3");

        // Then
        ArgumentCaptor<CoverageEntity> captor = ArgumentCaptor.forClass(CoverageEntity.class);
        verify(coverageRepository).save(captor.capture());
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
        verify(kafkaTemplate).send(eq("fhir.coverages.deleted"), eq(COVERAGE_ID.toString()), any());
    }

    @Test
    void getCoveragesByPatientShouldReturnList() {
        // Given
        List<CoverageEntity> entities = List.of(
                createCoverageEntityWithJson(),
                createCoverageEntityWithJson()
        );

        when(coverageRepository.findByTenantIdAndPatientIdAndDeletedAtIsNullOrderByPeriodStartDesc(TENANT, PATIENT_ID))
                .thenReturn(entities);

        // When
        List<Coverage> results = coverageService.getCoveragesByPatient(TENANT, PATIENT_ID);

        // Then
        assertThat(results).hasSize(2);
    }

    @Test
    void getActiveCoveragesShouldReturnOnlyActive() {
        // Given
        List<CoverageEntity> entities = List.of(createCoverageEntityWithJson());

        when(coverageRepository.findActiveCoveragesForPatient(eq(TENANT), eq(PATIENT_ID), any(Instant.class)))
                .thenReturn(entities);

        // When
        List<Coverage> results = coverageService.getActiveCoverages(TENANT, PATIENT_ID);

        // Then
        assertThat(results).hasSize(1);
    }

    @Test
    void hasActiveCoverageShouldReturnTrue() {
        // Given
        when(coverageRepository.hasActiveCoverage(eq(TENANT), eq(PATIENT_ID), any(Instant.class)))
                .thenReturn(true);

        // When
        boolean result = coverageService.hasActiveCoverage(TENANT, PATIENT_ID);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void getPrimaryCoverageShouldReturnOptional() {
        // Given
        CoverageEntity entity = createCoverageEntityWithJson();
        when(coverageRepository.findPrimaryCoverage(eq(TENANT), eq(PATIENT_ID), any(Instant.class)))
                .thenReturn(Optional.of(entity));

        // When
        Optional<Coverage> result = coverageService.getPrimaryCoverage(TENANT, PATIENT_ID);

        // Then
        assertThat(result).isPresent();
    }

    // Helper methods

    private Coverage createFhirCoverage() {
        Coverage coverage = new Coverage();
        coverage.setId(COVERAGE_ID.toString());
        coverage.setStatus(Coverage.CoverageStatus.ACTIVE);
        coverage.setBeneficiary(new Reference("Patient/" + PATIENT_ID));
        coverage.addPayor(new Reference("Organization/payor-1").setDisplay("Blue Cross"));
        coverage.setSubscriberId(SUBSCRIBER_ID);
        coverage.setType(new CodeableConcept()
                .addCoding(new Coding()
                        .setSystem("http://terminology.hl7.org/CodeSystem/v3-ActCode")
                        .setCode("HIP")
                        .setDisplay("health insurance plan policy")));
        coverage.setPeriod(new Period()
                .setStart(new Date())
                .setEnd(Date.from(Instant.now().plusSeconds(365 * 24 * 60 * 60))));
        return coverage;
    }

    private CoverageEntity createCoverageEntity() {
        return CoverageEntity.builder()
                .id(COVERAGE_ID)
                .tenantId(TENANT)
                .patientId(PATIENT_ID)
                .status("active")
                .typeCode("HIP")
                .typeDisplay("health insurance plan policy")
                .subscriberId(SUBSCRIBER_ID)
                .groupNumber(GROUP_NUMBER)
                .payorReference("Organization/payor-1")
                .payorDisplay("Blue Cross")
                .periodStart(Instant.now())
                .periodEnd(Instant.now().plusSeconds(365 * 24 * 60 * 60))
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
    }

    private CoverageEntity createCoverageEntityWithJson() {
        Coverage coverage = createFhirCoverage();
        String json = JSON_PARSER.encodeResourceToString(coverage);

        return CoverageEntity.builder()
                .id(COVERAGE_ID)
                .tenantId(TENANT)
                .patientId(PATIENT_ID)
                .resourceJson(json)
                .status("active")
                .typeCode("HIP")
                .typeDisplay("health insurance plan policy")
                .subscriberId(SUBSCRIBER_ID)
                .groupNumber(GROUP_NUMBER)
                .payorReference("Organization/payor-1")
                .payorDisplay("Blue Cross")
                .periodStart(Instant.now())
                .periodEnd(Instant.now().plusSeconds(365 * 24 * 60 * 60))
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
    }
}
