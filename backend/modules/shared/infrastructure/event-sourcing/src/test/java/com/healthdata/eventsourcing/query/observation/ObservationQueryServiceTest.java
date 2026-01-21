package com.healthdata.eventsourcing.query.observation;

import com.healthdata.eventsourcing.projection.observation.ObservationProjection;
import com.healthdata.eventsourcing.projection.observation.ObservationProjectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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

@ExtendWith(MockitoExtension.class)
class ObservationQueryServiceTest {

    @Mock
    private ObservationProjectionRepository observationRepository;

    @InjectMocks
    private ObservationQueryService queryService;

    private ObservationProjection testObservation;

    @BeforeEach
    void setUp() {
        testObservation = ObservationProjection.builder()
            .patientId("patient-123")
            .tenantId("tenant-123")
            .loincCode("2345-7")
            .value(new BigDecimal("145.5"))
            .unit("mg/dL")
            .observationDate(Instant.parse("2024-01-15T00:00:00Z"))
            .build();
    }

    @Test
    @DisplayName("Should find observations by patient and tenant")
    void shouldFindObservationsByPatientAndTenant() {
        when(observationRepository.findByPatientIdAndTenantId("patient-123", "tenant-123"))
            .thenReturn(List.of(testObservation));
        List<ObservationProjection> results = queryService.findByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results).hasSize(1).contains(testObservation);
    }

    @Test
    @DisplayName("Should find observations by LOINC code")
    void shouldFindObservationsByLoincCode() {
        when(observationRepository.findByLoincCodeAndTenantId("2345-7", "tenant-123"))
            .thenReturn(List.of(testObservation));
        List<ObservationProjection> results = queryService.findByLoincCodeAndTenant("2345-7", "tenant-123");
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("Should find latest observation by LOINC and patient")
    void shouldFindLatestObservationByLoincAndPatient() {
        when(observationRepository.findByPatientIdAndLoincCodeAndTenantId("patient-123", "2345-7", "tenant-123"))
            .thenReturn(Optional.of(testObservation));
        Optional<ObservationProjection> result = queryService.findLatestByLoincAndPatient("patient-123", "2345-7", "tenant-123");
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Should find observations by date range")
    void shouldFindObservationsByDateRange() {
        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        when(observationRepository.findByTenantIdAndObservationDateBetween("tenant-123", startDate, endDate))
            .thenReturn(List.of(testObservation));
        List<ObservationProjection> results = queryService.findByDateRange("tenant-123", startDate, endDate);
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("Should preserve LOINC code")
    void shouldPreserveLoincCode() {
        when(observationRepository.findByPatientIdAndTenantId("patient-123", "tenant-123"))
            .thenReturn(List.of(testObservation));
        List<ObservationProjection> results = queryService.findByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results.get(0).getLoincCode()).isEqualTo("2345-7");
    }

    @Test
    @DisplayName("Should preserve value and unit")
    void shouldPreserveValueAndUnit() {
        when(observationRepository.findByPatientIdAndTenantId("patient-123", "tenant-123"))
            .thenReturn(List.of(testObservation));
        List<ObservationProjection> results = queryService.findByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results.get(0).getValue()).isEqualTo(new BigDecimal("145.5"));
        assertThat(results.get(0).getUnit()).isEqualTo("mg/dL");
    }

    @Test
    @DisplayName("Should enforce multi-tenant isolation")
    void shouldEnforceMultiTenantIsolation() {
        ObservationProjection tenant2Obs = ObservationProjection.builder()
            .patientId("patient-123")
            .tenantId("tenant-456")
            .loincCode("2345-7")
            .value(new BigDecimal("150.0"))
            .unit("mg/dL")
            .observationDate(Instant.parse("2024-01-15T00:00:00Z"))
            .build();
        when(observationRepository.findByPatientIdAndTenantId("patient-123", "tenant-456"))
            .thenReturn(List.of(tenant2Obs));
        List<ObservationProjection> results = queryService.findByPatientAndTenant("patient-123", "tenant-456");
        assertThat(results.get(0).getTenantId()).isEqualTo("tenant-456");
    }

    @Test
    @DisplayName("Should return empty list when no observations")
    void shouldReturnEmptyWhenNoObservations() {
        when(observationRepository.findByPatientIdAndTenantId("unknown", "tenant-123"))
            .thenReturn(List.of());
        List<ObservationProjection> results = queryService.findByPatientAndTenant("unknown", "tenant-123");
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should handle null values")
    void shouldHandleNullValues() {
        ObservationProjection nullValueObs = ObservationProjection.builder()
            .patientId("patient-123")
            .tenantId("tenant-123")
            .loincCode("2345-7")
            .value(null)
            .unit(null)
            .observationDate(Instant.parse("2024-01-15T00:00:00Z"))
            .build();
        when(observationRepository.findByPatientIdAndTenantId("patient-123", "tenant-123"))
            .thenReturn(List.of(nullValueObs));
        List<ObservationProjection> results = queryService.findByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("Should preserve observation dates")
    void shouldPreserveObservationDates() {
        Instant obsInstant = Instant.parse("2023-06-15T00:00:00Z");
        ObservationProjection timedObs = ObservationProjection.builder()
            .patientId("patient-123")
            .tenantId("tenant-123")
            .loincCode("2345-7")
            .value(new BigDecimal("145.0"))
            .unit("mg/dL")
            .observationDate(obsInstant)
            .build();
        when(observationRepository.findByPatientIdAndTenantId("patient-123", "tenant-123"))
            .thenReturn(List.of(timedObs));
        List<ObservationProjection> results = queryService.findByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results.get(0).getObservationDate()).isEqualTo(obsInstant);
    }

    @Test
    @DisplayName("Should handle repository exceptions")
    void shouldHandleRepositoryExceptions() {
        when(observationRepository.findByPatientIdAndTenantId(anyString(), anyString()))
            .thenThrow(new RuntimeException("Database error"));
        assertThatThrownBy(() -> queryService.findByPatientAndTenant("patient-123", "tenant-123"))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should call repository with correct parameters")
    void shouldCallRepositoryWithCorrectParameters() {
        when(observationRepository.findByPatientIdAndTenantId("patient-123", "tenant-123"))
            .thenReturn(List.of(testObservation));
        queryService.findByPatientAndTenant("patient-123", "tenant-123");
        verify(observationRepository, times(1)).findByPatientIdAndTenantId("patient-123", "tenant-123");
    }

    @Test
    @DisplayName("Should handle multiple observations")
    void shouldHandleMultipleObservations() {
        ObservationProjection obs2 = ObservationProjection.builder()
            .patientId("patient-123")
            .tenantId("tenant-123")
            .loincCode("2345-7")
            .value(new BigDecimal("150.0"))
            .unit("mg/dL")
            .observationDate(Instant.parse("2024-01-16T00:00:00Z"))
            .build();
        when(observationRepository.findByPatientIdAndTenantId("patient-123", "tenant-123"))
            .thenReturn(List.of(testObservation, obs2));
        List<ObservationProjection> results = queryService.findByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("Should support LOINC filtering")
    void shouldSupportLoincFiltering() {
        when(observationRepository.findByLoincCodeAndTenantId("2345-7", "tenant-123"))
            .thenReturn(List.of(testObservation));
        List<ObservationProjection> results = queryService.findByLoincCodeAndTenant("2345-7", "tenant-123");
        assertThat(results).allMatch(obs -> obs.getLoincCode().equals("2345-7"));
    }

    @Test
    @DisplayName("Should verify repository delegation")
    void shouldVerifyRepositoryDelegation() {
        when(observationRepository.findByLoincCodeAndTenantId("2345-7", "tenant-123"))
            .thenReturn(List.of(testObservation));
        queryService.findByLoincCodeAndTenant("2345-7", "tenant-123");
        verify(observationRepository).findByLoincCodeAndTenantId("2345-7", "tenant-123");
    }

    @Test
    @DisplayName("Should handle date range queries")
    void shouldHandleDateRangeQueries() {
        LocalDate start = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2024, 1, 31);
        when(observationRepository.findByTenantIdAndObservationDateBetween("tenant-123", start, end))
            .thenReturn(List.of(testObservation));
        List<ObservationProjection> results = queryService.findByDateRange("tenant-123", start, end);
        assertThat(results).hasSize(1);
    }
}
