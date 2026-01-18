package com.healthdata.eventsourcing.projection.observation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for ObservationProjectionService
 *
 * Tests CQRS read model for vital signs (observations) including:
 * - Time-series data retrieval
 * - LOINC code filtering
 * - Date range queries
 * - Multi-tenant isolation
 * - Pagination support
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ObservationProjectionService Tests")
class ObservationProjectionServiceTest {

    private final String TENANT_ID = "tenant-123";
    private final String PATIENT_ID = "patient-456";
    private final String LOINC_TEMPERATURE = "8310-5";
    private final String LOINC_HEART_RATE = "8867-4";

    @Mock
    private ObservationProjectionRepository repository;

    private ObservationProjectionService service;

    @BeforeEach
    void setUp() {
        service = new ObservationProjectionService(repository);
    }

    @Test
    @DisplayName("Should save observation projection")
    void shouldSaveObservationProjection() {
        ObservationProjection projection = ObservationProjection.builder()
            .patientId(PATIENT_ID)
            .tenantId(TENANT_ID)
            .loincCode(LOINC_TEMPERATURE)
            .value(new BigDecimal("37.5"))
            .unit("°C")
            .observationDate(Instant.now())
            .build();

        when(repository.save(any())).thenReturn(projection);

        ObservationProjection result = service.saveProjection(projection);

        assertThat(result).isNotNull();
        assertThat(result.getLoincCode()).isEqualTo(LOINC_TEMPERATURE);
        verify(repository).save(projection);
    }

    @Test
    @DisplayName("Should find observations by tenant and patient")
    void shouldFindObservationsByTenantAndPatient() {
        List<ObservationProjection> observations = List.of(
            ObservationProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .loincCode(LOINC_TEMPERATURE)
                .value(new BigDecimal("37.5"))
                .build(),
            ObservationProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .loincCode(LOINC_HEART_RATE)
                .value(new BigDecimal("72"))
                .build()
        );

        when(repository.findByTenantIdAndPatientIdOrderByObservationDateDesc(TENANT_ID, PATIENT_ID))
            .thenReturn(observations);

        List<ObservationProjection> result = service.findByTenantAndPatient(TENANT_ID, PATIENT_ID);

        assertThat(result).hasSize(2);
        assertThat(result).extracting("loincCode")
            .containsExactly(LOINC_TEMPERATURE, LOINC_HEART_RATE);
    }

    @Test
    @DisplayName("Should find observations by LOINC code")
    void shouldFindObservationsByLoincCode() {
        List<ObservationProjection> observations = List.of(
            ObservationProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .loincCode(LOINC_TEMPERATURE)
                .value(new BigDecimal("37.5"))
                .build(),
            ObservationProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .loincCode(LOINC_TEMPERATURE)
                .value(new BigDecimal("37.8"))
                .build()
        );

        when(repository.findByTenantIdAndPatientIdAndLoincCodeOrderByObservationDateDesc(
            TENANT_ID, PATIENT_ID, LOINC_TEMPERATURE))
            .thenReturn(observations);

        List<ObservationProjection> result = service.findByTenantPatientAndLoinc(
            TENANT_ID, PATIENT_ID, LOINC_TEMPERATURE);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(o -> o.getLoincCode().equals(LOINC_TEMPERATURE));
    }

