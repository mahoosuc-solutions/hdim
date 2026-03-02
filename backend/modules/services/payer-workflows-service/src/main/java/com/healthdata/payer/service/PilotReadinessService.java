package com.healthdata.payer.service;

import com.healthdata.payer.domain.PilotReadiness;
import com.healthdata.payer.domain.PilotReadiness.*;
import com.healthdata.payer.dto.PilotOnboardRequest;
import com.healthdata.payer.dto.PilotReadinessResponse;
import com.healthdata.payer.repository.PilotReadinessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PilotReadinessService {

    private final PilotReadinessRepository repository;

    private static final int FHIR_CONNECT_TIMEOUT_MS = 5000;

    /**
     * Onboard a new pilot customer — creates initial readiness record.
     */
    @Transactional
    public PilotReadinessResponse onboard(PilotOnboardRequest request, String tenantId) {
        EhrType ehrType;
        try {
            ehrType = EhrType.valueOf(request.getEhrType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown EHR type: " + request.getEhrType());
        }

        // Check for existing record
        Optional<PilotReadiness> existing = repository.findByCustomerIdAndTenantId(
                request.getCustomerId(), tenantId);
        if (existing.isPresent()) {
            throw new IllegalStateException("Pilot customer already onboarded: " + request.getCustomerId());
        }

        PilotReadiness pilot = PilotReadiness.builder()
                .tenantId(tenantId)
                .customerId(request.getCustomerId())
                .customerName(request.getCustomerName())
                .ehrType(ehrType)
                .integrationStatus(IntegrationStatus.NOT_STARTED)
                .fhirEndpointUrl(request.getFhirEndpointUrl())
                .dataIngestionStatus(DataStatus.NOT_STARTED)
                .demoDataSeeded(false)
                .qualityMeasuresConfigured(false)
                .userAccountsProvisioned(false)
                .readinessScore(0)
                .build();

        pilot = repository.save(pilot);
        return toResponse(pilot);
    }

    /**
     * Get readiness status for a customer.
     */
    public Optional<PilotReadinessResponse> getReadiness(String customerId, String tenantId) {
        return repository.findByCustomerIdAndTenantId(customerId, tenantId)
                .map(this::toResponse);
    }

    /**
     * List all pilot customers for a tenant, ordered by readiness score.
     */
    public List<PilotReadinessResponse> listAll(String tenantId) {
        return repository.findByTenantIdOrderByReadinessScoreDesc(tenantId).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Validate FHIR endpoint connectivity for a pilot customer.
     * Attempts HTTP GET to the FHIR metadata endpoint with a 5-second timeout.
     */
    @Transactional
    public PilotReadinessResponse validateIntegration(String customerId, String tenantId) {
        PilotReadiness pilot = repository.findByCustomerIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Pilot customer not found: " + customerId));

        if (pilot.getFhirEndpointUrl() == null || pilot.getFhirEndpointUrl().isBlank()) {
            pilot.setIntegrationStatus(IntegrationStatus.FAILED);
            pilot.setBlockers("No FHIR endpoint URL configured");
            pilot = repository.save(pilot);
            return toResponse(pilot);
        }

        try {
            String metadataUrl = pilot.getFhirEndpointUrl().replaceAll("/$", "") + "/metadata";
            HttpURLConnection conn = (HttpURLConnection) URI.create(metadataUrl).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(FHIR_CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(FHIR_CONNECT_TIMEOUT_MS);
            conn.setRequestProperty("Accept", "application/fhir+json");

            int status = conn.getResponseCode();
            conn.disconnect();

            if (status >= 200 && status < 300) {
                pilot.setIntegrationStatus(IntegrationStatus.CONNECTED);
                pilot.setBlockers(null);
            } else {
                pilot.setIntegrationStatus(IntegrationStatus.FAILED);
                pilot.setBlockers("FHIR endpoint returned HTTP " + status);
            }
        } catch (Exception e) {
            log.warn("FHIR connectivity check failed for customer {}: {}", customerId, e.getMessage());
            pilot.setIntegrationStatus(IntegrationStatus.FAILED);
            pilot.setBlockers("FHIR endpoint unreachable: " + e.getMessage());
        }

        pilot.calculateReadinessScore();
        pilot = repository.save(pilot);
        return toResponse(pilot);
    }

    private PilotReadinessResponse toResponse(PilotReadiness entity) {
        return PilotReadinessResponse.builder()
                .id(entity.getId())
                .customerId(entity.getCustomerId())
                .customerName(entity.getCustomerName())
                .ehrType(entity.getEhrType().name())
                .integrationStatus(entity.getIntegrationStatus().name())
                .fhirEndpointUrl(entity.getFhirEndpointUrl())
                .dataIngestionStatus(entity.getDataIngestionStatus().name())
                .demoDataSeeded(entity.getDemoDataSeeded())
                .qualityMeasuresConfigured(entity.getQualityMeasuresConfigured())
                .userAccountsProvisioned(entity.getUserAccountsProvisioned())
                .readinessScore(entity.getReadinessScore())
                .blockers(entity.getBlockers())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
