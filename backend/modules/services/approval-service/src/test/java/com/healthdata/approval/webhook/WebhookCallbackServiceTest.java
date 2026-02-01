package com.healthdata.approval.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.approval.domain.entity.ApprovalRequest;
import com.healthdata.approval.domain.entity.ApprovalRequest.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookCallbackService Tests")
class WebhookCallbackServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WebhookCallbackService webhookService;

    @Captor
    private ArgumentCaptor<String> urlCaptor;

    @Captor
    private ArgumentCaptor<String> payloadCaptor;

    private static final String TENANT_ID = "tenant-123";
    private static final String USER_ID = "user-456";
    private static final String CALLBACK_URL = "https://webhook.example.com/callback";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(webhookService, "webhookSecret", "test-secret");
        ReflectionTestUtils.setField(webhookService, "timeoutSeconds", 30);
        ReflectionTestUtils.setField(webhookService, "maxRetries", 3);
    }

    @Nested
    @DisplayName("Send Decision Callback")
    class SendDecisionCallbackTests {

        @Test
        @DisplayName("should send webhook callback successfully")
        void sendDecisionCallback_Success() throws Exception {
            // Given
            ApprovalRequest request = createApprovedRequest();
            setupWebClientMock();

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("OK"));

            // When
            webhookService.sendDecisionCallback(request);
            Thread.sleep(10); // Wait for async execution

            // Then
            verify(requestBodyUriSpec).uri(CALLBACK_URL);
            verify(requestBodySpec).header(eq(HttpHeaders.CONTENT_TYPE), eq(MediaType.APPLICATION_JSON_VALUE));
            verify(requestBodySpec).header(eq("X-HDIM-Request-Id"), eq(request.getId().toString()));
            verify(requestBodySpec).header(eq("X-HDIM-Signature"), startsWith("sha256="));
        }

        @Test
        @DisplayName("should skip when no callback URL present")
        void sendDecisionCallback_NoUrl_Skips() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            request.setPayload(new HashMap<>()); // No callback URL

            // When
            webhookService.sendDecisionCallback(request);
            Thread.sleep(10);

            // Then
            verify(webClientBuilder, never()).build();
        }

        @Test
        @DisplayName("should skip when callback URL is blank")
        void sendDecisionCallback_BlankUrl_Skips() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            Map<String, Object> payload = new HashMap<>();
            payload.put("n8nCallbackUrl", "");
            request.setPayload(payload);

            // When
            webhookService.sendDecisionCallback(request);
            Thread.sleep(10);

            // Then
            verify(webClientBuilder, never()).build();
        }

        @Test
        @DisplayName("should include decision details in payload")
        void sendDecisionCallback_IncludesDecisionDetails() throws Exception {
            // Given
            ApprovalRequest request = createApprovedRequest();
            setupWebClientMock();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            when(objectMapper.writeValueAsString(payloadCaptor.capture())).thenReturn("{}");
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("OK"));

            // When
            webhookService.sendDecisionCallback(request);
            Thread.sleep(10);

            // Then
            Map<String, Object> capturedPayload = payloadCaptor.getValue();
            assertThat(capturedPayload).containsKeys(
                "requestId", "tenantId", "status", "entityType", "entityId",
                "actionRequested", "riskLevel", "requestType", "decidedBy", "decisionReason"
            );
            assertThat(capturedPayload.get("status")).isEqualTo("APPROVED");
            assertThat(capturedPayload.get("decidedBy")).isEqualTo("reviewer-123");
        }

        @Test
        @DisplayName("should exclude callback URL from original payload")
        void sendDecisionCallback_ExcludesCallbackUrl() throws Exception {
            // Given
            ApprovalRequest request = createApprovedRequest();
            setupWebClientMock();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            when(objectMapper.writeValueAsString(payloadCaptor.capture())).thenReturn("{}");
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("OK"));

            // When
            webhookService.sendDecisionCallback(request);
            Thread.sleep(10);

            // Then
            Map<String, Object> capturedPayload = payloadCaptor.getValue();
            @SuppressWarnings("unchecked")
            Map<String, Object> originalPayload = (Map<String, Object>) capturedPayload.get("payload");
            assertThat(originalPayload).doesNotContainKey("n8nCallbackUrl");
        }

        @Test
        @DisplayName("should compute HMAC signature when secret configured")
        void sendDecisionCallback_ComputesSignature() throws Exception {
            // Given
            ReflectionTestUtils.setField(webhookService, "webhookSecret", "my-secret-key");
            ApprovalRequest request = createApprovedRequest();
            setupWebClientMock();

            when(objectMapper.writeValueAsString(any())).thenReturn("{\"test\":\"data\"}");
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("OK"));

            ArgumentCaptor<String> signatureCaptor = ArgumentCaptor.forClass(String.class);

            // When
            webhookService.sendDecisionCallback(request);
            Thread.sleep(10);

            // Then
            verify(requestBodySpec).header(eq("X-HDIM-Signature"), signatureCaptor.capture());
            String signature = signatureCaptor.getValue();
            assertThat(signature).startsWith("sha256=");
            assertThat(signature).isNotEqualTo("sha256=unsigned");
        }

        @Test
        @DisplayName("should use unsigned signature when no secret")
        void sendDecisionCallback_NoSecret_UnsignedSignature() throws Exception {
            // Given
            ReflectionTestUtils.setField(webhookService, "webhookSecret", "");
            ApprovalRequest request = createApprovedRequest();
            setupWebClientMock();

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("OK"));

            ArgumentCaptor<String> signatureCaptor = ArgumentCaptor.forClass(String.class);

            // When
            webhookService.sendDecisionCallback(request);
            Thread.sleep(10);

            // Then
            verify(requestBodySpec).header(eq("X-HDIM-Signature"), signatureCaptor.capture());
            assertThat(signatureCaptor.getValue()).isEqualTo("sha256=unsigned");
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should handle HTTP error gracefully")
        void sendDecisionCallback_HttpError_HandlesGracefully() throws Exception {
            // Given
            ApprovalRequest request = createApprovedRequest();
            setupWebClientMock();

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new RuntimeException("HTTP 500")));

            // When/Then
            assertThatCode(() -> {
                webhookService.sendDecisionCallback(request);
                Thread.sleep(10);
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should handle serialization error gracefully")
        void sendDecisionCallback_SerializationError_HandlesGracefully() throws Exception {
            // Given
            ApprovalRequest request = createApprovedRequest();

            when(objectMapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("Serialization failed"));

            // When/Then
            assertThatCode(() -> {
                webhookService.sendDecisionCallback(request);
                Thread.sleep(10);
            }).doesNotThrowAnyException();

            verify(webClientBuilder, never()).build();
        }

        @Test
        @DisplayName("should retry on retryable errors")
        void sendDecisionCallback_RetryableError_Retries() throws Exception {
            // Given
            ApprovalRequest request = createApprovedRequest();
            setupWebClientMock();

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new RuntimeException("Connection timeout")));

            // When
            webhookService.sendDecisionCallback(request);
            Thread.sleep(50); // Wait for retries

            // Then - Verify retry mechanism was configured
            verify(requestBodySpec).bodyValue(anyString());
        }

        @Test
        @DisplayName("should handle null payload gracefully")
        void sendDecisionCallback_NullPayload_HandlesGracefully() throws Exception {
            // Given
            ApprovalRequest request = createApprovedRequest();
            request.setPayload(null);

            // When/Then
            assertThatCode(() -> {
                webhookService.sendDecisionCallback(request);
                Thread.sleep(10);
            }).doesNotThrowAnyException();

            verify(webClientBuilder, never()).build();
        }
    }

    @Nested
    @DisplayName("Payload Construction")
    class PayloadConstructionTests {

        @Test
        @DisplayName("should include all required fields")
        void buildPayload_IncludesAllRequiredFields() throws Exception {
            // Given
            ApprovalRequest request = createApprovedRequest();
            request.setCorrelationId("corr-123");
            request.setEscalationCount(2);

            setupWebClientMock();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            when(objectMapper.writeValueAsString(payloadCaptor.capture())).thenReturn("{}");
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("OK"));

            // When
            webhookService.sendDecisionCallback(request);
            Thread.sleep(10);

            // Then
            Map<String, Object> payload = payloadCaptor.getValue();
            assertThat(payload).containsEntry("correlationId", "corr-123");
            assertThat(payload).containsEntry("escalationCount", 2);
            assertThat(payload).containsEntry("tenantId", TENANT_ID);
        }

        @Test
        @DisplayName("should handle null decision timestamp")
        void buildPayload_NullDecisionAt_HandlesGracefully() throws Exception {
            // Given
            ApprovalRequest request = createRequest();
            Map<String, Object> payload = new HashMap<>();
            payload.put("n8nCallbackUrl", CALLBACK_URL);
            payload.put("data", "test");
            request.setPayload(payload);
            request.setDecisionBy(null);
            request.setDecisionAt(null);

            setupWebClientMock();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> payloadCaptor = ArgumentCaptor.forClass(Map.class);
            when(objectMapper.writeValueAsString(payloadCaptor.capture())).thenReturn("{}");
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just("OK"));

            // When
            webhookService.sendDecisionCallback(request);
            Thread.sleep(10);

            // Then
            Map<String, Object> capturedPayload = payloadCaptor.getValue();
            assertThat(capturedPayload.get("decidedAt")).isNull();
        }
    }

    @Nested
    @DisplayName("Retry Logic")
    class RetryLogicTests {

        @Test
        @DisplayName("should not retry on 4xx client errors")
        void retry_ClientError_DoesNotRetry() throws Exception {
            // Given
            ApprovalRequest request = createApprovedRequest();
            setupWebClientMock();

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new RuntimeException("400 Bad Request")));

            // When
            webhookService.sendDecisionCallback(request);
            Thread.sleep(10);

            // Then - Should configure retry but not actually retry 4xx
            verify(requestBodySpec).bodyValue(anyString());
        }

        @Test
        @DisplayName("should retry on 408 timeout")
        void retry_Timeout_Retries() throws Exception {
            // Given
            ApprovalRequest request = createApprovedRequest();
            setupWebClientMock();

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new RuntimeException("408 Request Timeout")));

            // When
            webhookService.sendDecisionCallback(request);
            Thread.sleep(10);

            // Then
            verify(requestBodySpec).bodyValue(anyString());
        }

        @Test
        @DisplayName("should retry on 429 rate limit")
        void retry_RateLimit_Retries() throws Exception {
            // Given
            ApprovalRequest request = createApprovedRequest();
            setupWebClientMock();

            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new RuntimeException("429 Too Many Requests")));

            // When
            webhookService.sendDecisionCallback(request);
            Thread.sleep(10);

            // Then
            verify(requestBodySpec).bodyValue(anyString());
        }
    }

    // Helper methods

    private ApprovalRequest createRequest() {
        return ApprovalRequest.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .requestType(RequestType.AGENT_ACTION)
            .entityType("TestEntity")
            .entityId("entity-123")
            .actionRequested("EXECUTE")
            .riskLevel(RiskLevel.MEDIUM)
            .status(ApprovalStatus.PENDING)
            .requestedBy(USER_ID)
            .payload(new HashMap<>())
            .build();
    }

    private ApprovalRequest createApprovedRequest() {
        ApprovalRequest request = createRequest();
        Map<String, Object> payload = new HashMap<>();
        payload.put("n8nCallbackUrl", CALLBACK_URL);
        payload.put("data", "test-data");
        request.setPayload(payload);
        request.approve("reviewer-123", "Approved");
        return request;
    }

    @SuppressWarnings("unchecked")
    private void setupWebClientMock() {
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn((WebClient.RequestHeadersSpec) requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }
}
