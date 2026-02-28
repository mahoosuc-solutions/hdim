package com.healthdata.payer.service;

import com.healthdata.payer.revenue.dto.ClaimSubmissionRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class RestClearinghouseSubmissionAdapter implements ClearinghouseSubmissionAdapter {
    private final RestTemplate restTemplate;
    private final String baseUrl;

    public RestClearinghouseSubmissionAdapter(RestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public ClearinghouseSubmissionResult submit(ClaimSubmissionRequest request, int attempt)
            throws RetryableClearinghouseException, NonRetryableClearinghouseException {
        String url = baseUrl + "/api/v1/clearinghouse/claims/submissions";
        Map<String, Object> payload = new HashMap<>();
        payload.put("tenantId", request.getTenantId());
        payload.put("claimId", request.getClaimId());
        payload.put("patientId", request.getPatientId());
        payload.put("payerId", request.getPayerId());
        payload.put("totalAmount", request.getTotalAmount());
        payload.put("correlationId", request.getCorrelationId());
        payload.put("attempt", attempt);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(payload), Map.class);
            Map body = response.getBody();
            boolean accepted = body != null && Boolean.TRUE.equals(body.get("accepted"));
            String externalTrackingId = body == null ? null : (String) body.get("externalTrackingId");
            return new ClearinghouseSubmissionResult(accepted, externalTrackingId);
        } catch (ResourceAccessException ex) {
            throw new RetryableClearinghouseException("Clearinghouse timeout: " + ex.getMessage());
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().is5xxServerError()) {
                throw new RetryableClearinghouseException("Clearinghouse 5xx: " + ex.getStatusCode());
            }
            throw new NonRetryableClearinghouseException("Clearinghouse 4xx: " + ex.getStatusCode());
        }
    }
}
