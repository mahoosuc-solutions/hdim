package com.healthdata.audit.context;

import com.healthdata.audit.models.AuditEvent;
import com.healthdata.authentication.context.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Unified provider for audit context across HTTP and event-driven flows.
 *
 * This component provides a single point for extracting user context for audit events,
 * regardless of whether the request came from:
 * - HTTP request (via gateway-trust headers)
 * - Kafka event consumer (via UserContextKafkaInterceptor)
 * - Scheduled job (via system context)
 *
 * Priority for user context extraction:
 * 1. UserContextHolder (set by TrustedHeaderAuthFilter or Kafka aspect)
 * 2. SecurityContext (Spring Security authentication)
 * 3. HTTP Request attributes (fallback for legacy code)
 * 4. System context (for scheduled jobs with no user)
 *
 * HIPAA Compliance:
 * - 45 CFR 164.312(b): Audit controls - consistent user identification
 * - 45 CFR 164.312(d): Entity authentication - verified user context
 *
 * Usage:
 * <pre>
 * @Autowired
 * private AuditContextProvider auditContextProvider;
 *
 * AuditEvent.Builder builder = AuditEvent.builder()
 *     .action("READ")
 *     .resourceType("Patient");
 *
 * auditContextProvider.enrichWithUserContext(builder);
 * auditService.logAuditEvent(builder.build());
 * </pre>
 */
@Component
public class AuditContextProvider {

    private static final Logger log = LoggerFactory.getLogger(AuditContextProvider.class);

    /**
     * Thread-local holder for UserContext.
     * This mirrors the holder in authentication-headers module for services that
     * don't have that dependency.
     */
    private static final ThreadLocal<UserContext> USER_CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * Set the user context for the current thread.
     * This is typically called by TrustedHeaderAuthFilter or Kafka consumer aspect.
     *
     * @param context the user context
     */
    public static void setContext(UserContext context) {
        USER_CONTEXT_HOLDER.set(context);
    }

    /**
     * Get the user context for the current thread.
     *
     * @return the user context, or null if not set
     */
    public static UserContext getContext() {
        return USER_CONTEXT_HOLDER.get();
    }

    /**
     * Clear the user context for the current thread.
     */
    public static void clearContext() {
        USER_CONTEXT_HOLDER.remove();
    }

    /**
     * Enrich an audit event builder with user context from the current request/thread.
     *
     * @param builder the audit event builder to enrich
     * @return the enriched builder (for chaining)
     */
    public AuditEvent.Builder enrichWithUserContext(AuditEvent.Builder builder) {
        // Try to get user context from the holder first
        UserContext userContext = USER_CONTEXT_HOLDER.get();

        if (userContext != null) {
            return enrichFromUserContext(builder, userContext);
        }

        // Fall back to Spring Security context
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return enrichFromAuthentication(builder, authentication);
        }

        // Fall back to HTTP request attributes
        return enrichFromHttpRequest(builder);
    }

    /**
     * Enrich builder from UserContext.
     */
    private AuditEvent.Builder enrichFromUserContext(AuditEvent.Builder builder, UserContext context) {
        log.trace("Enriching audit event from UserContext: user={}", context.username());

        builder.userId(context.userIdAsString());
        builder.username(context.username());

        if (context.ipAddress() != null) {
            builder.ipAddress(context.ipAddress());
        }

        if (context.userAgent() != null) {
            builder.userAgent(context.userAgent());
        }

        String tenantId = context.primaryTenantId();
        if (tenantId != null) {
            builder.tenantId(tenantId);
        }

        if (context.roles() != null && !context.roles().isEmpty()) {
            String role = context.roles().iterator().next();
            builder.role(role);
        }

        return builder;
    }

    /**
     * Enrich builder from Spring Security Authentication.
     */
    private AuditEvent.Builder enrichFromAuthentication(AuditEvent.Builder builder, Authentication authentication) {
        log.trace("Enriching audit event from Authentication: user={}", authentication.getName());

        builder.username(authentication.getName());

        if (authentication.getPrincipal() instanceof String) {
            builder.userId((String) authentication.getPrincipal());
        }

        if (!authentication.getAuthorities().isEmpty()) {
            String role = authentication.getAuthorities().iterator().next().getAuthority();
            builder.role(role);
        }

        // Also try to get HTTP context
        enrichFromHttpRequest(builder);

        return builder;
    }

    /**
     * Enrich builder from HTTP request context.
     */
    private AuditEvent.Builder enrichFromHttpRequest(AuditEvent.Builder builder) {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();

                // IP address
                String ipAddress = request.getRemoteAddr();
                String forwardedFor = request.getHeader("X-Forwarded-For");
                if (forwardedFor != null && !forwardedFor.isBlank()) {
                    ipAddress = forwardedFor.split(",")[0].trim();
                }
                // Also check gateway-injected header
                String authIp = request.getHeader("X-Auth-Client-IP");
                if (authIp != null && !authIp.isBlank()) {
                    ipAddress = authIp;
                }
                builder.ipAddress(ipAddress);

                // User agent
                String userAgent = request.getHeader("User-Agent");
                builder.userAgent(userAgent);

                // Request path
                String requestPath = request.getRequestURI();
                builder.requestPath(requestPath);

                // Tenant ID from header
                String tenantId = request.getHeader("X-Tenant-ID");
                if (tenantId != null) {
                    builder.tenantId(tenantId);
                }

                // Try to get user info from gateway headers if not already set
                String userId = request.getHeader("X-Auth-User-Id");
                String username = request.getHeader("X-Auth-Username");
                if (userId != null) {
                    builder.userId(userId);
                }
                if (username != null) {
                    builder.username(username);
                }

                log.trace("Enriched audit event from HTTP request: path={}", requestPath);
            }
        } catch (Exception e) {
            log.debug("Could not extract HTTP context for audit", e);
        }

        return builder;
    }

    /**
     * Create a system context for scheduled jobs or system-initiated operations.
     *
     * @param jobName the name of the scheduled job
     * @param tenantId the tenant ID (if applicable)
     * @return a UserContext representing system
     */
    public static UserContext systemContext(String jobName, String tenantId) {
        return UserContext.builder()
            .userId("system")
            .username("scheduled:" + jobName)
            .tenantIds(tenantId != null ? java.util.Set.of(tenantId) : java.util.Set.of())
            .roles(java.util.Set.of("SYSTEM"))
            .build();
    }

    /**
     * Get current username for simple logging purposes.
     *
     * @return current username or "anonymous"
     */
    public String getCurrentUsername() {
        UserContext context = USER_CONTEXT_HOLDER.get();
        if (context != null && context.username() != null) {
            return context.username();
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }

        return "anonymous";
    }

    /**
     * Get current tenant ID.
     *
     * @return current tenant ID or null
     */
    public String getCurrentTenantId() {
        UserContext context = USER_CONTEXT_HOLDER.get();
        if (context != null) {
            return context.primaryTenantId();
        }

        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest().getHeader("X-Tenant-ID");
            }
        } catch (Exception e) {
            // Ignore
        }

        return null;
    }
}
