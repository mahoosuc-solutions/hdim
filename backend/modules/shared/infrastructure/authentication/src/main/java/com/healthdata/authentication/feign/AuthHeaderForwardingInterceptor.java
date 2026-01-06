package com.healthdata.authentication.feign;

import com.healthdata.authentication.constants.AuthHeaderConstants;
import com.healthdata.authentication.context.AuthHeaderContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Feign RequestInterceptor that forwards X-Auth-* headers from the current
 * request context to downstream service calls.
 *
 * This interceptor ensures user identity, tenant context, and roles are propagated
 * through the entire request chain when using Feign clients for service-to-service
 * communication.
 *
 * <h2>Headers Forwarded</h2>
 * <ul>
 *   <li><b>X-Auth-User-Id</b> - User's unique identifier (UUID)</li>
 *   <li><b>X-Auth-Username</b> - User's login name</li>
 *   <li><b>X-Auth-Tenant-Ids</b> - Comma-separated authorized tenant IDs</li>
 *   <li><b>X-Auth-Roles</b> - Comma-separated user roles</li>
 *   <li><b>X-Auth-Validated</b> - Gateway validation signature</li>
 *   <li><b>X-Auth-Token-Id</b> - Original JWT token ID (jti claim)</li>
 *   <li><b>X-Auth-Token-Expires</b> - Token expiration timestamp</li>
 *   <li><b>X-Tenant-ID</b> - Current tenant context for the request</li>
 * </ul>
 *
 * <h2>Implementation</h2>
 * This interceptor reads headers from AuthHeaderContext (InheritableThreadLocal)
 * instead of RequestContextHolder. This ensures headers are available even when
 * Feign executes requests in different threads (e.g., thread pools).
 *
 * <h2>Usage</h2>
 * This interceptor is auto-registered as a Spring component when Feign is on the classpath.
 * Services using the authentication module and Feign clients will automatically have
 * auth headers forwarded to downstream services.
 *
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li>Headers are only forwarded if present in the original request</li>
 *   <li>The X-Auth-Validated signature is forwarded to maintain trust chain</li>
 *   <li>Downstream services must validate the signature to ensure headers originated from gateway</li>
 * </ul>
 *
 * <h2>Example</h2>
 * <pre>
 * // No additional configuration needed - interceptor is auto-registered
 * &#64;FeignClient(name = "patient-service", url = "${patient.service.url}")
 * public interface PatientServiceClient {
 *     &#64;GetMapping("/api/v1/patients/{id}")
 *     PatientResponse getPatient(@PathVariable String id);
 * }
 * </pre>
 *
 * @see AuthHeaderConstants
 * @see AuthHeaderContext
 * @see com.healthdata.authentication.context.AuthHeaderContextFilter
 * @see com.healthdata.authentication.filter.TrustedHeaderAuthFilter
 */
@Slf4j
@Component
@ConditionalOnClass(name = "feign.RequestInterceptor")
public class AuthHeaderForwardingInterceptor implements RequestInterceptor {

    private static final String TENANT_ID_HEADER = "X-Tenant-ID";

    @Override
    public void apply(RequestTemplate template) {
        // Read headers from AuthHeaderContext (InheritableThreadLocal)
        // This works across threads, unlike RequestContextHolder
        Map<String, String> headers = AuthHeaderContext.getAllHeaders();

        if (headers.isEmpty()) {
            log.trace("No auth headers in context, skipping header forwarding");
            return;
        }

        int forwardedCount = 0;

        // Forward all X-Auth-* headers from context
        for (String headerName : AuthHeaderConstants.getAllAuthHeaders()) {
            String headerValue = headers.get(headerName);
            if (headerValue != null && !headerValue.isBlank()) {
                template.header(headerName, headerValue);
                log.trace("Forwarding header: {} = {}", headerName, maskSensitiveValue(headerName, headerValue));
                forwardedCount++;
            }
        }

        // Forward X-Tenant-ID for tenant context
        String tenantId = headers.get(TENANT_ID_HEADER);
        if (tenantId != null && !tenantId.isBlank()) {
            template.header(TENANT_ID_HEADER, tenantId);
            log.trace("Forwarding header: {} = {}", TENANT_ID_HEADER, tenantId);
            forwardedCount++;
        }

        if (forwardedCount > 0) {
            log.debug("Forwarded {} auth headers to downstream service: {}",
                forwardedCount, template.feignTarget().name());
        }
    }

    /**
     * Mask sensitive header values for logging.
     */
    private String maskSensitiveValue(String headerName, String value) {
        if (headerName.equals(AuthHeaderConstants.HEADER_VALIDATED) ||
            headerName.equals(AuthHeaderConstants.HEADER_USER_ID) ||
            headerName.equals(AuthHeaderConstants.HEADER_TOKEN_ID)) {
            // Mask sensitive values
            if (value.length() > 8) {
                return value.substring(0, 8) + "***";
            }
            return "***";
        }
        return value;
    }
}
