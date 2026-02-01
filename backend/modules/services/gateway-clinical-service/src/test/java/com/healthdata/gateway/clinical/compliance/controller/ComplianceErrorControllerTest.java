package com.healthdata.gateway.clinical.compliance.controller;

import com.healthdata.gateway.clinical.compliance.dto.ComplianceErrorDto;
import com.healthdata.gateway.clinical.compliance.dto.ErrorContextDto;
import com.healthdata.gateway.clinical.compliance.dto.ErrorSyncRequest;
import com.healthdata.gateway.clinical.compliance.entity.ComplianceErrorEntity;
import com.healthdata.gateway.clinical.compliance.service.ComplianceErrorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComplianceErrorController")
class ComplianceErrorControllerTest {

    @Mock
    private ComplianceErrorService complianceErrorService;

    @InjectMocks
    private ComplianceErrorController controller;

    @Test
    @DisplayName("Should reject oversized error sync batches")
    void shouldRejectOversizedBatch() {
        List<ComplianceErrorDto> errors = new ArrayList<>();
        for (int i = 0; i < 1001; i++) {
            errors.add(ComplianceErrorDto.builder()
                .id("err-" + i)
                .timestamp(Instant.now().toString())
                .context(ErrorContextDto.builder().service("ui").operation("op").build())
                .message("boom")
                .build());
        }

        ErrorSyncRequest request = new ErrorSyncRequest(errors, Instant.now().toString());
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("X-Tenant-ID", "tenant-a");

        var response = controller.syncErrors(request, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(complianceErrorService, never()).syncErrors(any(), any());
    }

    @Test
    @DisplayName("Should cap page size when querying errors")
    void shouldCapPageSize() {
        Page<ComplianceErrorEntity> emptyPage = new PageImpl<>(List.of());
        when(complianceErrorService.getErrors(eq("tenant-a"), any(Pageable.class))).thenReturn(emptyPage);

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("X-Tenant-ID", "tenant-a");

        controller.getErrors(null, null, null, 0, 10000, httpRequest);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(complianceErrorService).getErrors(eq("tenant-a"), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(500);
    }

    @Test
    @DisplayName("Should use time-bounded severity counts")
    void shouldUseTimeBoundedSeverityCounts() {
        when(complianceErrorService.getErrorCountInRange(any(), any(), any())).thenReturn(10L);
        when(complianceErrorService.getErrorCountBySeverityInRange(any(), any(), any(), any())).thenReturn(1L);

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("X-Tenant-ID", "tenant-a");

        var response = controller.getErrorStats(null, 24, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(complianceErrorService).getErrorCountInRange(eq("tenant-a"), any(), any());
        verify(complianceErrorService).getErrorCountBySeverityInRange(eq("tenant-a"), eq("CRITICAL"), any(), any());
        verify(complianceErrorService).getErrorCountBySeverityInRange(eq("tenant-a"), eq("ERROR"), any(), any());
        verify(complianceErrorService).getErrorCountBySeverityInRange(eq("tenant-a"), eq("WARNING"), any(), any());
        verify(complianceErrorService).getErrorCountBySeverityInRange(eq("tenant-a"), eq("INFO"), any(), any());
    }

    @Test
    @DisplayName("Should return cleanup deleted count")
    void shouldReturnCleanupDeletedCount() {
        when(complianceErrorService.cleanupOldErrors(eq("tenant-a"), eq(30))).thenReturn(12L);

        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        httpRequest.addHeader("X-Tenant-ID", "tenant-a");

        var response = controller.cleanupOldErrors(null, 30, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Object body = response.getBody();
        assertThat(body).isInstanceOf(ComplianceErrorController.CleanupResponse.class);
        var cleanup = (ComplianceErrorController.CleanupResponse) body;
        assertThat(cleanup.deleted()).isEqualTo(12L);
        assertThat(cleanup.retentionDays()).isEqualTo(30);
    }
}
