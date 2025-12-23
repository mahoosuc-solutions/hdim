package com.healthdata.fhir.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.healthdata.fhir.admin.model.ApiPreset;
import com.healthdata.fhir.admin.model.DashboardSnapshot;
import com.healthdata.fhir.admin.model.ServiceCatalog;
import com.healthdata.fhir.admin.model.SystemHealthSnapshot;
import com.healthdata.fhir.persistence.PatientEntity;
import com.healthdata.fhir.persistence.PatientRepository;

@ExtendWith(MockitoExtension.class)
class AdminPortalServiceTest {

    private static final String DEFAULT_TENANT = "tenant-1";

    @Mock
    private PatientRepository patientRepository;

    private AdminPortalService adminPortalService;

    @BeforeEach
    void setUp() {
        adminPortalService = new AdminPortalService(patientRepository);
    }

    @Test
    void getDashboardSnapshotShouldUseDefaultTenant() {
        when(patientRepository.countByTenantId(DEFAULT_TENANT)).thenReturn(12L);

        DashboardSnapshot snapshot = adminPortalService.getDashboardSnapshot(null);

        assertThat(snapshot.metrics()).hasSize(3);
        assertThat(snapshot.services()).hasSize(2);
        verify(patientRepository).countByTenantId(DEFAULT_TENANT);
    }

    @Test
    void getServiceCatalogShouldIncludePatientCount() {
        when(patientRepository.countByTenantId(DEFAULT_TENANT)).thenReturn(10L);

        ServiceCatalog catalog = adminPortalService.getServiceCatalog(" ");

        assertThat(catalog.services()).hasSize(3);
        ServiceCatalog.ServiceDefinition registry = catalog.services().stream()
                .filter(service -> "patient-registry".equals(service.serviceId()))
                .findFirst()
                .orElseThrow();
        assertThat(registry.description()).contains("10");
    }

    @Test
    void getSystemHealthShouldClampQueueDepth() {
        when(patientRepository.countByTenantId(DEFAULT_TENANT)).thenReturn(10L);

        SystemHealthSnapshot snapshot = adminPortalService.getSystemHealth("");

        assertThat(snapshot.queues()).hasSize(2);
        assertThat(snapshot.queues().get(0).depth()).isZero();
    }

    @Test
    void getApiPresetsShouldUseSamplePatientWhenAvailable() {
        UUID sampleId = UUID.randomUUID();
        PatientEntity entity = PatientEntity.builder()
                .id(sampleId)
                .tenantId(DEFAULT_TENANT)
                .resourceType("Patient")
                .resourceJson("{\"resourceType\":\"Patient\",\"id\":\"" + sampleId + "\"}")
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .version(0)
                .build();
        when(patientRepository.findByTenantIdAndLastNameContainingIgnoreCaseOrderByLastNameAsc(
                DEFAULT_TENANT, "")).thenReturn(List.of(entity));

        List<ApiPreset> presets = adminPortalService.getApiPresets(DEFAULT_TENANT);

        assertThat(presets).hasSize(3);
        assertThat(presets.get(1).path()).contains(sampleId.toString());
        verify(patientRepository).findByTenantIdAndLastNameContainingIgnoreCaseOrderByLastNameAsc(
                eq(DEFAULT_TENANT), eq(""));
    }

    @Test
    void getApiPresetsShouldFallbackWhenNoPatientFound() {
        when(patientRepository.findByTenantIdAndLastNameContainingIgnoreCaseOrderByLastNameAsc(
                DEFAULT_TENANT, "")).thenReturn(Collections.emptyList());

        List<ApiPreset> presets = adminPortalService.getApiPresets(DEFAULT_TENANT);

        assertThat(presets).hasSize(3);
        assertThat(presets.get(1).path()).contains("8b7e0540-2f8a-4f49-9f82-c0f4a6b46b95");
    }
}
