package com.healthdata.payer.controller;

import com.healthdata.payer.revenue.RevenueClaimState;
import com.healthdata.payer.revenue.dto.*;
import com.healthdata.payer.service.RevenueContractService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("RevenueContractController Tests")
class RevenueContractControllerTest {

    private MockMvc mockMvc;
    @Mock
    private RevenueContractService revenueContractService;

    @InjectMocks
    private RevenueContractController revenueContractController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(revenueContractController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/revenue/claims/submissions returns submitted state")
    void shouldSubmitClaim() throws Exception {
        when(revenueContractService.submitClaim(any())).thenReturn(ClaimSubmissionResponse.builder()
                .tenantId("tenant-a")
                .claimId("clm-001")
                .correlationId("corr-001")
                .status(RevenueClaimState.SUBMITTED)
                .duplicate(false)
                .auditEnvelope(audit("tenant-a", "corr-001", "CLAIM_SUBMISSION", "SUBMITTED"))
                .build());

        String request = """
            {
              "tenantId":"tenant-a",
              "claimId":"clm-001",
              "patientId":"pat-001",
              "payerId":"payer-a",
              "totalAmount":1250.50,
              "idempotencyKey":"idem-001",
              "correlationId":"corr-001",
              "actor":"system-test"
            }
            """;

        mockMvc.perform(post("/api/v1/revenue/claims/submissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.duplicate").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/revenue/eligibility/checks returns eligibility result")
    void shouldCheckEligibility() throws Exception {
        when(revenueContractService.checkEligibility(any())).thenReturn(EligibilityCheckResponse.builder()
                .tenantId("tenant-a")
                .payerId("payer-a")
                .patientId("pat-001")
                .correlationId("corr-elig")
                .eligible(true)
                .auditEnvelope(audit("tenant-a", "corr-elig", "ELIGIBILITY_CHECK", "ELIGIBLE"))
                .build());

        String request = """
            {
              "tenantId":"tenant-a",
              "payerId":"payer-a",
              "patientId":"pat-001",
              "correlationId":"corr-elig",
              "actor":"system-test"
            }
            """;

        mockMvc.perform(post("/api/v1/revenue/eligibility/checks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/revenue/claim-status/checks returns known claim state")
    void shouldReturnClaimStatus() throws Exception {
        when(revenueContractService.checkClaimStatus(any())).thenReturn(ClaimStatusResponse.builder()
                .tenantId("tenant-a")
                .claimId("clm-status")
                .correlationId("corr-status")
                .status(RevenueClaimState.SUBMITTED)
                .auditEnvelope(audit("tenant-a", "corr-status", "CLAIM_STATUS_CHECK", "STATUS_RETURNED"))
                .build());

        String request = """
            {
              "tenantId":"tenant-a",
              "claimId":"clm-status",
              "correlationId":"corr-status",
              "actor":"system-test"
            }
            """;

        mockMvc.perform(post("/api/v1/revenue/claim-status/checks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("POST /api/v1/revenue/remittance/advice returns reconciliation preview")
    void shouldReturnReconciliationPreview() throws Exception {
        when(revenueContractService.ingestRemittanceAdvice(any())).thenReturn(ReconciliationPreviewResponse.builder()
                .tenantId("tenant-a")
                .claimId("clm-remit")
                .remittanceId("rem-001")
                .correlationId("corr-remit")
                .priorStatus(RevenueClaimState.SUBMITTED)
                .newStatus(RevenueClaimState.PARTIALLY_PAID)
                .remainingBalance(java.math.BigDecimal.valueOf(150))
                .auditEnvelope(audit("tenant-a", "corr-remit", "REMITTANCE_INGEST", "RECONCILIATION_PREVIEW_READY"))
                .build());

        String request = """
            {
              "tenantId":"tenant-a",
              "claimId":"clm-remit",
              "remittanceId":"rem-001",
              "paymentAmount":350.00,
              "adjustmentAmount":0.00,
              "correlationId":"corr-remit",
              "actor":"system-test"
            }
            """;

        mockMvc.perform(post("/api/v1/revenue/remittance/advice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newStatus").value("PARTIALLY_PAID"))
                .andExpect(jsonPath("$.remainingBalance").value(150));
    }

    @Test
    @DisplayName("GET /api/v1/revenue/audit/{correlationId} returns audit envelopes")
    void shouldReturnAuditTrail() throws Exception {
        when(revenueContractService.getAuditTrail("corr-audit")).thenReturn(List.of(
                audit("tenant-a", "corr-audit", "CLAIM_SUBMISSION", "SUBMITTED")
        ));

        mockMvc.perform(get("/api/v1/revenue/audit/{correlationId}", "corr-audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].action").value("CLAIM_SUBMISSION"));
    }

    private RevenueAuditEnvelope audit(String tenantId, String correlationId, String action, String outcome) {
        return RevenueAuditEnvelope.builder()
                .tenantId(tenantId)
                .correlationId(correlationId)
                .actor("system-test")
                .timestamp(Instant.now())
                .action(action)
                .outcome(outcome)
                .build();
    }
}
