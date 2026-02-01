package com.healthdata.priorauth.service;

import com.healthdata.priorauth.audit.PriorAuthAuditIntegration;
import com.healthdata.priorauth.client.PayerApiClient;
import com.healthdata.priorauth.dto.PriorAuthRequestDTO;
import com.healthdata.priorauth.persistence.PayerEndpointEntity;
import com.healthdata.priorauth.persistence.PayerEndpointRepository;
import com.healthdata.priorauth.persistence.PriorAuthRequestEntity;
import com.healthdata.priorauth.persistence.PriorAuthRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Claim;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Core service for Prior Authorization management.
 *
 * Handles the full PA lifecycle: creation, submission, status tracking,
 * and notification. Integrates with payer APIs using Da Vinci PAS specification.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PriorAuthService {

    private final PriorAuthRequestRepository requestRepository;
    private final PayerEndpointRepository payerEndpointRepository;
    private final PasClaimBuilder claimBuilder;
    private final PayerApiClient payerApiClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final PriorAuthAuditIntegration priorAuthAuditIntegration;

    private static final String PA_EVENTS_TOPIC = "prior-auth-events";

    /**
     * Create a new prior authorization request.
     *
     * @param tenantId The tenant identifier
     * @param request The PA request details
     * @param requestedBy The user creating the request
     * @return The created PA request response
     */
    @Transactional
    public PriorAuthRequestDTO.Response createRequest(String tenantId,
                                                       PriorAuthRequestDTO request,
                                                       String requestedBy) {
        log.info("Creating PA request for patient {} with payer {}",
            request.getPatientId(), request.getPayerId());

        // Validate payer exists
        PayerEndpointEntity payer = payerEndpointRepository.findByPayerId(request.getPayerId())
            .orElseThrow(() -> new IllegalArgumentException("Unknown payer: " + request.getPayerId()));

        // Create entity
        PriorAuthRequestEntity entity = PriorAuthRequestEntity.builder()
            .tenantId(tenantId)
            .patientId(request.getPatientId())
            .paRequestId(generatePaRequestId())
            .serviceCode(request.getServiceCode())
            .serviceDescription(request.getServiceDescription())
            .urgency(request.getUrgency())
            .status(PriorAuthRequestEntity.Status.DRAFT)
            .payerId(request.getPayerId())
            .payerName(payer.getPayerName())
            .providerId(request.getProviderId())
            .providerNpi(request.getProviderNpi())
            .facilityId(request.getFacilityId())
            .diagnosisCodes(request.getDiagnosisCodes() != null ?
                String.join(",", request.getDiagnosisCodes()) : null)
            .procedureCodes(request.getProcedureCodes() != null ?
                String.join(",", request.getProcedureCodes()) : null)
            .quantityRequested(request.getQuantityRequested())
            .supportingInfoJson(request.getSupportingInfo())
            .requestedBy(requestedBy)
            .build();

        entity = requestRepository.save(entity);

        log.info("Created PA request {} for patient {}", entity.getPaRequestId(), request.getPatientId());

        // Publish audit event
        priorAuthAuditIntegration.publishPriorAuthRequestEvent(
            tenantId,
            entity.getId(),
            entity.getPatientId(),
            entity.getPayerId(),
            entity.getServiceCode(),
            entity.getUrgency().name(),
            requestedBy
        );

        return mapToResponse(entity);
    }

    /**
     * Submit a PA request to the payer.
     *
     * @param tenantId The tenant identifier
     * @param requestId The PA request ID
     * @return Updated PA request with submission status
     */
    @Transactional
    public PriorAuthRequestDTO.Response submitRequest(String tenantId, UUID requestId) {
        log.info("Submitting PA request: {}", requestId);

        PriorAuthRequestEntity entity = requestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("PA request not found: " + requestId));

        if (entity.getStatus() != PriorAuthRequestEntity.Status.DRAFT &&
            entity.getStatus() != PriorAuthRequestEntity.Status.ERROR) {
            throw new IllegalStateException("Cannot submit PA request in status: " + entity.getStatus());
        }

        // Build FHIR Claim
        PriorAuthRequestDTO requestDto = mapEntityToDto(entity);
        Claim claim = claimBuilder.buildClaim(requestDto, entity,
            "Patient/" + entity.getPatientId());
        Bundle claimBundle = claimBuilder.buildClaimBundle(claim, null);

        // Store the claim bundle
        Map<String, Object> bundleMap = new HashMap<>();
        bundleMap.put("resourceType", "Bundle");
        bundleMap.put("type", "collection");
        entity.setClaimBundleJson(bundleMap);

        // Update status to pending
        entity.setStatus(PriorAuthRequestEntity.Status.PENDING_SUBMISSION);
        entity.setSubmittedAt(LocalDateTime.now());
        entity.calculateSlaDeadline();
        requestRepository.save(entity);

        // Submit async to payer
        submitToPayerAsync(entity.getId(), claimBundle);

        return mapToResponse(entity);
    }

    /**
     * Asynchronously submit PA request to payer.
     */
    @Async
    public void submitToPayerAsync(UUID requestId, Bundle claimBundle) {
        PriorAuthRequestEntity entity = requestRepository.findById(requestId).orElse(null);
        if (entity == null) return;

        try {
            PayerApiClient.PayerResponse response =
                payerApiClient.submitPriorAuthRequest(entity.getPayerId(), claimBundle);

            if (response.isSuccess()) {
                entity.setStatus(PriorAuthRequestEntity.Status.SUBMITTED);
                entity.setPayerTrackingId(response.getTrackingId());

                if (response.getResponseData() != null) {
                    entity.setClaimResponseJson(response.getResponseData());

                    // Parse response for immediate decision
                    PasClaimBuilder.PaDecision decision =
                        claimBuilder.parseClaimResponse(response.getResponseData());

                    if (decision.getStatus() != null &&
                        decision.getStatus() != PriorAuthRequestEntity.Status.PENDING_REVIEW) {
                        applyDecision(entity, decision);
                    }
                }
            } else {
                entity.setStatus(PriorAuthRequestEntity.Status.ERROR);
                entity.setLastError(response.getErrorMessage());
                entity.setRetryCount(entity.getRetryCount() + 1);
            }

            requestRepository.save(entity);

            // Publish event
            publishPaEvent(entity, "PA_SUBMITTED");

            // Publish audit event
            priorAuthAuditIntegration.publishPriorAuthSubmissionEvent(
                entity.getTenantId(),
                entity.getId(),
                entity.getPatientId(),
                entity.getPayerId(),
                entity.getStatus() == PriorAuthRequestEntity.Status.SUBMITTED,
                entity.getLastError(),
                entity.getSubmittedAt() != null ? 
                    java.time.Duration.between(entity.getCreatedAt(), entity.getSubmittedAt()).toMillis() : 0,
                entity.getRequestedBy()
            );

        } catch (Exception e) {
            log.error("Failed to submit PA request to payer: {}", e.getMessage());
            entity.setStatus(PriorAuthRequestEntity.Status.ERROR);
            entity.setLastError(e.getMessage());
            entity.setRetryCount(entity.getRetryCount() + 1);
            requestRepository.save(entity);
        }
    }

    /**
     * Check status of a PA request with the payer.
     *
     * @param tenantId The tenant identifier
     * @param requestId The PA request ID
     * @return Updated PA request with current status
     */
    @Transactional
    public PriorAuthRequestDTO.Response checkStatus(String tenantId, UUID requestId) {
        log.info("Checking PA status for request: {}", requestId);

        PriorAuthRequestEntity entity = requestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("PA request not found: " + requestId));

        if (entity.getStatus() != PriorAuthRequestEntity.Status.SUBMITTED &&
            entity.getStatus() != PriorAuthRequestEntity.Status.PENDING_REVIEW) {
            return mapToResponse(entity);
        }

        try {
            PayerApiClient.PayerResponse response =
                payerApiClient.checkPaStatus(entity.getPayerId(), entity.getPaRequestId());

            if (response.isSuccess() && response.getResponseData() != null) {
                entity.setClaimResponseJson(response.getResponseData());

                PasClaimBuilder.PaDecision decision =
                    claimBuilder.parseClaimResponse(response.getResponseData());

                if (decision.getStatus() != null) {
                    applyDecision(entity, decision);
                    requestRepository.save(entity);

                    // Publish status update event
                    publishPaEvent(entity, "PA_STATUS_UPDATED");
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check PA status: {}", e.getMessage());
        }

        return mapToResponse(entity);
    }

    /**
     * Cancel a PA request.
     *
     * @param tenantId The tenant identifier
     * @param requestId The PA request ID
     * @return Updated PA request
     */
    @Transactional
    public PriorAuthRequestDTO.Response cancelRequest(String tenantId, UUID requestId) {
        log.info("Cancelling PA request: {}", requestId);

        PriorAuthRequestEntity entity = requestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("PA request not found: " + requestId));

        if (entity.getStatus() == PriorAuthRequestEntity.Status.APPROVED ||
            entity.getStatus() == PriorAuthRequestEntity.Status.DENIED) {
            throw new IllegalStateException("Cannot cancel PA request in status: " + entity.getStatus());
        }

        // If already submitted, notify payer
        if (entity.getStatus() == PriorAuthRequestEntity.Status.SUBMITTED ||
            entity.getStatus() == PriorAuthRequestEntity.Status.PENDING_REVIEW) {
            try {
                payerApiClient.cancelPriorAuthRequest(entity.getPayerId(), entity.getPaRequestId());
            } catch (Exception e) {
                log.warn("Failed to notify payer of cancellation: {}", e.getMessage());
            }
        }

        entity.setStatus(PriorAuthRequestEntity.Status.CANCELLED);
        requestRepository.save(entity);

        publishPaEvent(entity, "PA_CANCELLED");

        return mapToResponse(entity);
    }

    /**
     * Get PA request by ID.
     */
    public PriorAuthRequestDTO.Response getRequest(String tenantId, UUID requestId) {
        PriorAuthRequestEntity entity = requestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("PA request not found: " + requestId));

        if (!entity.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("PA request not found: " + requestId);
        }

        return mapToResponse(entity);
    }

    /**
     * Get PA requests for a patient.
     */
    public Page<PriorAuthRequestDTO.Response> getPatientRequests(String tenantId,
                                                                  UUID patientId,
                                                                  Pageable pageable) {
        return requestRepository.findByTenantIdAndPatientId(tenantId, patientId, pageable)
            .map(this::mapToResponse);
    }

    /**
     * Get PA requests by status.
     */
    public Page<PriorAuthRequestDTO.Response> getRequestsByStatus(String tenantId,
                                                                   PriorAuthRequestEntity.Status status,
                                                                   Pageable pageable) {
        return requestRepository.findByTenantIdAndStatus(tenantId, status, pageable)
            .map(this::mapToResponse);
    }

    /**
     * Get PA requests approaching SLA deadline.
     */
    public List<PriorAuthRequestDTO.Response> getApproachingSlaDeadline(String tenantId,
                                                                         int hoursUntilDeadline) {
        LocalDateTime deadline = LocalDateTime.now().plusHours(hoursUntilDeadline);
        List<PriorAuthRequestEntity.Status> pendingStatuses = List.of(
            PriorAuthRequestEntity.Status.SUBMITTED,
            PriorAuthRequestEntity.Status.PENDING_REVIEW
        );

        return requestRepository.findApproachingSlaDeadline(tenantId, deadline, pendingStatuses)
            .stream()
            .map(this::mapToResponse)
            .toList();
    }

    /**
     * Get PA statistics for a tenant.
     */
    public PriorAuthRequestDTO.Statistics getStatistics(String tenantId) {
        List<Object[]> statusCounts = requestRepository.getStatusCounts(tenantId);

        long total = 0, pending = 0, approved = 0, denied = 0, expired = 0;

        for (Object[] row : statusCounts) {
            PriorAuthRequestEntity.Status status = (PriorAuthRequestEntity.Status) row[0];
            long count = (Long) row[1];
            total += count;

            switch (status) {
                case SUBMITTED, PENDING_REVIEW, INFO_REQUESTED -> pending += count;
                case APPROVED, PARTIALLY_APPROVED -> approved += count;
                case DENIED -> denied += count;
                case EXPIRED -> expired += count;
            }
        }

        double approvalRate = total > 0 ? (double) approved / total * 100 : 0;

        return PriorAuthRequestDTO.Statistics.builder()
            .totalRequests(total)
            .pendingRequests(pending)
            .approvedRequests(approved)
            .deniedRequests(denied)
            .expiredRequests(expired)
            .approvalRate(approvalRate)
            .build();
    }

    private void applyDecision(PriorAuthRequestEntity entity, PasClaimBuilder.PaDecision decision) {
        entity.setStatus(decision.getStatus());
        entity.setDecisionAt(LocalDateTime.now());
        entity.setDecisionReason(decision.getReason());
        entity.setAuthNumber(decision.getAuthNumber());
        entity.setQuantityApproved(decision.getApprovedQuantity());
        entity.setAuthEffectiveDate(decision.getEffectiveDate());
        entity.setAuthExpirationDate(decision.getExpirationDate());
    }

    private void publishPaEvent(PriorAuthRequestEntity entity, String eventType) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("tenantId", entity.getTenantId());
            event.put("paRequestId", entity.getPaRequestId());
            event.put("patientId", entity.getPatientId().toString());
            event.put("status", entity.getStatus().name());
            event.put("timestamp", LocalDateTime.now().toString());

            kafkaTemplate.send(PA_EVENTS_TOPIC, entity.getPaRequestId(), event);
        } catch (Exception e) {
            log.warn("Failed to publish PA event: {}", e.getMessage());
        }
    }

    private String generatePaRequestId() {
        return "PA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private PriorAuthRequestDTO mapEntityToDto(PriorAuthRequestEntity entity) {
        return PriorAuthRequestDTO.builder()
            .patientId(entity.getPatientId())
            .serviceCode(entity.getServiceCode())
            .serviceDescription(entity.getServiceDescription())
            .urgency(entity.getUrgency())
            .payerId(entity.getPayerId())
            .providerId(entity.getProviderId())
            .providerNpi(entity.getProviderNpi())
            .facilityId(entity.getFacilityId())
            .diagnosisCodes(entity.getDiagnosisCodes() != null ?
                Arrays.asList(entity.getDiagnosisCodes().split(",")) : null)
            .procedureCodes(entity.getProcedureCodes() != null ?
                Arrays.asList(entity.getProcedureCodes().split(",")) : null)
            .quantityRequested(entity.getQuantityRequested())
            .supportingInfo(entity.getSupportingInfoJson())
            .build();
    }

    private PriorAuthRequestDTO.Response mapToResponse(PriorAuthRequestEntity entity) {
        return PriorAuthRequestDTO.Response.builder()
            .id(entity.getId())
            .paRequestId(entity.getPaRequestId())
            .patientId(entity.getPatientId())
            .serviceCode(entity.getServiceCode())
            .serviceDescription(entity.getServiceDescription())
            .urgency(entity.getUrgency())
            .status(entity.getStatus())
            .payerId(entity.getPayerId())
            .payerName(entity.getPayerName())
            .providerId(entity.getProviderId())
            .providerNpi(entity.getProviderNpi())
            .facilityId(entity.getFacilityId())
            .diagnosisCodes(entity.getDiagnosisCodes() != null ?
                Arrays.asList(entity.getDiagnosisCodes().split(",")) : null)
            .procedureCodes(entity.getProcedureCodes() != null ?
                Arrays.asList(entity.getProcedureCodes().split(",")) : null)
            .quantityRequested(entity.getQuantityRequested())
            .quantityApproved(entity.getQuantityApproved())
            .submittedAt(entity.getSubmittedAt())
            .slaDeadline(entity.getSlaDeadline())
            .decisionAt(entity.getDecisionAt())
            .decisionReason(entity.getDecisionReason())
            .authNumber(entity.getAuthNumber())
            .authEffectiveDate(entity.getAuthEffectiveDate())
            .authExpirationDate(entity.getAuthExpirationDate())
            .slaBreached(entity.isSlaBreached())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }
}