    @Test
    @DisplayName("Should find observations within date range")
    void shouldFindObservationsWithinDateRange() {
        Instant startDate = Instant.parse("2024-01-01T00:00:00Z");
        Instant endDate = Instant.parse("2024-12-31T23:59:59Z");

        List<ObservationProjection> observations = List.of(
            ObservationProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .loincCode(LOINC_TEMPERATURE)
                .value(new BigDecimal("37.5"))
                .observationDate(Instant.parse("2024-06-15T10:00:00Z"))
                .build()
        );

        when(repository.findByTenantIdAndPatientIdAndObservationDateBetweenOrderByObservationDateDesc(
            TENANT_ID, PATIENT_ID, startDate, endDate))
            .thenReturn(observations);

        List<ObservationProjection> result = service.findByTenantPatientAndDateRange(
            TENANT_ID, PATIENT_ID, startDate, endDate);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("Should enforce multi-tenant isolation")
    void shouldEnforceMultiTenantIsolation() {
        String otherTenant = "tenant-999";

        when(repository.findByTenantIdAndPatientIdOrderByObservationDateDesc(TENANT_ID, PATIENT_ID))
            .thenReturn(List.of(ObservationProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .loincCode(LOINC_TEMPERATURE)
                .build()));

        when(repository.findByTenantIdAndPatientIdOrderByObservationDateDesc(otherTenant, PATIENT_ID))
            .thenReturn(List.of());

        List<ObservationProjection> result1 = service.findByTenantAndPatient(TENANT_ID, PATIENT_ID);
        List<ObservationProjection> result2 = service.findByTenantAndPatient(otherTenant, PATIENT_ID);

        assertThat(result1).hasSize(1);
        assertThat(result2).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when no observations found")
    void shouldReturnEmptyListWhenNoObservationsFound() {
        when(repository.findByTenantIdAndPatientIdOrderByObservationDateDesc(anyString(), anyString()))
            .thenReturn(List.of());

        List<ObservationProjection> result = service.findByTenantAndPatient(TENANT_ID, PATIENT_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should count observations for patient")
    void shouldCountObservationsForPatient() {
        when(repository.countByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
            .thenReturn(5L);

        long count = service.countByTenantAndPatient(TENANT_ID, PATIENT_ID);

        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("Should find latest observation by LOINC code")
    void shouldFindLatestObservationByLoincCode() {
        ObservationProjection latest = ObservationProjection.builder()
            .patientId(PATIENT_ID)
            .tenantId(TENANT_ID)
            .loincCode(LOINC_TEMPERATURE)
            .value(new BigDecimal("37.5"))
            .observationDate(Instant.now())
            .build();

        when(repository.findFirstByTenantIdAndPatientIdAndLoincCodeOrderByObservationDateDesc(
            TENANT_ID, PATIENT_ID, LOINC_TEMPERATURE))
            .thenReturn(Optional.of(latest));

        Optional<ObservationProjection> result = service.findLatestByTenantPatientAndLoinc(
            TENANT_ID, PATIENT_ID, LOINC_TEMPERATURE);

        assertThat(result).isPresent();
        assertThat(result.get().getValue()).isEqualTo(new BigDecimal("37.5"));
    }

    @Test
    @DisplayName("Should handle observations with different units")
    void shouldHandleObservationsWithDifferentUnits() {
        ObservationProjection celsius = ObservationProjection.builder()
            .patientId(PATIENT_ID)
            .tenantId(TENANT_ID)
            .loincCode(LOINC_TEMPERATURE)
            .value(new BigDecimal("37.5"))
            .unit("°C")
            .build();

        ObservationProjection fahrenheit = ObservationProjection.builder()
            .patientId(PATIENT_ID)
            .tenantId(TENANT_ID)
            .loincCode(LOINC_TEMPERATURE)
            .value(new BigDecimal("99.5"))
            .unit("°F")
            .build();

        when(repository.save(celsius)).thenReturn(celsius);
        when(repository.save(fahrenheit)).thenReturn(fahrenheit);

        ObservationProjection result1 = service.saveProjection(celsius);
        ObservationProjection result2 = service.saveProjection(fahrenheit);

        assertThat(result1.getUnit()).isEqualTo("°C");
        assertThat(result2.getUnit()).isEqualTo("°F");
    }

    @Test
    @DisplayName("Should find vital signs for trend analysis")
    void shouldFindVitalSignsForTrendAnalysis() {
        List<ObservationProjection> vitals = List.of(
            ObservationProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .loincCode(LOINC_TEMPERATURE)
                .value(new BigDecimal("37.5"))
                .observationDate(Instant.parse("2024-01-01T08:00:00Z"))
                .build(),
            ObservationProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .loincCode(LOINC_TEMPERATURE)
                .value(new BigDecimal("37.8"))
                .observationDate(Instant.parse("2024-01-02T08:00:00Z"))
                .build(),
            ObservationProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .loincCode(LOINC_TEMPERATURE)
                .value(new BigDecimal("37.2"))
                .observationDate(Instant.parse("2024-01-03T08:00:00Z"))
                .build()
        );

        when(repository.findByTenantIdAndPatientIdAndLoincCodeOrderByObservationDateDesc(
            TENANT_ID, PATIENT_ID, LOINC_TEMPERATURE))
            .thenReturn(vitals);

        List<ObservationProjection> result = service.findByTenantPatientAndLoinc(
            TENANT_ID, PATIENT_ID, LOINC_TEMPERATURE);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getValue()).isEqualTo(new BigDecimal("37.5"));
    }

    @Test
    @DisplayName("Should verify repository called for all queries")
    void shouldVerifyRepositoryCalls() {
        Instant now = Instant.now();

        service.findByTenantAndPatient(TENANT_ID, PATIENT_ID);
        service.findByTenantPatientAndLoinc(TENANT_ID, PATIENT_ID, LOINC_TEMPERATURE);
        service.findByTenantPatientAndDateRange(TENANT_ID, PATIENT_ID, now, now);
        service.countByTenantAndPatient(TENANT_ID, PATIENT_ID);

        verify(repository).findByTenantIdAndPatientIdOrderByObservationDateDesc(TENANT_ID, PATIENT_ID);
        verify(repository).findByTenantIdAndPatientIdAndLoincCodeOrderByObservationDateDesc(
            TENANT_ID, PATIENT_ID, LOINC_TEMPERATURE);
        verify(repository).countByTenantIdAndPatientId(TENANT_ID, PATIENT_ID);
    }

    @Test
    @DisplayName("Should handle observations with clinical notes")
    void shouldHandleObservationsWithClinicalNotes() {
        ObservationProjection projection = ObservationProjection.builder()
            .patientId(PATIENT_ID)
            .tenantId(TENANT_ID)
            .loincCode(LOINC_TEMPERATURE)
            .value(new BigDecimal("38.5"))
            .unit("°C")
            .notes("Patient reports fever symptoms")
            .observationDate(Instant.now())
            .build();

        when(repository.save(any())).thenReturn(projection);

        ObservationProjection result = service.saveProjection(projection);

        assertThat(result.getNotes()).isEqualTo("Patient reports fever symptoms");
    }
}
