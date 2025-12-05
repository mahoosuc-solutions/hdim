package com.healthdata.audit.service;

import com.healthdata.audit.entity.AuditEventEntity;
import com.healthdata.audit.mapper.AuditEventMapper;
import com.healthdata.audit.models.AuditEvent;
import com.healthdata.audit.models.AuditAction;
import com.healthdata.audit.models.AuditOutcome;
import com.healthdata.audit.repository.AuditEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * HIPAA-compliant audit service.
 *
 * Responsibilities:
 * - Store audit events
 * - Encrypt sensitive data
 * - Query audit logs
 * - Generate FHIR AuditEvent resources
 * - Enforce 7-year retention policy
 */
@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private static final int RETENTION_YEARS = 7; // HIPAA requirement

    private final ObjectMapper objectMapper;
    private final AuditEncryptionService encryptionService;
    private final AuditEventRepository repository;
    private final AuditEventMapper mapper;

    public AuditService(
            ObjectMapper objectMapper,
            AuditEncryptionService encryptionService,
            AuditEventRepository repository,
            AuditEventMapper mapper) {
        this.objectMapper = objectMapper;
        this.encryptionService = encryptionService;
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Log an audit event.
     * This is the primary method for recording HIPAA audit events.
     */
    @Transactional
    public void logAuditEvent(AuditEvent event) {
        try {
            // Encrypt sensitive fields if requested
            if (event.isEncrypted()) {
                encryptSensitiveData(event);
            }

            // Persist to database
            AuditEventEntity entity = mapper.toEntity(event);
            repository.save(entity);

            // Log to file as backup (meets basic HIPAA requirements)
            logger.info("AUDIT_EVENT: {}", objectMapper.writeValueAsString(event));

            // TODO: Optionally create FHIR AuditEvent resource
            // TODO: Optionally publish to audit event stream (Kafka)

        } catch (Exception e) {
            // CRITICAL: Audit logging failure should never break the application
            // but must be logged separately for compliance review
            logger.error("CRITICAL: Failed to log audit event. Event ID: {}, Error: {}",
                event.getId(), e.getMessage(), e);
        }
    }

    /**
     * Log a simple audit event with minimal details.
     */
    public void logEvent(String tenantId, String userId, AuditAction action,
                        String resourceType, String resourceId, AuditOutcome outcome) {
        AuditEvent event = AuditEvent.builder()
            .tenantId(tenantId)
            .userId(userId)
            .action(action)
            .resourceType(resourceType)
            .resourceId(resourceId)
            .outcome(outcome)
            .build();

        logAuditEvent(event);
    }

    /**
     * Log an access attempt (successful or failed).
     */
    public void logAccess(String tenantId, String userId, String resourceType,
                         String resourceId, boolean granted) {
        AuditAction action = granted ? AuditAction.ACCESS_GRANTED : AuditAction.ACCESS_DENIED;
        AuditOutcome outcome = granted ? AuditOutcome.SUCCESS : AuditOutcome.MINOR_FAILURE;

        logEvent(tenantId, userId, action, resourceType, resourceId, outcome);
    }

    /**
     * Log a login attempt.
     */
    public void logLogin(String username, String ipAddress, boolean successful, String errorMessage) {
        AuditEvent event = AuditEvent.builder()
            .username(username)
            .ipAddress(ipAddress)
            .action(successful ? AuditAction.LOGIN : AuditAction.LOGIN_FAILED)
            .outcome(successful ? AuditOutcome.SUCCESS : AuditOutcome.MINOR_FAILURE)
            .errorMessage(errorMessage)
            .build();

        logAuditEvent(event);
    }

    /**
     * Log emergency access (break-glass scenario).
     */
    public void logEmergencyAccess(String tenantId, String userId, String resourceType,
                                  String resourceId, String justification) {
        AuditEvent event = AuditEvent.builder()
            .tenantId(tenantId)
            .userId(userId)
            .action(AuditAction.EMERGENCY_ACCESS)
            .resourceType(resourceType)
            .resourceId(resourceId)
            .purposeOfUse("EMERGENCY")
            .outcome(AuditOutcome.SUCCESS)
            .errorMessage(justification) // Reuse this field for justification
            .build();

        logAuditEvent(event);
    }

    /**
     * Query audit events by user ID with default date range (last 30 days).
     *
     * @param userId the user ID to search for
     * @param pageable pagination parameters
     * @return page of audit events
     */
    @Transactional(readOnly = true)
    public Page<AuditEvent> findByUserId(String userId, Pageable pageable) {
        Instant to = Instant.now();
        Instant from = to.minus(30, ChronoUnit.DAYS);
        return findByUserId(userId, from, to, pageable);
    }

    /**
     * Query audit events by user ID with custom date range.
     *
     * @param userId the user ID to search for
     * @param from start of date range (inclusive)
     * @param to end of date range (inclusive)
     * @param pageable pagination parameters
     * @return page of audit events
     */
    @Transactional(readOnly = true)
    public Page<AuditEvent> findByUserId(String userId, Instant from, Instant to, Pageable pageable) {
        logger.debug("Querying audit events for user: {} from {} to {}", userId, from, to);
        Page<AuditEventEntity> entities = repository.findByUserIdAndTimestampBetween(userId, from, to, pageable);
        return entities.map(mapper::toModel);
    }

    /**
     * Query audit events by resource with default date range (last 30 days).
     *
     * @param resourceType the type of resource (e.g., "Patient", "Observation")
     * @param resourceId the resource ID
     * @param pageable pagination parameters
     * @return page of audit events
     */
    @Transactional(readOnly = true)
    public Page<AuditEvent> findByResource(String resourceType, String resourceId, Pageable pageable) {
        Instant to = Instant.now();
        Instant from = to.minus(30, ChronoUnit.DAYS);
        return findByResource(resourceType, resourceId, from, to, pageable);
    }

    /**
     * Query audit events by resource with custom date range.
     *
     * @param resourceType the type of resource (e.g., "Patient", "Observation")
     * @param resourceId the resource ID
     * @param from start of date range (inclusive)
     * @param to end of date range (inclusive)
     * @param pageable pagination parameters
     * @return page of audit events
     */
    @Transactional(readOnly = true)
    public Page<AuditEvent> findByResource(String resourceType, String resourceId, Instant from, Instant to, Pageable pageable) {
        logger.debug("Querying audit events for resource: {}/{} from {} to {}", resourceType, resourceId, from, to);
        Page<AuditEventEntity> entities = repository.findByResourceTypeAndResourceIdAndTimestampBetween(
                resourceType, resourceId, from, to, pageable);
        return entities.map(mapper::toModel);
    }

    /**
     * Query audit events by tenant with default date range (last 30 days).
     * Ensures multi-tenant isolation.
     *
     * @param tenantId the tenant ID
     * @param pageable pagination parameters
     * @return page of audit events
     */
    @Transactional(readOnly = true)
    public Page<AuditEvent> findByTenant(String tenantId, Pageable pageable) {
        Instant to = Instant.now();
        Instant from = to.minus(30, ChronoUnit.DAYS);
        return findByTenant(tenantId, from, to, pageable);
    }

    /**
     * Query audit events by tenant with custom date range.
     * Ensures multi-tenant isolation.
     *
     * @param tenantId the tenant ID
     * @param from start of date range (inclusive)
     * @param to end of date range (inclusive)
     * @param pageable pagination parameters
     * @return page of audit events
     */
    @Transactional(readOnly = true)
    public Page<AuditEvent> findByTenant(String tenantId, Instant from, Instant to, Pageable pageable) {
        logger.debug("Querying audit events for tenant: {} from {} to {}", tenantId, from, to);
        Page<AuditEventEntity> entities = repository.findByTenantIdAndTimestampBetween(tenantId, from, to, pageable);
        return entities.map(mapper::toModel);
    }

    /**
     * Purge audit events older than retention period (default 7 years for HIPAA).
     * Should be run as a scheduled job.
     *
     * @return number of records deleted
     */
    @Transactional
    public int purgeOldAuditEvents() {
        return purgeOldAuditEvents(RETENTION_YEARS);
    }

    /**
     * Purge audit events older than the specified retention period.
     * Allows custom retention periods for specific compliance requirements.
     *
     * @param retentionYears number of years to retain audit events
     * @return number of records deleted
     */
    @Transactional
    public int purgeOldAuditEvents(int retentionYears) {
        Instant cutoffDate = Instant.now().minus(retentionYears * 365L, ChronoUnit.DAYS);

        // First, count how many events will be deleted for logging
        long countToPurge = repository.countByTimestampBefore(cutoffDate);

        logger.info("Purging {} audit events older than {} (retention: {} years)",
                countToPurge, cutoffDate, retentionYears);

        // Perform the deletion
        int deletedCount = repository.deleteByTimestampBefore(cutoffDate);

        logger.info("Successfully purged {} audit events", deletedCount);
        return deletedCount;
    }

    /**
     * Encrypt sensitive data in the audit event.
     */
    private void encryptSensitiveData(AuditEvent event) {
        try {
            if (event.getRequestPayload() != null) {
                String encrypted = encryptionService.encrypt(event.getRequestPayload().toString());
                event.setRequestPayload(objectMapper.readTree("\"" + encrypted + "\""));
            }

            if (event.getResponsePayload() != null) {
                String encrypted = encryptionService.encrypt(event.getResponsePayload().toString());
                event.setResponsePayload(objectMapper.readTree("\"" + encrypted + "\""));
            }
        } catch (Exception e) {
            logger.error("Failed to encrypt audit event data", e);
        }
    }

    /**
     * Decrypt sensitive data from an audit event (for authorized review).
     */
    public void decryptSensitiveData(AuditEvent event) {
        try {
            if (event.isEncrypted() && event.getRequestPayload() != null) {
                String decrypted = encryptionService.decrypt(event.getRequestPayload().asText());
                event.setRequestPayload(objectMapper.readTree(decrypted));
            }

            if (event.isEncrypted() && event.getResponsePayload() != null) {
                String decrypted = encryptionService.decrypt(event.getResponsePayload().asText());
                event.setResponsePayload(objectMapper.readTree(decrypted));
            }
        } catch (Exception e) {
            logger.error("Failed to decrypt audit event data", e);
        }
    }
}
