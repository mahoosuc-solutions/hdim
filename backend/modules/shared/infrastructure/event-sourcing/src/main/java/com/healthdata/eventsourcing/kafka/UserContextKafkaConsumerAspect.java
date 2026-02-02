package com.healthdata.eventsourcing.kafka;

import com.healthdata.authentication.context.UserContext;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Aspect that extracts user context from Kafka message headers before @KafkaListener executes.
 *
 * This aspect ensures that audit logging and authorization work correctly for
 * event-driven processing by populating the UserContextKafkaInterceptor's
 * thread-local holder with user context from Kafka headers.
 *
 * How it works:
 * 1. Intercepts @KafkaListener method invocations
 * 2. Looks for ConsumerRecord parameter in method arguments
 * 3. Extracts X-Auth-* headers from the ConsumerRecord
 * 4. Builds UserContext and sets it in the thread-local holder
 * 5. Executes the listener method
 * 6. Clears the context after execution
 *
 * HIPAA Compliance:
 * - Enables audit trails for event-driven PHI access
 * - Propagates user identification through async processing
 *
 * Expected Headers (set by UserContextKafkaInterceptor on producer):
 * - X-Auth-User-Id: User's UUID
 * - X-Auth-Username: User's login name
 * - X-Auth-Tenant-Id: Active tenant ID
 * - X-Auth-Roles: Comma-separated list of roles
 * - X-Auth-IP-Address: Client IP
 * - X-Correlation-Id: Distributed tracing ID
 */
@Aspect
@Component
public class UserContextKafkaConsumerAspect {

    private static final Logger log = LoggerFactory.getLogger(UserContextKafkaConsumerAspect.class);

    /**
     * Around advice for @KafkaListener methods.
     * Extracts user context from Kafka headers before the listener executes.
     */
    @Around("@annotation(org.springframework.kafka.annotation.KafkaListener)")
    public Object extractUserContext(ProceedingJoinPoint joinPoint) throws Throwable {
        ConsumerRecord<?, ?> record = findConsumerRecord(joinPoint.getArgs());

        if (record != null) {
            UserContext context = extractContextFromHeaders(record);
            if (context != null) {
                UserContextKafkaInterceptor.setUserContext(context);
                log.trace("Set user context from Kafka headers: user={}, tenant={}",
                    context.username(), context.primaryTenantId());
            }
        }

        try {
            return joinPoint.proceed();
        } finally {
            // Always clear context after listener execution
            UserContextKafkaInterceptor.clearUserContext();
        }
    }

    /**
     * Find ConsumerRecord in method arguments.
     */
    private ConsumerRecord<?, ?> findConsumerRecord(Object[] args) {
        if (args == null) {
            return null;
        }

        for (Object arg : args) {
            if (arg instanceof ConsumerRecord) {
                return (ConsumerRecord<?, ?>) arg;
            }
        }
        return null;
    }

    /**
     * Extract UserContext from Kafka message headers.
     */
    private UserContext extractContextFromHeaders(ConsumerRecord<?, ?> record) {
        String userId = getHeaderValue(record, UserContextKafkaInterceptor.HEADER_USER_ID);
        String username = getHeaderValue(record, UserContextKafkaInterceptor.HEADER_USERNAME);

        // If no user context headers, return null
        if (userId == null && username == null) {
            log.trace("No user context headers found in Kafka message: topic={}, partition={}, offset={}",
                record.topic(), record.partition(), record.offset());
            return null;
        }

        String tenantId = getHeaderValue(record, UserContextKafkaInterceptor.HEADER_TENANT_ID);
        String rolesHeader = getHeaderValue(record, UserContextKafkaInterceptor.HEADER_ROLES);
        String ipAddress = getHeaderValue(record, UserContextKafkaInterceptor.HEADER_IP_ADDRESS);

        // Parse roles
        Set<String> roles = new HashSet<>();
        if (rolesHeader != null && !rolesHeader.isBlank()) {
            roles.addAll(Arrays.asList(rolesHeader.split(",")));
        }

        // Parse tenant IDs (for now, just the single tenant)
        Set<String> tenantIds = new HashSet<>();
        if (tenantId != null && !tenantId.isBlank()) {
            tenantIds.add(tenantId);
        }

        // Parse user ID
        UUID parsedUserId = null;
        if (userId != null && !userId.isBlank() && !"anonymous".equals(userId)) {
            try {
                parsedUserId = UUID.fromString(userId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid user ID format in Kafka header: {}", userId);
            }
        }

        return UserContext.builder()
            .userId(parsedUserId)
            .username(username)
            .tenantIds(tenantIds)
            .roles(roles)
            .ipAddress(ipAddress)
            .build();
    }

    /**
     * Get header value as string from Kafka record.
     */
    private String getHeaderValue(ConsumerRecord<?, ?> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        if (header != null && header.value() != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return null;
    }
}
