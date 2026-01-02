package com.healthdata.approval.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.approval.domain.entity.ApprovalRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;

/**
 * Service for sending webhook callbacks when approval decisions are made.
 * Used by n8n and other external integrations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookCallbackService {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${hdim.approval.webhook.secret:}")
    private String webhookSecret;

    @Value("${hdim.approval.webhook.timeout-seconds:30}")
    private int timeoutSeconds;

    @Value("${hdim.approval.webhook.max-retries:3}")
    private int maxRetries;

    private static final String PAYLOAD_KEY = "n8nCallbackUrl";

    /**
     * Send webhook callback when an approval decision is made.
     * This is called asynchronously to not block the main approval flow.
     */
    @Async
    public void sendDecisionCallback(ApprovalRequest request) {
        String callbackUrl = extractCallbackUrl(request);
        if (callbackUrl == null || callbackUrl.isBlank()) {
            log.debug("No callback URL for request {}, skipping webhook", request.getId());
            return;
        }

        log.info("Sending webhook callback for request {} to {}", request.getId(), callbackUrl);

        try {
            Map<String, Object> payload = buildPayload(request);
            String payloadJson = objectMapper.writeValueAsString(payload);
            String signature = computeSignature(payloadJson);

            webClientBuilder.build()
                .post()
                .uri(callbackUrl)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("X-HDIM-Signature", signature)
                .header("X-HDIM-Request-Id", request.getId().toString())
                .bodyValue(payloadJson)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .retryWhen(Retry.backoff(maxRetries, Duration.ofSeconds(1))
                    .filter(this::isRetryable)
                    .doBeforeRetry(signal -> log.warn(
                        "Retrying webhook callback for request {} (attempt {})",
                        request.getId(), signal.totalRetries() + 1)))
                .doOnSuccess(response -> log.info(
                    "Webhook callback successful for request {} to {}",
                    request.getId(), callbackUrl))
                .doOnError(error -> log.error(
                    "Webhook callback failed for request {} to {}: {}",
                    request.getId(), callbackUrl, error.getMessage()))
                .onErrorResume(error -> {
                    // Log but don't fail - webhook failures shouldn't affect the approval
                    return Mono.empty();
                })
                .subscribe();
        } catch (Exception e) {
            log.error("Failed to send webhook callback for request {}: {}",
                request.getId(), e.getMessage(), e);
        }
    }

    private String extractCallbackUrl(ApprovalRequest request) {
        if (request.getPayload() == null) {
            return null;
        }
        Object url = request.getPayload().get(PAYLOAD_KEY);
        return url != null ? url.toString() : null;
    }

    private Map<String, Object> buildPayload(ApprovalRequest request) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("requestId", request.getId().toString());
        payload.put("tenantId", request.getTenantId());
        payload.put("status", request.getStatus().name());
        payload.put("entityType", request.getEntityType());
        payload.put("entityId", request.getEntityId());
        payload.put("actionRequested", request.getActionRequested());
        payload.put("riskLevel", request.getRiskLevel().name());
        payload.put("requestType", request.getRequestType().name());

        // Include the original payload (minus the callback URL)
        Map<String, Object> originalPayload = new HashMap<>(request.getPayload());
        originalPayload.remove(PAYLOAD_KEY);
        payload.put("payload", originalPayload);

        // Decision details
        payload.put("decidedBy", request.getDecisionBy());
        payload.put("decidedAt", request.getDecisionAt() != null ?
            request.getDecisionAt().toString() : null);
        payload.put("decisionReason", request.getDecisionReason());

        // Tracking
        payload.put("correlationId", request.getCorrelationId());
        payload.put("escalationCount", request.getEscalationCount());

        return payload;
    }

    private String computeSignature(String payload) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return "sha256=unsigned";
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                webhookSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to compute webhook signature: {}", e.getMessage());
            return "sha256=error";
        }
    }

    private boolean isRetryable(Throwable throwable) {
        // Retry on network errors and 5xx responses
        String message = throwable.getMessage();
        if (message == null) return true;
        return !message.contains("4") || message.contains("408") || message.contains("429");
    }
}
