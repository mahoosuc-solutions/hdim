package com.healthdata.eventsourcing.projection.condition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConditionProjectionService Tests")
class ConditionProjectionServiceTest {

    private final String TENANT_ID = "tenant-123";
    private final String PATIENT_ID = "patient-456";
    private final String ICD_CODE = "E11.9";

    @Mock
    private ConditionProjectionRepository repository;

    private ConditionProjectionService service;

    @BeforeEach
    void setUp() {
        service = new ConditionProjectionService(repository);
    }

    @Test
    @DisplayName("Should save condition projection")
    void shouldSaveConditionProjection() {
        ConditionProjection projection = ConditionProjection.builder()
            .patientId(PATIENT_ID)
            .tenantId(TENANT_ID)
            .icdCode(ICD_CODE)
            .status("active")
            .onsetDate(LocalDate.of(2024, 1, 1))
            .build();

        when(repository.save(any())).thenReturn(projection);

        ConditionProjection result = service.saveProjection(projection);

        assertThat(result).isNotNull();
        assertThat(result.getIcdCode()).isEqualTo(ICD_CODE);
        verify(repository).save(projection);
    }

    @Test
    @DisplayName("Should find active conditions for patient")
    void shouldFindActiveConditionsForPatient() {
        List<ConditionProjection> conditions = List.of(
            ConditionProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .icdCode("E11.9")
                .status("active")
                .build(),
            ConditionProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .icdCode("I10")
                .status("active")
                .build()
        );

        when(repository.findByTenantIdAndPatientIdAndStatus(TENANT_ID, PATIENT_ID, "active"))
            .thenReturn(conditions);

        List<ConditionProjection> result = service.findActiveConditions(TENANT_ID, PATIENT_ID);

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(c -> c.getStatus().equals("active"));
    }

    @Test
    @DisplayName("Should find conditions by status")
    void shouldFindConditionsByStatus() {
        List<ConditionProjection> conditions = List.of(
            ConditionProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .icdCode("E11.9")
                .status("resolved")
                .build()
        );

        when(repository.findByTenantIdAndPatientIdAndStatus(TENANT_ID, PATIENT_ID, "resolved"))
            .thenReturn(conditions);

        List<ConditionProjection> result = service.findByTenantPatientAndStatus(
            TENANT_ID, PATIENT_ID, "resolved");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo("resolved");
    }

    @Test
    @DisplayName("Should find all conditions for patient")
    void shouldFindAllConditionsForPatient() {
        List<ConditionProjection> conditions = List.of(
            ConditionProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .icdCode("E11.9")
                .status("active")
                .build(),
            ConditionProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .icdCode("I10")
                .status("resolved")
                .build()
        );

        when(repository.findByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
            .thenReturn(conditions);

        List<ConditionProjection> result = service.findByTenantAndPatient(TENANT_ID, PATIENT_ID);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Should enforce multi-tenant isolation")
    void shouldEnforceMultiTenantIsolation() {
        String otherTenant = "tenant-999";

        when(repository.findByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
            .thenReturn(List.of(ConditionProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .icdCode(ICD_CODE)
                .build()));

        when(repository.findByTenantIdAndPatientId(otherTenant, PATIENT_ID))
            .thenReturn(List.of());

        List<ConditionProjection> result1 = service.findByTenantAndPatient(TENANT_ID, PATIENT_ID);
        List<ConditionProjection> result2 = service.findByTenantAndPatient(otherTenant, PATIENT_ID);

        assertThat(result1).hasSize(1);
        assertThat(result2).isEmpty();
    }

    @Test
    @DisplayName("Should count conditions for patient")
    void shouldCountConditionsForPatient() {
        when(repository.countByTenantIdAndPatientId(TENANT_ID, PATIENT_ID))
            .thenReturn(3L);

        long count = service.countByTenantAndPatient(TENANT_ID, PATIENT_ID);

        assertThat(count).isEqualTo(3L);
    }

    @Test
    @DisplayName("Should find condition by ICD code")
    void shouldFindConditionByIcdCode() {
        ConditionProjection condition = ConditionProjection.builder()
            .patientId(PATIENT_ID)
            .tenantId(TENANT_ID)
            .icdCode(ICD_CODE)
            .status("active")
            .build();

        when(repository.findByTenantIdAndPatientIdAndIcdCode(TENANT_ID, PATIENT_ID, ICD_CODE))
            .thenReturn(Optional.of(condition));

        Optional<ConditionProjection> result = service.findByTenantPatientAndIcdCode(
            TENANT_ID, PATIENT_ID, ICD_CODE);

        assertThat(result).isPresent();
        assertThat(result.get().getIcdCode()).isEqualTo(ICD_CODE);
    }

    @Test
    @DisplayName("Should handle conditions with verification status")
    void shouldHandleConditionsWithVerificationStatus() {
        ConditionProjection projection = ConditionProjection.builder()
            .patientId(PATIENT_ID)
            .tenantId(TENANT_ID)
            .icdCode(ICD_CODE)
            .status("active")
            .verificationStatus("confirmed")
            .build();

        when(repository.save(any())).thenReturn(projection);

        ConditionProjection result = service.saveProjection(projection);

        assertThat(result.getVerificationStatus()).isEqualTo("confirmed");
    }

    @Test
    @DisplayName("Should track condition history")
    void shouldTrackConditionHistory() {
        List<ConditionProjection> history = List.of(
            ConditionProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .icdCode(ICD_CODE)
                .status("active")
                .onsetDate(LocalDate.of(2020, 1, 1))
                .build(),
            ConditionProjection.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .icdCode(ICD_CODE)
                .status("resolved")
                .onsetDate(LocalDate.of(2020, 1, 1))
                .build()
        );

        when(repository.findByTenantIdAndPatientIdAndIcdCodeOrderByOnsetDateDesc(TENANT_ID, PATIENT_ID, ICD_CODE))
            .thenReturn(history);

        List<ConditionProjection> result = service.findConditionHistory(TENANT_ID, PATIENT_ID, ICD_CODE);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Should count active conditions")
    void shouldCountActiveConditions() {
        when(repository.countByTenantIdAndPatientIdAndStatus(TENANT_ID, PATIENT_ID, "active"))
            .thenReturn(2L);

        long count = service.countActiveConditions(TENANT_ID, PATIENT_ID);

        assertThat(count).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should return empty when condition not found")
    void shouldReturnEmptyWhenConditionNotFound() {
        when(repository.findByTenantIdAndPatientIdAndIcdCode(anyString(), anyString(), anyString()))
            .thenReturn(Optional.empty());

        Optional<ConditionProjection> result = service.findByTenantPatientAndIcdCode(
            TENANT_ID, PATIENT_ID, "INVALID");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should verify repository called for all queries")
    void shouldVerifyRepositoryCalls() {
        service.findByTenantAndPatient(TENANT_ID, PATIENT_ID);
        service.findActiveConditions(TENANT_ID, PATIENT_ID);
        service.countByTenantAndPatient(TENANT_ID, PATIENT_ID);

        verify(repository).findByTenantIdAndPatientId(TENANT_ID, PATIENT_ID);
        verify(repository).findByTenantIdAndPatientIdAndStatus(TENANT_ID, PATIENT_ID, "active");
        verify(repository).countByTenantIdAndPatientId(TENANT_ID, PATIENT_ID);
    }
}
