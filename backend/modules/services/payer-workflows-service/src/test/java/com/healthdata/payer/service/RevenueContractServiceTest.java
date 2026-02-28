package com.healthdata.payer.service;

import com.healthdata.payer.revenue.RevenueClaimState;
import com.healthdata.payer.revenue.RevenueErrorCode;
import com.healthdata.payer.revenue.dto.ClaimSubmissionRequest;
import com.healthdata.payer.revenue.dto.PriceEstimateRequest;
import com.healthdata.payer.revenue.dto.PriceTransparencyRateEntry;
import com.healthdata.payer.revenue.dto.PriceTransparencyRatePublishRequest;
import com.healthdata.payer.revenue.dto.ReconciliationPreviewResponse;
import com.healthdata.payer.revenue.dto.RemittanceAdviceEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RevenueContractService Tests")
class RevenueContractServiceTest {

    private final RevenueContractService revenueContractService = new RevenueContractService();

    @Test
    @DisplayName("ingestRemittanceAdvice sets claim to PARTIALLY_PAID when residual balance remains")
    void shouldSetPartiallyPaidStatus() {
        revenueContractService.submitClaim(ClaimSubmissionRequest.builder()
                .tenantId("tenant-a")
                .claimId("clm-001")
                .patientId("pat-001")
                .payerId("payer-a")
                .totalAmount(BigDecimal.valueOf(500))
                .idempotencyKey("idem-001")
                .correlationId("corr-001")
                .actor("test")
                .build());

        ReconciliationPreviewResponse response = revenueContractService.ingestRemittanceAdvice(RemittanceAdviceEvent.builder()
                .tenantId("tenant-a")
                .claimId("clm-001")
                .remittanceId("rem-001")
                .paymentAmount(BigDecimal.valueOf(350))
                .adjustmentAmount(BigDecimal.ZERO)
                .correlationId("corr-001")
                .actor("test")
                .build());

        assertThat(response.getNewStatus()).isEqualTo(RevenueClaimState.PARTIALLY_PAID);
        assertThat(response.getRemainingBalance()).isEqualByComparingTo(BigDecimal.valueOf(150));
    }

