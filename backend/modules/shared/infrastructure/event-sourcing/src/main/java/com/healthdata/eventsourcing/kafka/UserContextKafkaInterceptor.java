package com.healthdata.eventsourcing.kafka;

import com.healthdata.authentication.constants.AuthHeaderConstants;
import com.healthdata.authentication.context.UserContext;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Kafka producer interceptor that propagates user context via headers.
 *
 * This interceptor automatically adds user context headers to all Kafka messages,
 * enabling HIPAA-compliant audit trails for event-driven processing.
 *
 * Headers added:
 * - X-Auth-User-Id: User's UUID
 * - X-Auth-Username: User's login name
 * - X-Auth-Tenant-Id: Active tenant ID
 * - X-Auth-Roles: Comma-separated list of roles
 * - X-Correlation-Id: Unique ID for distributed tracing
 * - X-Auth-IP-Address: Client IP (for audit)
 * - X-Auth-Initiated-At: Timestamp when action was initiated
 *
 * HIPAA Compliance:
 * - 45 CFR 164.312(b): Audit controls - user identification in events
 * - 45 CFR 164.312(d): Entity authentication - verified user context
 *
 * Configuration:
 * Add to Kafka producer config:
 * <pre>
 * spring.kafka.producer.properties.interceptor.classes=\
 *   com.healthdata.eventsourcing.kafka.UserContextKafkaInterceptor
 * </pre>
 *
 * Note: This interceptor uses a static UserContextHolder reference because
 * Kafka interceptors are instantiated by Kafka, not Spring.
 */
public class UserContextKafkaInterceptor implements ProducerInterceptor<String, Object> {

    private static final Logger log = LoggerFactory.getLogger(UserContextKafkaInterceptor.class);

    // Kafka header names (prefixed for clarity in Kafka consumers)
    public static final String HEADER_USER_ID = "X-Auth-User-Id";
    public static final String HEADER_USERNAME = "X-Auth-Username";
    public static final String HEADER_TENANT_ID = "X-Auth-Tenant-Id";
    public static final String HEADER_ROLES = "X-Auth-Roles";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";
    public static final String HEADER_IP_ADDRESS = "X-Auth-IP-Address";
    public static final String HEADER_INITIATED_AT = "X-Auth-Initiated-At";

    /**
     * Static holder for UserContext. This allows the interceptor to access
     * the current user context without Spring dependency injection.
     */
    private static final ThreadLocal<UserContext> USER_CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * Set the user context for the current thread.
     * Call this before sending Kafka messages.
     *
     * @param context the user context to propagate
     */
    public static void setUserContext(UserContext context) {
        USER_CONTEXT_HOLDER.set(context);
    }

    /**
     * Get the user context for the current thread.
     *
     * @return the current user context, or null if not set
     */
    public static UserContext getUserContext() {
        return USER_CONTEXT_HOLDER.get();
    }

    /**
     * Clear the user context for the current thread.
     */
    public static void clearUserContext() {
        USER_CONTEXT_HOLDER.remove();
    }

    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
        UserContext context = USER_CONTEXT_HOLDER.get();

        if (context != null) {
            Headers headers = record.headers();

            addHeader(headers, HEADER_USER_ID, context.userIdAsString());
            addHeader(headers, HEADER_USERNAME, context.username());

            String tenantId = context.primaryTenantId();
            if (tenantId != null) {
                addHeader(headers, HEADER_TENANT_ID, tenantId);
            }

            if (context.roles() != null && !context.roles().isEmpty()) {
                addHeader(headers, HEADER_ROLES, String.join(",", context.roles()));
            }

            if (context.ipAddress() != null) {
                addHeader(headers, HEADER_IP_ADDRESS, context.ipAddress());
            }

            // Add correlation ID for distributed tracing
            String correlationId = UUID.randomUUID().toString();
            addHeader(headers, HEADER_CORRELATION_ID, correlationId);

            // Add timestamp
            addHeader(headers, HEADER_INITIATED_AT, java.time.Instant.now().toString());

            log.trace("Added user context headers to Kafka message: topic={}, user={}, tenant={}",
                record.topic(), context.username(), tenantId);
        } else {
            log.trace("No user context available for Kafka message: topic={}", record.topic());
        }

        return record;
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        if (exception != null) {
            log.warn("Kafka message send failed: topic={}, partition={}, error={}",
                metadata != null ? metadata.topic() : "unknown",
                metadata != null ? metadata.partition() : -1,
                exception.getMessage());
        }
    }

    @Override
    public void close() {
        // Nothing to clean up
    }

    @Override
    public void configure(Map<String, ?> configs) {
        log.info("UserContextKafkaInterceptor configured");
    }

    /**
     * Add a header to the Kafka message if value is not null or blank.
     */
    private void addHeader(Headers headers, String key, String value) {
        if (value != null && !value.isBlank()) {
            headers.add(key, value.getBytes(StandardCharsets.UTF_8));
        }
    }
}
