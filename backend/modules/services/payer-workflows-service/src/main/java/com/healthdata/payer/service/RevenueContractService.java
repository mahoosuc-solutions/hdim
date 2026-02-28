package com.healthdata.payer.service;

import com.healthdata.payer.revenue.RevenueClaimState;
import com.healthdata.payer.revenue.RevenueErrorCode;
import com.healthdata.payer.revenue.dto.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class RevenueContractService {
    private static final int MAX_SUBMISSION_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MILLIS = 100L;

    private final ClearinghouseSubmissionAdapter clearinghouseSubmissionAdapter;
    private final ClearinghouseBackoffExecutor backoffExecutor;

    private final Map<String, ClaimSubmissionResponse> claimSubmissionsByIdempotencyKey = new ConcurrentHashMap<>();
    private final Map<String, RevenueClaimState> claimStatusByClaimId = new ConcurrentHashMap<>();
    private final Map<String, BigDecimal> claimTotalByClaimId = new ConcurrentHashMap<>();
    private final Map<String, List<RevenueAuditEnvelope>> auditByCorrelationId = new ConcurrentHashMap<>();

    public RevenueContractService() {
        this(
                (request, attempt) -> new ClearinghouseSubmissionResult(
                        true,
                        "ACK-" + request.getClaimId() + "-A" + attempt
                ),
                millis -> {
                    // Intentionally no-op in current scaffold.
                }
        );
    }

    RevenueContractService(
            ClearinghouseSubmissionAdapter clearinghouseSubmissionAdapter,
            ClearinghouseBackoffExecutor backoffExecutor
    ) {
        this.clearinghouseSubmissionAdapter = clearinghouseSubmissionAdapter;
        this.backoffExecutor = backoffExecutor;
    }

    public ClaimSubmissionResponse submitClaim(ClaimSubmissionRequest request) {
        ClaimSubmissionResponse existing = claimSubmissionsByIdempotencyKey.get(request.getIdempotencyKey());
        if (existing != null) {
            appendAudit(existing.getCorrelationId(), audit(
                    existing.getTenantId(),
                    existing.getCorrelationId(),
                    request.getActor(),
                    "CLAIM_SUBMISSION_REPLAY",
                    "DUPLICATE_SUPPRESSED"
            ));

            return ClaimSubmissionResponse.builder()
                    .tenantId(existing.getTenantId())
                    .claimId(existing.getClaimId())
                    .correlationId(existing.getCorrelationId())
                    .status(existing.getStatus())
                    .duplicate(true)
                    .auditEnvelope(audit(
                            existing.getTenantId(),
                            existing.getCorrelationId(),
                            request.getActor(),
                            "CLAIM_SUBMISSION_REPLAY",
                            "DUPLICATE_SUPPRESSED"))
                    .build();
        }

        claimStatusByClaimId.put(request.getClaimId(), RevenueClaimState.PENDING_SUBMIT);
        claimTotalByClaimId.put(request.getClaimId(), request.getTotalAmount());

        int attempt = 1;
        long backoff = INITIAL_BACKOFF_MILLIS;
        RevenueClaimState finalStatus = RevenueClaimState.REJECTED;
        RevenueErrorCode errorCode = null;
        String outcome = "SUBMISSION_REJECTED";
        while (attempt <= MAX_SUBMISSION_ATTEMPTS) {
            try {
                ClearinghouseSubmissionResult result = clearinghouseSubmissionAdapter.submit(request, attempt);
                finalStatus = result.accepted() ? RevenueClaimState.SUBMITTED : RevenueClaimState.REJECTED;
                outcome = "SUBMITTED";
                break;
            } catch (NonRetryableClearinghouseException e) {
                finalStatus = RevenueClaimState.REJECTED;
                errorCode = RevenueErrorCode.NON_RETRYABLE_UPSTREAM;
                outcome = "NON_RETRYABLE_REJECTED";
                break;
            } catch (RetryableClearinghouseException e) {
                if (attempt == MAX_SUBMISSION_ATTEMPTS) {
                    finalStatus = RevenueClaimState.REJECTED;
                    errorCode = RevenueErrorCode.UPSTREAM_TIMEOUT;
                    outcome = "RETRY_EXHAUSTED";
                    break;
                }
                backoffExecutor.backoff(backoff);
                backoff = backoff * 2;
                attempt++;
            }
        }

        claimStatusByClaimId.put(request.getClaimId(), finalStatus);
        ClaimSubmissionResponse response = ClaimSubmissionResponse.builder()
                .tenantId(request.getTenantId())
                .claimId(request.getClaimId())
                .correlationId(request.getCorrelationId())
                .status(finalStatus)
                .duplicate(false)
                .errorCode(errorCode)
                .auditEnvelope(audit(
                        request.getTenantId(),
                        request.getCorrelationId(),
                        request.getActor(),
                        "CLAIM_SUBMISSION",
                        outcome))
                .build();

        claimSubmissionsByIdempotencyKey.put(request.getIdempotencyKey(), response);
        appendAudit(response.getCorrelationId(), response.getAuditEnvelope());
        return response;
    }

    public ReconciliationPreviewResponse ingestRemittanceAdvice(RemittanceAdviceEvent request) {
        RevenueClaimState priorStatus = claimStatusByClaimId.get(request.getClaimId());
        BigDecimal claimTotal = claimTotalByClaimId.get(request.getClaimId());
        if (priorStatus == null || claimTotal == null) {
            ReconciliationPreviewResponse response = ReconciliationPreviewResponse.builder()
                    .tenantId(request.getTenantId())
                    .claimId(request.getClaimId())
                    .remittanceId(request.getRemittanceId())
                    .correlationId(request.getCorrelationId())
                    .priorStatus(RevenueClaimState.REJECTED)
                    .newStatus(RevenueClaimState.REJECTED)
                    .paidAmount(request.getPaymentAmount())
                    .adjustmentAmount(request.getAdjustmentAmount())
                    .remainingBalance(BigDecimal.ZERO)
                    .errorCode(RevenueErrorCode.NON_RETRYABLE_UPSTREAM)
                    .auditEnvelope(audit(
                            request.getTenantId(),
                            request.getCorrelationId(),
                            request.getActor(),
                            "REMITTANCE_INGEST",
                            "CLAIM_NOT_FOUND"))
                    .build();
            appendAudit(response.getCorrelationId(), response.getAuditEnvelope());
            return response;
        }

        BigDecimal applied = request.getPaymentAmount().add(request.getAdjustmentAmount());
        BigDecimal remainingBalance = claimTotal.subtract(applied);
        RevenueClaimState newStatus = remainingBalance.compareTo(BigDecimal.ZERO) <= 0
                ? RevenueClaimState.PAID
                : RevenueClaimState.PARTIALLY_PAID;
        claimStatusByClaimId.put(request.getClaimId(), newStatus);

        ReconciliationPreviewResponse response = ReconciliationPreviewResponse.builder()
                .tenantId(request.getTenantId())
                .claimId(request.getClaimId())
                .remittanceId(request.getRemittanceId())
                .correlationId(request.getCorrelationId())
                .priorStatus(priorStatus)
                .newStatus(newStatus)
                .paidAmount(request.getPaymentAmount())
                .adjustmentAmount(request.getAdjustmentAmount())
                .remainingBalance(remainingBalance.max(BigDecimal.ZERO))
                .auditEnvelope(audit(
                        request.getTenantId(),
                        request.getCorrelationId(),
                        request.getActor(),
                        "REMITTANCE_INGEST",
                        "RECONCILIATION_PREVIEW_READY"))
                .build();
        appendAudit(response.getCorrelationId(), response.getAuditEnvelope());
        return response;
    }

    public EligibilityCheckResponse checkEligibility(EligibilityCheckRequest request) {
        boolean eligible = !"BLOCKED-PAYER".equalsIgnoreCase(request.getPayerId());
        EligibilityCheckResponse response = EligibilityCheckResponse.builder()
                .tenantId(request.getTenantId())
                .payerId(request.getPayerId())
                .patientId(request.getPatientId())
                .correlationId(request.getCorrelationId())
                .eligible(eligible)
                .errorCode(eligible ? null : RevenueErrorCode.NON_RETRYABLE_UPSTREAM)
                .auditEnvelope(audit(
                        request.getTenantId(),
                        request.getCorrelationId(),
                        request.getActor(),
                        "ELIGIBILITY_CHECK",
                        eligible ? "ELIGIBLE" : "INELIGIBLE"))
                .build();
        appendAudit(response.getCorrelationId(), response.getAuditEnvelope());
        return response;
    }

    public ClaimStatusResponse checkClaimStatus(ClaimStatusRequest request) {
        RevenueClaimState status = claimStatusByClaimId.get(request.getClaimId());
        if (status == null) {
            ClaimStatusResponse response = ClaimStatusResponse.builder()
                    .tenantId(request.getTenantId())
                    .claimId(request.getClaimId())
                    .correlationId(request.getCorrelationId())
                    .status(RevenueClaimState.REJECTED)
                    .errorCode(RevenueErrorCode.NON_RETRYABLE_UPSTREAM)
                    .auditEnvelope(audit(
                            request.getTenantId(),
                            request.getCorrelationId(),
                            request.getActor(),
                            "CLAIM_STATUS_CHECK",
                            "CLAIM_NOT_FOUND"))
                    .build();
            appendAudit(response.getCorrelationId(), response.getAuditEnvelope());
            return response;
        }

        ClaimStatusResponse response = ClaimStatusResponse.builder()
                .tenantId(request.getTenantId())
                .claimId(request.getClaimId())
                .correlationId(request.getCorrelationId())
                .status(status)
                .auditEnvelope(audit(
                        request.getTenantId(),
                        request.getCorrelationId(),
                        request.getActor(),
                        "CLAIM_STATUS_CHECK",
                        "STATUS_RETURNED"))
                .build();
        appendAudit(response.getCorrelationId(), response.getAuditEnvelope());
        return response;
    }

    public List<RevenueAuditEnvelope> getAuditTrail(String correlationId) {
        return auditByCorrelationId.getOrDefault(correlationId, List.of());
    }

    private void appendAudit(String correlationId, RevenueAuditEnvelope envelope) {
        auditByCorrelationId
                .computeIfAbsent(correlationId, unused -> new CopyOnWriteArrayList<>())
                .add(envelope);
    }

    private RevenueAuditEnvelope audit(
            String tenantId,
            String correlationId,
            String actor,
            String action,
            String outcome
    ) {
        return RevenueAuditEnvelope.builder()
                .tenantId(tenantId)
                .correlationId(correlationId)
                .actor(actor)
                .timestamp(Instant.now())
                .action(action)
                .outcome(outcome)
                .build();
    }
}