    @Test
    @DisplayName("ingestRemittanceAdvice sets claim to PAID when remittance covers total")
    void shouldSetPaidStatus() {
        revenueContractService.submitClaim(ClaimSubmissionRequest.builder()
                .tenantId("tenant-a")
                .claimId("clm-002")
                .patientId("pat-002")
                .payerId("payer-a")
                .totalAmount(BigDecimal.valueOf(500))
                .idempotencyKey("idem-002")
                .correlationId("corr-002")
                .actor("test")
                .build());

        ReconciliationPreviewResponse response = revenueContractService.ingestRemittanceAdvice(RemittanceAdviceEvent.builder()
                .tenantId("tenant-a")
                .claimId("clm-002")
                .remittanceId("rem-002")
                .paymentAmount(BigDecimal.valueOf(450))
                .adjustmentAmount(BigDecimal.valueOf(50))
                .correlationId("corr-002")
                .actor("test")
                .build());

        assertThat(response.getNewStatus()).isEqualTo(RevenueClaimState.PAID);
        assertThat(response.getRemainingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("submitClaim retries transient failures with exponential backoff and then succeeds")
    void shouldRetryAndEventuallySucceed() {
        AtomicInteger attempts = new AtomicInteger(0);
        List<Long> backoffCalls = new ArrayList<>();
        RevenueContractService service = new RevenueContractService(
                (request, attempt) -> {
                    if (attempts.incrementAndGet() < 3) {
                        throw new RetryableClearinghouseException("temporary timeout");
                    }
                    return new ClearinghouseSubmissionResult(true, "ack-123");
                },
                backoffCalls::add
        );

        var response = service.submitClaim(ClaimSubmissionRequest.builder()
                .tenantId("tenant-a")
                .claimId("clm-retry-success")
                .patientId("pat-retry")
                .payerId("payer-a")
                .totalAmount(BigDecimal.valueOf(250))
                .idempotencyKey("idem-retry-success")
                .correlationId("corr-retry-success")
                .actor("test")
                .build());

        assertThat(response.getStatus()).isEqualTo(RevenueClaimState.SUBMITTED);
        assertThat(response.getErrorCode()).isNull();
        assertThat(attempts.get()).isEqualTo(3);
        assertThat(backoffCalls).containsExactly(100L, 200L);
    }

    @Test
    @DisplayName("submitClaim returns UPSTREAM_TIMEOUT when retries are exhausted")
    void shouldFailAfterRetryExhaustion() {
        AtomicInteger attempts = new AtomicInteger(0);
        List<Long> backoffCalls = new ArrayList<>();
        RevenueContractService service = new RevenueContractService(
                (request, attempt) -> {
                    attempts.incrementAndGet();
                    throw new RetryableClearinghouseException("always timeout");
                },
                backoffCalls::add
        );

        var response = service.submitClaim(ClaimSubmissionRequest.builder()
                .tenantId("tenant-a")
                .claimId("clm-retry-fail")
                .patientId("pat-retry-fail")
                .payerId("payer-a")
                .totalAmount(BigDecimal.valueOf(250))
                .idempotencyKey("idem-retry-fail")
                .correlationId("corr-retry-fail")
                .actor("test")
                .build());

        assertThat(response.getStatus()).isEqualTo(RevenueClaimState.REJECTED);
        assertThat(response.getErrorCode()).isEqualTo(RevenueErrorCode.UPSTREAM_TIMEOUT);
        assertThat(attempts.get()).isEqualTo(3);
        assertThat(backoffCalls).containsExactly(100L, 200L);
    }

    @Test
    @DisplayName("ingestRemittanceAdvice blocks illegal regression from PAID to PARTIALLY_PAID")
    void shouldBlockIllegalPaidToPartialRegression() {
        revenueContractService.submitClaim(ClaimSubmissionRequest.builder()
                .tenantId("tenant-a")
                .claimId("clm-transition")
                .patientId("pat-transition")
                .payerId("payer-a")
                .totalAmount(BigDecimal.valueOf(500))
                .idempotencyKey("idem-transition")
                .correlationId("corr-transition")
                .actor("test")
                .build());

        ReconciliationPreviewResponse first = revenueContractService.ingestRemittanceAdvice(RemittanceAdviceEvent.builder()
                .tenantId("tenant-a")
                .claimId("clm-transition")
                .remittanceId("rem-paid")
                .paymentAmount(BigDecimal.valueOf(500))
                .adjustmentAmount(BigDecimal.ZERO)
                .correlationId("corr-transition")
                .actor("test")
                .build());
        assertThat(first.getNewStatus()).isEqualTo(RevenueClaimState.PAID);

        ReconciliationPreviewResponse second = revenueContractService.ingestRemittanceAdvice(RemittanceAdviceEvent.builder()
                .tenantId("tenant-a")
                .claimId("clm-transition")
                .remittanceId("rem-regress")
                .paymentAmount(BigDecimal.valueOf(200))
                .adjustmentAmount(BigDecimal.ZERO)
                .correlationId("corr-transition")
                .actor("test")
                .build());

        assertThat(second.getErrorCode()).isEqualTo(RevenueErrorCode.VALIDATION_ERROR);
        assertThat(second.getNewStatus()).isEqualTo(RevenueClaimState.PAID);
    }

    @Test
    @DisplayName("publish + read current rates returns immutable version metadata")
    void shouldPublishAndReadCurrentPriceTransparencyRates() {
        var publish = revenueContractService.publishPriceTransparencyRates(PriceTransparencyRatePublishRequest.builder()
                .tenantId("tenant-a")
                .sourceReference("cms-rate-file")
                .correlationId("corr-price-publish")
                .actor("test")
                .rates(List.of(
                        PriceTransparencyRateEntry.builder().serviceCode("SVC-99213").negotiatedRate(BigDecimal.valueOf(75)).cashPrice(BigDecimal.valueOf(95)).build(),
                        PriceTransparencyRateEntry.builder().serviceCode("SVC-93000").negotiatedRate(BigDecimal.valueOf(40)).cashPrice(BigDecimal.valueOf(60)).build()
                ))
                .build());

        var current = revenueContractService.getCurrentPriceTransparencyRates("tenant-a", "corr-price-read", "test");
        assertThat(current).isNotNull();
        assertThat(current.getVersionId()).isEqualTo(publish.getVersionId());
        assertThat(current.getRates()).hasSize(2);
        assertThat(current.getChecksum()).isNotBlank();
    }

    @Test
    @DisplayName("estimatePrice returns deterministic amount for known service code")
    void shouldReturnDeterministicPriceEstimate() {
        var publish = revenueContractService.publishPriceTransparencyRates(PriceTransparencyRatePublishRequest.builder()
                .tenantId("tenant-a")
                .sourceReference("cms-rate-file")
                .correlationId("corr-price-publish")
                .actor("test")
                .rates(List.of(
                        PriceTransparencyRateEntry.builder().serviceCode("SVC-99213").negotiatedRate(BigDecimal.valueOf(75)).cashPrice(BigDecimal.valueOf(95)).build()
                ))
                .build());

        var estimate = revenueContractService.estimatePrice(PriceEstimateRequest.builder()
                .tenantId("tenant-a")
                .versionId(publish.getVersionId())
                .serviceCode("SVC-99213")
                .units(2)
                .correlationId("corr-price-est")
                .actor("test")
                .build());

        assertThat(estimate.getErrorCode()).isNull();
        assertThat(estimate.getEstimatedAllowedAmount()).isEqualByComparingTo(BigDecimal.valueOf(150.00));
        assertThat(estimate.getEstimatedPatientResponsibility()).isEqualByComparingTo(BigDecimal.valueOf(30.00));
    }

    @Test
    @DisplayName("estimatePrice returns VALIDATION_ERROR for unknown service code")
    void shouldReturnValidationErrorForUnknownServiceCode() {
        var publish = revenueContractService.publishPriceTransparencyRates(PriceTransparencyRatePublishRequest.builder()
                .tenantId("tenant-a")
                .sourceReference("cms-rate-file")
                .correlationId("corr-price-publish")
                .actor("test")
                .rates(List.of(
                        PriceTransparencyRateEntry.builder().serviceCode("SVC-99213").negotiatedRate(BigDecimal.valueOf(75)).cashPrice(BigDecimal.valueOf(95)).build()
                ))
                .build());

        var estimate = revenueContractService.estimatePrice(PriceEstimateRequest.builder()
                .tenantId("tenant-a")
                .versionId(publish.getVersionId())
                .serviceCode("SVC-UNKNOWN")
                .units(1)
                .correlationId("corr-price-est")
                .actor("test")
                .build());

        assertThat(estimate.getErrorCode()).isEqualTo(RevenueErrorCode.VALIDATION_ERROR);
    }
}
