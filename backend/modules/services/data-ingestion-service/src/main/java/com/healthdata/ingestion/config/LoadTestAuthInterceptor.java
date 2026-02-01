package com.healthdata.ingestion.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * RestTemplate interceptor that adds mock authentication headers for load testing.
 *
 * SECURITY WARNING: This is ONLY for load testing and should NEVER be used in production.
 * It bypasses gateway authentication by injecting mock X-Auth-* headers directly.
 *
 * The ingestion service creates a "load-test-system" user identity that services
 * will accept for data ingestion operations.
 */
@Component
@Slf4j
public class LoadTestAuthInterceptor implements ClientHttpRequestInterceptor {

    private static final String LOAD_TEST_USER_ID = "00000000-0000-0000-0000-000000000001";
    private static final String LOAD_TEST_USERNAME = "load-test-system";
    private static final String LOAD_TEST_ROLE = "SUPER_ADMIN";

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        // Add gateway-trust authentication headers
        // These headers normally come from the gateway after JWT validation
        request.getHeaders().set("X-Auth-Validated", "gateway-" + Instant.now().toEpochMilli() + "-mock");
        request.getHeaders().set("X-Auth-User-Id", LOAD_TEST_USER_ID);
        request.getHeaders().set("X-Auth-Username", LOAD_TEST_USERNAME);
        request.getHeaders().set("X-Auth-Roles", LOAD_TEST_ROLE);

        // If X-Tenant-ID already set, also add to X-Auth-Tenant-Ids
        String tenantId = request.getHeaders().getFirst("X-Tenant-ID");
        if (tenantId != null && !tenantId.isEmpty()) {
            request.getHeaders().set("X-Auth-Tenant-Ids", tenantId);
        }

        log.debug("Added load-test auth headers to request: {} {}",
                  request.getMethod(), request.getURI());

        return execution.execute(request, body);
    }
}
