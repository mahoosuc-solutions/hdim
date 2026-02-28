package com.healthdata.payer.service;

import com.healthdata.payer.revenue.RevenueClaimState;
import com.healthdata.payer.revenue.RevenueErrorCode;
import com.healthdata.payer.revenue.dto.*;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

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
    private final Map<String, PriceTransparencySnapshot> priceTransparencySnapshotsByVersionId = new ConcurrentHashMap<>();
    private final Map<String, String> currentPriceTransparencyVersionByTenant = new ConcurrentHashMap<>();
    private final AtomicLong priceTransparencyVersionSequence = new AtomicLong(0L);

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
        if (!isAllowedTransition(priorStatus, newStatus)) {
            ReconciliationPreviewResponse response = ReconciliationPreviewResponse.builder()
                    .tenantId(request.getTenantId())
                    .claimId(request.getClaimId())
                    .remittanceId(request.getRemittanceId())
                    .correlationId(request.getCorrelationId())
                    .priorStatus(priorStatus)
                    .newStatus(priorStatus)
                    .paidAmount(request.getPaymentAmount())
                    .adjustmentAmount(request.getAdjustmentAmount())
                    .remainingBalance(claimTotal.max(BigDecimal.ZERO))
                    .errorCode(RevenueErrorCode.VALIDATION_ERROR)
                    .auditEnvelope(audit(
                            request.getTenantId(),
                            request.getCorrelationId(),
                            request.getActor(),
                            "REMITTANCE_INGEST",
                            "ILLEGAL_STATE_TRANSITION"))
                    .build();
            appendAudit(response.getCorrelationId(), response.getAuditEnvelope());
            return response;
        }

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

    private boolean isAllowedTransition(RevenueClaimState from, RevenueClaimState to) {
        if (from == to) {
            return true;
        }
        return switch (from) {
            case DRAFT -> to == RevenueClaimState.PENDING_SUBMIT;
            case PENDING_SUBMIT -> to == RevenueClaimState.SUBMITTED || to == RevenueClaimState.REJECTED;
            case SUBMITTED -> to == RevenueClaimState.ACKNOWLEDGED
                    || to == RevenueClaimState.REJECTED
                    || to == RevenueClaimState.PARTIALLY_PAID
                    || to == RevenueClaimState.PAID
                    || to == RevenueClaimState.DENIED;
            case ACKNOWLEDGED -> to == RevenueClaimState.PARTIALLY_PAID
                    || to == RevenueClaimState.PAID
                    || to == RevenueClaimState.DENIED;
            case PARTIALLY_PAID -> to == RevenueClaimState.PAID
                    || to == RevenueClaimState.DENIED;
            case PAID -> false;
            case DENIED -> to == RevenueClaimState.UNDER_REVIEW;
            case UNDER_REVIEW -> false;
            case REJECTED -> false;
        };
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

    public PriceTransparencyRatePublishResponse publishPriceTransparencyRates(PriceTransparencyRatePublishRequest request) {
        List<PriceTransparencyRateEntry> normalizedRates = normalizeRates(request.getRates());
        validateDuplicateServiceCodes(normalizedRates);

        String versionId = "PTR-" + priceTransparencyVersionSequence.incrementAndGet();
        String checksum = computeChecksum(normalizedRates);
        Instant publishedAt = Instant.now();

        PriceTransparencySnapshot snapshot = new PriceTransparencySnapshot(
                request.getTenantId(),
                versionId,
                request.getSourceReference(),
                checksum,
                publishedAt,
                request.getActor(),
                normalizedRates
        );
        priceTransparencySnapshotsByVersionId.put(versionId, snapshot);
        currentPriceTransparencyVersionByTenant.put(request.getTenantId(), versionId);

        PriceTransparencyRatePublishResponse response = PriceTransparencyRatePublishResponse.builder()
                .tenantId(request.getTenantId())
                .versionId(versionId)
                .sourceReference(request.getSourceReference())
                .checksum(checksum)
                .lineItemCount(normalizedRates.size())
                .publishedAt(publishedAt)
                .publishedBy(request.getActor())
                .auditEnvelope(audit(
                        request.getTenantId(),
                        request.getCorrelationId(),
                        request.getActor(),
                        "PRICE_TRANSPARENCY_PUBLISH",
                        "VERSION_PUBLISHED"))
                .build();
        appendAudit(request.getCorrelationId(), response.getAuditEnvelope());
        return response;
    }

    public PriceTransparencyRatesViewResponse getCurrentPriceTransparencyRates(
            String tenantId,
            String correlationId,
            String actor
    ) {
        String versionId = currentPriceTransparencyVersionByTenant.get(tenantId);
        if (versionId == null) {
            return null;
        }
        return getPriceTransparencyRatesVersion(tenantId, versionId, correlationId, actor);
    }

    public PriceTransparencyRatesViewResponse getPriceTransparencyRatesVersion(
            String tenantId,
            String versionId,
            String correlationId,
            String actor
    ) {
        PriceTransparencySnapshot snapshot = priceTransparencySnapshotsByVersionId.get(versionId);
        if (snapshot == null || !snapshot.tenantId().equals(tenantId)) {
            return null;
        }

        PriceTransparencyRatesViewResponse response = PriceTransparencyRatesViewResponse.builder()
                .tenantId(snapshot.tenantId())
                .versionId(snapshot.versionId())
                .sourceReference(snapshot.sourceReference())
                .checksum(snapshot.checksum())
                .publishedAt(snapshot.publishedAt())
                .publishedBy(snapshot.publishedBy())
                .rates(new ArrayList<>(snapshot.rates()))
                .auditEnvelope(audit(
                        tenantId,
                        correlationId,
                        actor,
                        "PRICE_TRANSPARENCY_READ_VERSION",
                        "VERSION_RETURNED"))
                .build();
        appendAudit(correlationId, response.getAuditEnvelope());
        return response;
    }

    public boolean hasPriceTransparencyVersion(String tenantId, String versionId) {
        PriceTransparencySnapshot snapshot = priceTransparencySnapshotsByVersionId.get(versionId);
        return snapshot != null && snapshot.tenantId().equals(tenantId);
    }

    public PriceEstimateResponse estimatePrice(PriceEstimateRequest request) {
        String resolvedVersionId = request.getVersionId();
        if (resolvedVersionId == null || resolvedVersionId.isBlank()) {
            resolvedVersionId = currentPriceTransparencyVersionByTenant.get(request.getTenantId());
        }

        if (resolvedVersionId == null) {
            PriceEstimateResponse response = PriceEstimateResponse.builder()
                    .tenantId(request.getTenantId())
                    .serviceCode(request.getServiceCode())
                    .units(request.getUnits())
                    .correlationId(request.getCorrelationId())
                    .errorCode(RevenueErrorCode.NON_RETRYABLE_UPSTREAM)
                    .auditEnvelope(audit(
                            request.getTenantId(),
                            request.getCorrelationId(),
                            request.getActor(),
                            "PRICE_ESTIMATE",
                            "NO_ACTIVE_VERSION"))
                    .build();
            appendAudit(request.getCorrelationId(), response.getAuditEnvelope());
            return response;
        }

        PriceTransparencySnapshot snapshot = priceTransparencySnapshotsByVersionId.get(resolvedVersionId);
        if (snapshot == null || !snapshot.tenantId().equals(request.getTenantId())) {
            PriceEstimateResponse response = PriceEstimateResponse.builder()
                    .tenantId(request.getTenantId())
                    .versionId(resolvedVersionId)
                    .serviceCode(request.getServiceCode())
                    .units(request.getUnits())
                    .correlationId(request.getCorrelationId())
                    .errorCode(RevenueErrorCode.NON_RETRYABLE_UPSTREAM)
                    .auditEnvelope(audit(
                            request.getTenantId(),
                            request.getCorrelationId(),
                            request.getActor(),
                            "PRICE_ESTIMATE",
                            "VERSION_NOT_FOUND"))
                    .build();
            appendAudit(request.getCorrelationId(), response.getAuditEnvelope());
            return response;
        }

        PriceTransparencyRateEntry rateEntry = snapshot.rates().stream()
                .filter(item -> item.getServiceCode().equals(request.getServiceCode()))
                .findFirst()
                .orElse(null);
        if (rateEntry == null) {
            PriceEstimateResponse response = PriceEstimateResponse.builder()
                    .tenantId(request.getTenantId())
                    .versionId(resolvedVersionId)
                    .serviceCode(request.getServiceCode())
                    .units(request.getUnits())
                    .correlationId(request.getCorrelationId())
                    .errorCode(RevenueErrorCode.VALIDATION_ERROR)
                    .auditEnvelope(audit(
                            request.getTenantId(),
                            request.getCorrelationId(),
                            request.getActor(),
                            "PRICE_ESTIMATE",
                            "SERVICE_CODE_NOT_FOUND"))
                    .build();
            appendAudit(request.getCorrelationId(), response.getAuditEnvelope());
            return response;
        }

        BigDecimal unitRate = rateEntry.getNegotiatedRate().setScale(2, RoundingMode.HALF_UP);
        BigDecimal estimatedAllowedAmount = unitRate
                .multiply(BigDecimal.valueOf(request.getUnits()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal estimatedPatientResponsibility = estimatedAllowedAmount
                .multiply(BigDecimal.valueOf(0.20d))
                .setScale(2, RoundingMode.HALF_UP);

        PriceEstimateResponse response = PriceEstimateResponse.builder()
                .tenantId(request.getTenantId())
                .versionId(resolvedVersionId)
                .serviceCode(request.getServiceCode())
                .units(request.getUnits())
                .unitRate(unitRate)
                .estimatedAllowedAmount(estimatedAllowedAmount)
                .estimatedPatientResponsibility(estimatedPatientResponsibility)
                .correlationId(request.getCorrelationId())
                .auditEnvelope(audit(
                        request.getTenantId(),
                        request.getCorrelationId(),
                        request.getActor(),
                        "PRICE_ESTIMATE",
                        "ESTIMATE_READY"))
                .build();
        appendAudit(request.getCorrelationId(), response.getAuditEnvelope());
        return response;
    }

    private List<PriceTransparencyRateEntry> normalizeRates(List<PriceTransparencyRateEntry> rates) {
        return rates.stream()
                .map(rate -> PriceTransparencyRateEntry.builder()
                        .serviceCode(rate.getServiceCode())
                        .negotiatedRate(rate.getNegotiatedRate().setScale(2, RoundingMode.HALF_UP))
                        .cashPrice(rate.getCashPrice().setScale(2, RoundingMode.HALF_UP))
                        .build())
                .sorted(Comparator.comparing(PriceTransparencyRateEntry::getServiceCode))
                .collect(Collectors.toList());
    }

    private void validateDuplicateServiceCodes(List<PriceTransparencyRateEntry> rates) {
        long uniqueCodes = rates.stream()
                .map(PriceTransparencyRateEntry::getServiceCode)
                .distinct()
                .count();
        if (uniqueCodes != rates.size()) {
            throw new IllegalArgumentException("Duplicate serviceCode values are not allowed");
        }
    }

    private String computeChecksum(List<PriceTransparencyRateEntry> rates) {
        String payload = rates.stream()
                .map(rate -> rate.getServiceCode() + "|" + rate.getNegotiatedRate() + "|" + rate.getCashPrice())
                .collect(Collectors.joining(";"));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
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

    private record PriceTransparencySnapshot(
            String tenantId,
            String versionId,
            String sourceReference,
            String checksum,
            Instant publishedAt,
            String publishedBy,
            List<PriceTransparencyRateEntry> rates
    ) {
    }
}
