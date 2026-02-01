package com.healthdata.gateway.clinical.compliance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.gateway.clinical.compliance.dto.ComplianceErrorDto;
import com.healthdata.gateway.clinical.compliance.dto.ErrorContextDto;
import com.healthdata.gateway.clinical.compliance.dto.ErrorSyncRequest;
import com.healthdata.gateway.clinical.compliance.entity.ComplianceErrorEntity;
import com.healthdata.gateway.clinical.compliance.repository.ComplianceErrorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComplianceErrorService")
class ComplianceErrorServiceTest {

    @Mock
    private ComplianceErrorRepository repository;

    private ComplianceErrorService service;

    @BeforeEach
    void setUp() {
        service = new ComplianceErrorService(repository, new ObjectMapper());
    }

    @Test
    @DisplayName("Should scope error IDs by tenant to avoid cross-tenant dedupe")
    void shouldScopeErrorIdsByTenant() {
        ComplianceErrorDto dto = ComplianceErrorDto.builder()
            .id("err-123")
            .timestamp(Instant.now().toString())
            .context(ErrorContextDto.builder().service("ui").operation("op").build())
            .message("boom")
            .build();

        ErrorSyncRequest request = new ErrorSyncRequest(List.of(dto), Instant.now().toString());
        when(repository.existsById(any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.syncErrors(request, "tenant-a");
        service.syncErrors(request, "tenant-b");

        ArgumentCaptor<ComplianceErrorEntity> captor = ArgumentCaptor.forClass(ComplianceErrorEntity.class);
        verify(repository, org.mockito.Mockito.times(2)).save(captor.capture());

        List<ComplianceErrorEntity> saved = captor.getAllValues();
        assertThat(saved.get(0).getId()).isNotEqualTo(saved.get(1).getId());
        assertThat(saved.get(0).getTenantId()).isEqualTo("tenant-a");
        assertThat(saved.get(1).getTenantId()).isEqualTo("tenant-b");
    }

    @Test
    @DisplayName("Should ignore DTO tenant override and use gateway tenant")
    void shouldIgnoreDtoTenantOverride() {
        ComplianceErrorDto dto = ComplianceErrorDto.builder()
            .id("err-456")
            .timestamp(Instant.now().toString())
            .context(ErrorContextDto.builder()
                .tenantId("spoofed-tenant")
                .service("ui")
                .operation("op")
                .build())
            .message("boom")
            .build();

        ErrorSyncRequest request = new ErrorSyncRequest(List.of(dto), Instant.now().toString());
        when(repository.existsById(any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.syncErrors(request, "trusted-tenant");

        ArgumentCaptor<ComplianceErrorEntity> captor = ArgumentCaptor.forClass(ComplianceErrorEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo("trusted-tenant");
    }

    @Test
    @DisplayName("Should default invalid timestamps to now")
    void shouldDefaultInvalidTimestamp() {
        ComplianceErrorDto dto = ComplianceErrorDto.builder()
            .id("err-789")
            .timestamp("not-a-timestamp")
            .context(ErrorContextDto.builder().service("ui").operation("op").build())
            .message("boom")
            .build();

        ErrorSyncRequest request = new ErrorSyncRequest(List.of(dto), Instant.now().toString());
        when(repository.existsById(any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Instant before = Instant.now();
        service.syncErrors(request, "tenant-a");
        Instant after = Instant.now();

        ArgumentCaptor<ComplianceErrorEntity> captor = ArgumentCaptor.forClass(ComplianceErrorEntity.class);
        verify(repository).save(captor.capture());

        Instant savedTimestamp = captor.getValue().getTimestamp();
        assertThat(savedTimestamp).isNotNull();
        assertThat(savedTimestamp).isBetween(before.minusSeconds(1), after.plusSeconds(1));
    }

    @Test
    @DisplayName("Should return deleted count from cleanup")
    void shouldReturnDeletedCount() {
        when(repository.deleteByTenantIdAndTimestampBefore(eq("tenant-a"), any()))
            .thenReturn(7L);

        long deleted = service.cleanupOldErrors("tenant-a", 30);
        assertThat(deleted).isEqualTo(7L);
    }

    @Test
    @DisplayName("Should return severity counts within range")
    void shouldReturnSeverityCountsInRange() {
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();
        when(repository.countByTenantIdAndSeverityAndDateRange(eq("tenant-a"), eq("ERROR"), eq(start), eq(end)))
            .thenReturn(5L);

        long count = service.getErrorCountBySeverityInRange("tenant-a", "ERROR", start, end);
        assertThat(count).isEqualTo(5L);
    }
}
