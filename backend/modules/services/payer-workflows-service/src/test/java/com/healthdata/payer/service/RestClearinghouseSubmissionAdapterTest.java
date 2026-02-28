package com.healthdata.payer.service;

import com.healthdata.payer.revenue.dto.ClaimSubmissionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

@DisplayName("RestClearinghouseSubmissionAdapter Tests")
class RestClearinghouseSubmissionAdapterTest {
    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private RestClearinghouseSubmissionAdapter adapter;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        adapter = new RestClearinghouseSubmissionAdapter(restTemplate, "http://clearinghouse.test");
    }

    @Test
    @DisplayName("submit returns accepted result on HTTP 200")
    void shouldReturnAcceptedResult() {
        server.expect(once(), requestTo("http://clearinghouse.test/api/v1/clearinghouse/claims/submissions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"accepted\":true,\"externalTrackingId\":\"track-123\"}", MediaType.APPLICATION_JSON));

        var result = adapter.submit(sampleRequest(), 1);

        assertThat(result.accepted()).isTrue();
        assertThat(result.externalTrackingId()).isEqualTo("track-123");
        server.verify();
    }

    @Test
    @DisplayName("submit throws retryable on HTTP 5xx")
    void shouldThrowRetryableOnServerError() {
        server.expect(once(), requestTo("http://clearinghouse.test/api/v1/clearinghouse/claims/submissions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        assertThatThrownBy(() -> adapter.submit(sampleRequest(), 1))
                .isInstanceOf(RetryableClearinghouseException.class);
        server.verify();
    }

    @Test
    @DisplayName("submit throws non-retryable on HTTP 4xx")
    void shouldThrowNonRetryableOnClientError() {
        server.expect(once(), requestTo("http://clearinghouse.test/api/v1/clearinghouse/claims/submissions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withBadRequest());

        assertThatThrownBy(() -> adapter.submit(sampleRequest(), 1))
                .isInstanceOf(NonRetryableClearinghouseException.class);
        server.verify();
    }

    private ClaimSubmissionRequest sampleRequest() {
        return ClaimSubmissionRequest.builder()
                .tenantId("tenant-a")
                .claimId("clm-001")
                .patientId("pat-001")
                .payerId("payer-a")
                .totalAmount(BigDecimal.valueOf(100))
                .idempotencyKey("idem-001")
                .correlationId("corr-001")
                .actor("test")
                .build();
    }
}
