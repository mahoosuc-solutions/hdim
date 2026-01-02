package com.healthdata.migration.cli.client;

import java.util.List;
import java.util.UUID;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.healthdata.migration.dto.DataQualityReport;
import com.healthdata.migration.dto.JobStatus;
import com.healthdata.migration.dto.MigrationErrorCategory;
import com.healthdata.migration.dto.MigrationJobRequest;
import com.healthdata.migration.dto.MigrationJobResponse;
import com.healthdata.migration.dto.MigrationProgress;
import com.healthdata.migration.dto.MigrationSummary;
import com.healthdata.migration.persistence.MigrationErrorEntity;

/**
 * REST client for communicating with migration-workflow-service
 */
@Component
public class MigrationApiClient {

    private WebClient createClient(String baseUrl, String tenantId) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Tenant-ID", tenantId)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public MigrationJobResponse createJob(String baseUrl, String tenantId, MigrationJobRequest request) {
        return createClient(baseUrl, tenantId)
                .post()
                .uri("/api/v1/migrations")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(MigrationJobResponse.class)
                .block();
    }

    public MigrationJobResponse getJob(String baseUrl, String tenantId, UUID jobId) {
        return createClient(baseUrl, tenantId)
                .get()
                .uri("/api/v1/migrations/{id}", jobId)
                .retrieve()
                .bodyToMono(MigrationJobResponse.class)
                .block();
    }

    public List<MigrationJobResponse> listJobs(String baseUrl, String tenantId, JobStatus status, int limit) {
        return createClient(baseUrl, tenantId)
                .get()
                .uri(uriBuilder -> {
                    uriBuilder.path("/api/v1/migrations");
                    if (status != null) {
                        uriBuilder.queryParam("status", status);
                    }
                    uriBuilder.queryParam("limit", limit);
                    return uriBuilder.build();
                })
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<MigrationJobResponse>>() {})
                .block();
    }

    public MigrationJobResponse startJob(String baseUrl, String tenantId, UUID jobId) {
        return createClient(baseUrl, tenantId)
                .post()
                .uri("/api/v1/migrations/{id}/start", jobId)
                .retrieve()
                .bodyToMono(MigrationJobResponse.class)
                .block();
    }

    public MigrationJobResponse pauseJob(String baseUrl, String tenantId, UUID jobId) {
        return createClient(baseUrl, tenantId)
                .post()
                .uri("/api/v1/migrations/{id}/pause", jobId)
                .retrieve()
                .bodyToMono(MigrationJobResponse.class)
                .block();
    }

    public MigrationJobResponse resumeJob(String baseUrl, String tenantId, UUID jobId) {
        return createClient(baseUrl, tenantId)
                .post()
                .uri("/api/v1/migrations/{id}/resume", jobId)
                .retrieve()
                .bodyToMono(MigrationJobResponse.class)
                .block();
    }

    public MigrationJobResponse cancelJob(String baseUrl, String tenantId, UUID jobId) {
        return createClient(baseUrl, tenantId)
                .post()
                .uri("/api/v1/migrations/{id}/cancel", jobId)
                .retrieve()
                .bodyToMono(MigrationJobResponse.class)
                .block();
    }

    public void deleteJob(String baseUrl, String tenantId, UUID jobId) {
        createClient(baseUrl, tenantId)
                .delete()
                .uri("/api/v1/migrations/{id}", jobId)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public MigrationProgress getProgress(String baseUrl, String tenantId, UUID jobId) {
        return createClient(baseUrl, tenantId)
                .get()
                .uri("/api/v1/migrations/{id}/progress", jobId)
                .retrieve()
                .bodyToMono(MigrationProgress.class)
                .block();
    }

    public MigrationSummary getSummary(String baseUrl, String tenantId, UUID jobId) {
        return createClient(baseUrl, tenantId)
                .get()
                .uri("/api/v1/migrations/{id}/summary", jobId)
                .retrieve()
                .bodyToMono(MigrationSummary.class)
                .block();
    }

    public DataQualityReport getQualityReport(String baseUrl, String tenantId, UUID jobId) {
        return createClient(baseUrl, tenantId)
                .get()
                .uri("/api/v1/migrations/{id}/quality", jobId)
                .retrieve()
                .bodyToMono(DataQualityReport.class)
                .block();
    }

    public List<MigrationErrorEntity> getErrors(String baseUrl, String tenantId, UUID jobId, int limit) {
        return createClient(baseUrl, tenantId)
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/migrations/{id}/errors")
                        .queryParam("limit", limit)
                        .build(jobId))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<MigrationErrorEntity>>() {})
                .block();
    }

    public boolean testConnection(String baseUrl, String tenantId,
            com.healthdata.migration.dto.SourceConfig config) {
        try {
            createClient(baseUrl, tenantId)
                    .post()
                    .uri("/api/v1/migrations/test-connection")
                    .bodyValue(config)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
