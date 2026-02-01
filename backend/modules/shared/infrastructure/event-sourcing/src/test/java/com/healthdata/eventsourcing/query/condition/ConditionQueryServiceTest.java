package com.healthdata.eventsourcing.query.condition;

import com.healthdata.eventsourcing.projection.condition.ConditionProjection;
import com.healthdata.eventsourcing.projection.condition.ConditionProjectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConditionQueryServiceTest {

    @Mock
    private ConditionProjectionRepository conditionRepository;

    @InjectMocks
    private ConditionQueryService queryService;

    private ConditionProjection testCondition;

    @BeforeEach
    void setUp() {
        testCondition = ConditionProjection.builder()
            .patientId("patient-123")
            .tenantId("tenant-123")
            .icdCode("E11.9")
            .status("active")
            .verificationStatus("confirmed")
            .onsetDate(LocalDate.of(2020, 1, 15))
            .build();
    }

    @Test
    @DisplayName("Should find conditions by patient and tenant")
    void shouldFindConditionsByPatientAndTenant() {
        when(conditionRepository.findByPatientIdAndTenantId("patient-123", "tenant-123"))
            .thenReturn(List.of(testCondition));
        List<ConditionProjection> results = queryService.findByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results).hasSize(1).contains(testCondition);
    }

    @Test
    @DisplayName("Should find conditions by ICD code")
    void shouldFindConditionsByIcdCode() {
        when(conditionRepository.findByIcdCodeAndTenantId("E11.9", "tenant-123"))
            .thenReturn(List.of(testCondition));
        List<ConditionProjection> results = queryService.findByIcdCodeAndTenant("E11.9", "tenant-123");
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("Should find active conditions by patient and tenant")
    void shouldFindActiveConditions() {
        when(conditionRepository.findByPatientIdAndTenantIdAndStatus("patient-123", "tenant-123", "active"))
            .thenReturn(List.of(testCondition));
        List<ConditionProjection> results = queryService.findActiveConditionsByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("Should find all conditions by tenant")
    void shouldFindAllByTenant() {
        when(conditionRepository.findByTenantId("tenant-123"))
            .thenReturn(List.of(testCondition));
        List<ConditionProjection> results = queryService.findAllByTenant("tenant-123");
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("Should preserve ICD code")
    void shouldPreserveIcdCode() {
        when(conditionRepository.findByPatientIdAndTenantId("patient-123", "tenant-123"))
            .thenReturn(List.of(testCondition));
        List<ConditionProjection> results = queryService.findByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results.get(0).getIcdCode()).isEqualTo("E11.9");
    }

    @Test
    @DisplayName("Should preserve status")
    void shouldPreserveStatus() {
        when(conditionRepository.findByPatientIdAndTenantId("patient-123", "tenant-123"))
            .thenReturn(List.of(testCondition));
        List<ConditionProjection> results = queryService.findByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results.get(0).getStatus()).isEqualTo("active");
    }

    @Test
    @DisplayName("Should enforce multi-tenant isolation")
    void shouldEnforceMultiTenantIsolation() {
        ConditionProjection tenant2Cond = ConditionProjection.builder()
            .patientId("patient-456")
            .tenantId("tenant-456")
            .icdCode("I10")
            .status("active")
            .verificationStatus("confirmed")
            .onsetDate(LocalDate.of(2021, 1, 1))
            .build();
        when(conditionRepository.findByPatientIdAndTenantId("patient-456", "tenant-456"))
            .thenReturn(List.of(tenant2Cond));
        List<ConditionProjection> results = queryService.findByPatientAndTenant("patient-456", "tenant-456");
        assertThat(results.get(0).getTenantId()).isEqualTo("tenant-456");
    }

    @Test
    @DisplayName("Should return empty when no conditions")
    void shouldReturnEmptyWhenNoConditions() {
        when(conditionRepository.findByPatientIdAndTenantId("unknown", "tenant-123"))
            .thenReturn(List.of());
        List<ConditionProjection> results = queryService.findByPatientAndTenant("unknown", "tenant-123");
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should handle null onset date")
    void shouldHandleNullOnsetDate() {
        ConditionProjection noDateCond = ConditionProjection.builder()
            .patientId("patient-123")
            .tenantId("tenant-123")
            .icdCode("E11.9")
            .status("active")
            .verificationStatus("confirmed")
            .onsetDate(null)
            .build();
        when(conditionRepository.findByPatientIdAndTenantId("patient-123", "tenant-123"))
            .thenReturn(List.of(noDateCond));
        List<ConditionProjection> results = queryService.findByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results).hasSize(1);
    }

    @Test
    @DisplayName("Should preserve verification status")
    void shouldPreserveVerificationStatus() {
        when(conditionRepository.findByPatientIdAndTenantId("patient-123", "tenant-123"))
            .thenReturn(List.of(testCondition));
        List<ConditionProjection> results = queryService.findByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results.get(0).getVerificationStatus()).isEqualTo("confirmed");
    }

    @Test
    @DisplayName("Should handle repository exceptions")
    void shouldHandleRepositoryExceptions() {
        when(conditionRepository.findByPatientIdAndTenantId(anyString(), anyString()))
            .thenThrow(new RuntimeException("Database error"));
        assertThatThrownBy(() -> queryService.findByPatientAndTenant("patient-123", "tenant-123"))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should filter by status")
    void shouldFilterByStatus() {
        when(conditionRepository.findByPatientIdAndTenantIdAndStatus("patient-123", "tenant-123", "active"))
            .thenReturn(List.of(testCondition));
        List<ConditionProjection> results = queryService.findActiveConditionsByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results).allMatch(c -> c.getStatus().equals("active"));
    }

    @Test
    @DisplayName("Should handle multiple conditions")
    void shouldHandleMultipleConditions() {
        ConditionProjection cond2 = ConditionProjection.builder()
            .patientId("patient-123")
            .tenantId("tenant-123")
            .icdCode("I10")
            .status("active")
            .verificationStatus("confirmed")
            .build();
        when(conditionRepository.findByPatientIdAndTenantId("patient-123", "tenant-123"))
            .thenReturn(List.of(testCondition, cond2));
        List<ConditionProjection> results = queryService.findByPatientAndTenant("patient-123", "tenant-123");
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("Should verify repository delegation")
    void shouldVerifyRepositoryDelegation() {
        when(conditionRepository.findByPatientIdAndTenantId("patient-123", "tenant-123"))
            .thenReturn(List.of(testCondition));
        queryService.findByPatientAndTenant("patient-123", "tenant-123");
        verify(conditionRepository).findByPatientIdAndTenantId("patient-123", "tenant-123");
    }
}
