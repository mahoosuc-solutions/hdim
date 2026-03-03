package com.healthdata.fhir.bulk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.healthdata.fhir.service.BundleTransactionService;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("Bulk Import Service Tests")
class BulkImportServiceTest {

    private static final String TENANT_ID = "test-tenant";
    private static final String USER_ID = "test-user";

    @Mock private BulkImportRepository importRepository;
    @Mock private BundleTransactionService bundleTransactionService;
    @Mock private KafkaTemplate<String, Object> kafkaTemplate;

    private BulkImportService service;

    @BeforeEach
    void setUp() {
        service = new BulkImportService(
                importRepository, bundleTransactionService,
                kafkaTemplate, new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("Should initiate import job and return job ID")
    void shouldInitiateImport() {
        when(importRepository.countActiveJobsByTenant(TENANT_ID)).thenReturn(0L);
        when(importRepository.save(any(BulkImportJob.class))).thenAnswer(inv -> inv.getArgument(0));

        UUID jobId = service.initiateImport(TENANT_ID, USER_ID);

        assertThat(jobId).isNotNull();

        ArgumentCaptor<BulkImportJob> captor = ArgumentCaptor.forClass(BulkImportJob.class);
        verify(importRepository).save(captor.capture());

        BulkImportJob saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(saved.getStatus()).isEqualTo(BulkImportJob.ImportStatus.PENDING);
        assertThat(saved.getSubmittedBy()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("Should reject import when concurrent limit exceeded")
    void shouldRejectWhenLimitExceeded() {
        when(importRepository.countActiveJobsByTenant(TENANT_ID)).thenReturn(3L);

        assertThatThrownBy(() -> service.initiateImport(TENANT_ID, USER_ID))
                .isInstanceOf(BulkImportService.ImportLimitExceededException.class);
    }

    @Test
    @DisplayName("Should process NDJSON stream and create batches")
    void shouldProcessNdjsonStream() {
        UUID jobId = UUID.randomUUID();
        BulkImportJob job = BulkImportJob.builder()
                .jobId(jobId)
                .tenantId(TENANT_ID)
                .status(BulkImportJob.ImportStatus.PENDING)
                .submittedAt(Instant.now())
                .build();

        when(importRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(importRepository.save(any(BulkImportJob.class))).thenAnswer(inv -> inv.getArgument(0));

        // Create batch response
        Bundle batchResponse = new Bundle();
        batchResponse.setType(Bundle.BundleType.BATCHRESPONSE);
        Bundle.BundleEntryComponent entry = batchResponse.addEntry();
        entry.getResponse().setStatus("201 Created");

        when(bundleTransactionService.processBundle(eq(TENANT_ID), any(Bundle.class), eq("bulk-import")))
                .thenReturn(batchResponse);

        // Single Patient NDJSON line
        String ndjson = "{\"resourceType\":\"Patient\",\"name\":[{\"family\":\"Smith\"}]}\n";
        ByteArrayInputStream stream = new ByteArrayInputStream(ndjson.getBytes(StandardCharsets.UTF_8));

        service.processNdjsonStream(jobId, TENANT_ID, stream);

        // Verify bundle was sent to BundleTransactionService
        verify(bundleTransactionService).processBundle(eq(TENANT_ID), any(Bundle.class), eq("bulk-import"));
    }

    @Test
    @DisplayName("Should get job status with tenant isolation")
    void shouldGetJobStatus() {
        UUID jobId = UUID.randomUUID();
        BulkImportJob job = BulkImportJob.builder()
                .jobId(jobId)
                .tenantId(TENANT_ID)
                .status(BulkImportJob.ImportStatus.IN_PROGRESS)
                .processedRecords(500L)
                .failedRecords(2L)
                .submittedAt(Instant.now())
                .build();

        when(importRepository.findByJobIdAndTenantId(jobId, TENANT_ID))
                .thenReturn(Optional.of(job));

        Optional<BulkImportJob> result = service.getJobStatus(TENANT_ID, jobId);

        assertThat(result).isPresent();
        assertThat(result.get().getProcessedRecords()).isEqualTo(500L);
        assertThat(result.get().getStatus()).isEqualTo(BulkImportJob.ImportStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Should publish Kafka event on import initiation")
    void shouldPublishKafkaEventOnInitiation() {
        when(importRepository.countActiveJobsByTenant(TENANT_ID)).thenReturn(0L);
        when(importRepository.save(any(BulkImportJob.class))).thenAnswer(inv -> inv.getArgument(0));

        service.initiateImport(TENANT_ID, USER_ID);

        verify(kafkaTemplate).send(eq("fhir.bulk-import.initiated"), anyString(), any());
    }

    @Test
    @DisplayName("Should handle malformed NDJSON lines gracefully")
    void shouldHandleMalformedNdjson() {
        UUID jobId = UUID.randomUUID();
        BulkImportJob job = BulkImportJob.builder()
                .jobId(jobId)
                .tenantId(TENANT_ID)
                .status(BulkImportJob.ImportStatus.PENDING)
                .submittedAt(Instant.now())
                .build();

        when(importRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(importRepository.save(any(BulkImportJob.class))).thenAnswer(inv -> inv.getArgument(0));

        // Mix of valid and invalid lines
        String ndjson = "not valid json\n"
                + "{\"resourceType\":\"Patient\",\"name\":[{\"family\":\"Valid\"}]}\n"
                + "also invalid\n";

        Bundle batchResponse = new Bundle();
        batchResponse.setType(Bundle.BundleType.BATCHRESPONSE);
        batchResponse.addEntry().getResponse().setStatus("201 Created");
        when(bundleTransactionService.processBundle(eq(TENANT_ID), any(Bundle.class), eq("bulk-import")))
                .thenReturn(batchResponse);

        ByteArrayInputStream stream = new ByteArrayInputStream(ndjson.getBytes(StandardCharsets.UTF_8));
        service.processNdjsonStream(jobId, TENANT_ID, stream);

        // Should complete without exception — errors captured in job status
        // findById called twice: once in processNdjsonStream, once in completeJob
        verify(importRepository, org.mockito.Mockito.atLeastOnce()).findById(jobId);
    }
}
